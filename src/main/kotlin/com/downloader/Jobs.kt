@file:Suppress("RegExpRedundantEscape")

package com.downloader

import com.downloader.DownloadState.*
import javafx.embed.swing.*
import javafx.scene.image.*
import kotlinx.coroutines.*
import tornadofx.*
import java.io.*
import java.net.*
import java.nio.file.*
import java.text.*
import java.time.*
import javax.imageio.*

fun placeToQueue(url: String?) = url
    ?.takeIf { it !in jobs.map { job -> job.url } }
    ?.takeIf { it.isYoutubeUrl() }
    ?.let {
        jobs += DownloadJob(url = it, title = it)
        saveJobs()
    }

fun DownloadJob.delete() = run {

    if (file != null)
        File(file).delete()

    Files.find(outDir.toPath(), 1, { path, _ -> path.toFile().name.matches("$title.*\\.part".toRegex()) })
        .forEach { path -> path.toFile().delete() }

    jobs.remove(this)
    saveJobs()
}

fun loadJobs() {

    if (jobsFile.exists())
        jobs += jobsFile.readText()
            .parseJson<List<DownloadJob>>()
            .onEach {
                if (it.state == IN_PROGRESS)
                    it.state = NEW
            }
}

fun runJobMonitor() = GlobalScope.launch {

    while (isActive) {
        jobs.firstOrNull { it.state == NEW }?.runDownload()
        delay(300)
    }
}


private fun DownloadJob.runDownload() = run {

    runLater { stateProperty().set(IN_PROGRESS) }
    execYoutubeDl("--dump-json", url) {

        if (it.startsWith("ERROR"))
            runLater { titleProperty().set(it) }

        val videoInfo = it.parseJson<YoutubeVideo>()

        val thumbnailImage = SwingFXUtils.toFXImage(ImageIO.read(URL(videoInfo.thumbnail)), null)
        val thumbnail = imageToBase64(thumbnailImage)

        setInfo(videoInfo, thumbnail, thumbnailImage)

        execYoutubeDl("--no-warnings", "-f", "bestvideo[height<=1080]+bestaudio/best[height<=1080]", "-o", outFile, url) { s ->

            if (s.startsWith("ERROR"))
                runLater { titleProperty().set(s) }

            val groups = downloadProgress.find(s)?.groups
            val pathname = downloaded.find(s)?.groupValues?.last() ?: alreadyDownloaded.find(s)?.groupValues?.last()

            if (groups != null)
                setProgress(groups)

            if (pathname != null)
                fileProperty().set(pathname)
        }

        setCompletedAndSave(videoInfo, File(file))
    }
}

private fun DownloadJob.setInfo(videoInfo: YoutubeVideo, thumbnail: String, thumbnailImage: Image) = runLater {
    titleProperty().set(videoInfo.title)
    thumbnailProperty().set(thumbnail)
    thumbnailImageProperty().set(thumbnailImage)
    durationProperty().set(LocalTime.ofSecondOfDay(videoInfo.duration.toLong()).toString())

    saveJobs()
}

private fun DownloadJob.setProgress(groups: MatchGroupCollection) = runLater {
    progressProperty().set(groups[1]?.value?.toDouble()?.div(100))
    fileSizeProperty().set("${groups[2]?.value} ${convertUnits(groups[3]?.value)}")
    speedProperty().set("${groups[4]?.value} ${convertUnits(groups[5]?.value)}".padStart(12))
    etaProperty().set(groups[6]?.value)
}

private fun DownloadJob.setCompletedAndSave(videoInfo: YoutubeVideo, file: File) = runLater {
    fileSizeProperty().set(humanReadableByteCountSI(file.length()))
    stateProperty().set(COMPLETED)

    val format = videoInfo.formats?.single { it.format_id == videoInfo.format_id?.split("+")?.first() }
    videoFormatProperty().set("${file.extension.toUpperCase()} · ${format?.format_note} · ${format?.fps} к/с")

    saveJobs()
}

private fun saveJobs() = jobsFile.writeText(jobs.toJson())

private fun humanReadableByteCountSI(bytesValue: Long) = run {

    var bytes = bytesValue

    when {

        -1000 < bytes && bytes < 1000 -> "$bytes Б"

        else -> {
            val ci = StringCharacterIterator("кМГТПЭ")

            while (bytes <= -999950 || bytes >= 999950) {
                bytes /= 1000
                ci.next()
            }

            String.format("%.1f %cБ", bytes / 1000.0, ci.current())
        }
    }
}

private fun convertUnits(value: String?) = when (value) {
    "GiB" -> "ГБ"
    "MiB" -> "МБ"
    "KiB" -> "КБ"
    "GiB/s" -> "ГБ/с"
    "MiB/s" -> "МБ/с"
    "KiB/s" -> "КБ/с"
    else -> ""
}


val jobs = mutableListOf<DownloadJob>().asObservable()

private val downloadProgress = """\[download\]\s+(.*)%\s+of\s+([\d.]*)(GiB|MiB|KiB).+at\s+([\d.]*)(GiB\/s|MiB\/s|KiB\/s).+ETA\s+([\d:]*)""".toRegex()
private val downloaded = """Merging formats into "([\s\S]*?)"""".toRegex()
private val alreadyDownloaded = """\[download\]\s+(.*)\s+has""".toRegex()

private val jobsFile = File("$localShare/$APP_NAME/jobs.json")
private val outFile = File(outDir, "%(title)s.%(ext)s").absolutePath
