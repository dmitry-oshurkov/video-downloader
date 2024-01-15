@file:Suppress("RegExpRedundantEscape")

package website.video.downloader

import javafx.embed.swing.*
import javafx.scene.control.ProgressIndicator.*
import javafx.scene.image.*
import kotlinx.coroutines.*
import org.apache.commons.io.file.*
import org.apache.commons.io.filefilter.DirectoryFileFilter.*
import org.zeroturnaround.exec.*
import org.zeroturnaround.exec.listener.*
import org.zeroturnaround.exec.stream.*
import tornadofx.*
import tornadofx.FX.Companion.messages
import tornadofx.FX.Companion.primaryStage
import website.video.downloader.DownloadState.*
import java.io.*
import java.net.*
import java.nio.file.*
import java.time.*
import java.time.format.*
import java.util.*
import javax.imageio.*
import kotlin.io.path.*
import kotlin.text.Regex.Companion.escape
import kotlin.time.Duration.Companion.seconds

fun placeToQueue(url: String?) = url
    ?.takeIf { it !in jobs.map { job -> job.url } }
    ?.takeIf { it.isYoutubeUrl() || it.isPHUrl() }
    ?.let {
        jobs += Job(remote = false, url = it, title = it)
        saveJobs()
    }

fun loadJobs() {

    jobsFile.parentFile.mkdirs()

    if (jobsFile.exists())
        jobs += jobsFile.readText()
            .parseJson<List<Job>>()
            .onEach {
                if (it.state == IN_PROGRESS)
                    it.state = NEW
            }
}

@OptIn(DelicateCoroutinesApi::class)
fun runJobMonitor() = GlobalScope.launch {

    while (isActive) {
        jobs.firstOrNull { !it.remote && it.state == NEW }?.runDownload()
        jobs.firstOrNull { it.remote && it.state == NEW }?.runRemoteDownload()
        delay(1.seconds)
    }
}


@OptIn(DelicateCoroutinesApi::class)
fun runRemoteJobMonitor() = GlobalScope.launch {

    val channels = File("/mnt/skyserver-public/services/video-collector/channels")

    while (isActive) {

        val downloaded = try {
            PathUtils
                .newDirectoryStream(Path(channels.absolutePath), INSTANCE)
                .map { PathUtils.newDirectoryStream(Path(it.absolutePathString()), INSTANCE).toList() }
                .filter { it.isNotEmpty() }
                .flatten()
                .map { it.toString() }
        } catch (e: FileSystemException) {
            emptyList()
        }

        val alreadyExists = jobs.mapNotNull { it.remoteDir }
        val ready = downloaded.filter { it !in alreadyExists }
        runLater { readyCount.value = ready.size }

        ready.forEach {

            val metadata = File(it, "metadata")

            if (metadata.exists()) {

                val thumbnailJpg = File(it, "thumbnail.jpg")
                val thumbnailWebp = File(it, "thumbnail.webp")
                val lines = metadata.readLines()

                val thumbnailBytes = when {
                    thumbnailJpg.exists() -> thumbnailJpg.readBytes()
                    thumbnailWebp.exists() -> thumbnailWebp.readBytes()
                    else -> null
                }

                if (thumbnailBytes != null) {

                    val thumbnailB64 = Base64.getEncoder().encodeToString(thumbnailBytes)
                    val duration = lines[3].toLong()

                    val job = Job(
                        remote = true,
                        remoteDir = it,
                        url = "https://youtu.be/${lines[0]}?t=${duration - 15}",
                        title = lines[1],
                        uploader = lines[2],
                        duration = LocalTime.ofSecondOfDay(duration).format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        file = File(it, File(lines[5]).name).absolutePath,
                        format = "★",
                        fps = 30,
                        thumbnail = thumbnailB64,
                    )

                    runLater { jobs += job }
                    runLater { readyCount.value -= 1 }
                }
            }
        }

        saveJobs()
        delay(10.seconds)
    }
}


fun Job.runRemoteDownload() = run {

    runLater {
        stateProperty().set(IN_PROGRESS)
        progressProperty().set(INDETERMINATE_PROGRESS)
    }

    val file1 = File(file)
    val copied = file1.copyTo(File(appConfig.downloadDir, file1.name), overwrite = true)

    file = copied.absolutePath

    File(remoteDir).deleteRecursively()

    runLater {
        fileSizeProperty().set(copied.length())
        stateProperty().set(COMPLETED)
        saveJobs()
    }
}


fun Job.runDownload() = run {

    runLater { stateProperty().set(IN_PROGRESS) }
    execYoutubeDl("--dump-json", url) {

        checkError(it)

        val videoInfo = runCatching { it.parseJson<YoutubeVideo>() }
            .onFailure { e -> error("Download", e.stackTraceToString(), owner = primaryStage) }
            .getOrNull()

        if (videoInfo != null) {

            val thumbnailImage = runCatching { SwingFXUtils.toFXImage(ImageIO.read(URL(videoInfo.thumbnail)), null) }
                .getOrElse {
                    videoInfo.thumbnails
                        ?.filter { t -> t.url?.endsWith("/default.jpg") == true }
                        ?.map { t -> SwingFXUtils.toFXImage(ImageIO.read(URL(t.url)), null) }
                        ?.singleOrNull()
                }

            val thumbnail = imageToBase64(thumbnailImage)
            setInfo(videoInfo, thumbnail, thumbnailImage)

            val height = if (Prefs.maxQuality)
                "2160"
            else
                "1080"

            execYoutubeDl("--no-warnings", "-f", "bestvideo[height<=$height]+bestaudio/best[height<=$height]", "-o", outFile, url) { s ->

                checkError(s)

                val groups = downloadProgress.find(s)?.groups
                val pathname = downloaded.find(s)?.groupValues?.last() ?: alreadyDownloaded.find(s)?.groupValues?.last()

                if (groups != null)
                    setProgress(groups)

                if (pathname != null)
                    runLater { fileProperty().set(pathname) }
            }

            if (file != null)
                setCompletedAndSave(videoInfo, File(file!!))
        }
    }
}

fun Job.delete() = run {

    deleted = true
    stop()

    jobs.remove(this)
    saveJobs()
}

fun Job.stop() {

    cancelDownload()

    if (file != null)
        File(file!!).delete()

    val regex = "${escape(title.replace("?", ""))}.*\\.part".toRegex()

    Files.find(Path.of(appConfig.downloadDir), 1, { path, _ -> path.toFile().name.matches(regex) })
        .forEach { it.toFile().delete() }
}


private fun Job.execYoutubeDl(vararg args: String, process: (String) -> Unit) {

    if (!deleted) {
        ProcessExecutor()
            .command(youtubeDl + "--ffmpeg-location" + ffmpeg + "--user-agent" + "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0" + args)
            .destroyOnExit()
            .addDestroyer(object : ProcessDestroyer {
                override fun add(process: Process): Boolean {
                    cancelDownload = { runCatching { process.destroy() } }
                    return true
                }

                override fun remove(process: Process) = true
                override fun size() = 1
            })
            .redirectOutput(object : LogOutputStream() {
                override fun processLine(line: String) {
                    println(line)
                    process(line)
                }
            })
            .execute()
    }
}

private fun Job.checkError(s: String) = runLater {
    if (s.startsWith("ERROR")) {
        needReload.set(true)
        titleProperty().set(s)
    } else
        needReload.set(false)
}

private fun Job.setInfo(videoInfo: YoutubeVideo, thumbnail: String?, thumbnailImage: Image?) = runLater {
    titleProperty().set(videoInfo.title)
    uploaderProperty().set(videoInfo.uploader)
    thumbnailProperty().set(thumbnail)
    thumbnailImageProperty().set(thumbnailImage)
    durationProperty().set(LocalTime.ofSecondOfDay(videoInfo.duration.toLong()).format(DateTimeFormatter.ofPattern("HH:mm:ss")))

    saveJobs()
}

private fun Job.setProgress(groups: MatchGroupCollection) = runLater {
    progressProperty().set(groups[1]?.value?.toDouble()?.div(100))
    fileSizeTextInProgressProperty().set("${groups[2]?.value} ${convertUnits(groups[3]?.value)}")
    speedProperty().set("${groups[4]?.value} ${convertUnits(groups[5]?.value)}".padStart(12))
    etaProperty().set(groups[6]?.value)
}

private fun Job.setCompletedAndSave(videoInfo: YoutubeVideo, file: File) = runLater {
    fileSizeProperty().set(file.length())
    stateProperty().set(COMPLETED)

    val format = videoInfo.formats?.single { it.format_id == videoInfo.format_id?.split("+")?.first() }
    formatProperty().set(format?.format_note)
    fpsProperty().set(format?.fps)

    saveJobs()
}

private fun saveJobs() = jobsFile.writeText(jobs.toJson())

private fun convertUnits(value: String?) = when (value) {
    "GiB" -> messages["jobs.units.GiB"]
    "MiB" -> messages["jobs.units.MiB"]
    "KiB" -> messages["jobs.units.KiB"]
    "GiB/s" -> messages["jobs.units.GiB/s"]
    "MiB/s" -> messages["jobs.units.MiB/s"]
    "KiB/s" -> messages["jobs.units.KiB/s"]
    else -> ""
}

/**
 * [jobs file](file:///home/dmitry/.local/share/video-get/jobs.json)
 */
val jobs = mutableListOf<Job>().asObservable()

var readyCount = (-1).toProperty()

private val downloadProgress = """\[download\]\s+(.*)%\s+of[\s~]+([\d.]*)(GiB|MiB|KiB).+at\s+([\d.]*)(GiB\/s|MiB\/s|KiB\/s).+ETA\s+([\d:]*)""".toRegex()
private val downloaded = """Merging formats into "([\s\S]*?)"""".toRegex()
private val alreadyDownloaded = """\[download\]\s+(.*)\s+has""".toRegex()

private val jobsFile = if (IS_WINDOWS)
    File("$configDir/$APP_NAME/jobs.json")
else
    File("$USER_HOME/.local/share/$APP_NAME/jobs.json")

private val outFile = File(appConfig.downloadDir, "%(title)s.%(ext)s").absolutePath

private val appRoot = File(Application::class.java.protectionDomain.codeSource.location.toURI()).parentFile.parent


private val ytDlpLinux = "$appRoot/runtime/bin/yt-dlp_linux"
private val ffmpegLinux = "$appRoot/runtime/bin/ffmpeg"

private val youtubeDl = if (IS_WINDOWS)
    listOf("$appRoot/runtime/bin/youtube-dl.exe")
else {
    if (File(ytDlpLinux).exists())
        listOf(ytDlpLinux)
    else
        listOf("setup/yt-dlp_linux")
}

private val ffmpeg = if (IS_WINDOWS)
    listOf("$appRoot/runtime/bin/youtube-dl.exe")
else {
    if (File(ffmpegLinux).exists())
        listOf(ffmpegLinux)
    else
        listOf("setup/ffmpeg")
}
