package website.video.downloader

import javafx.embed.swing.*
import javafx.event.*
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.*
import javafx.scene.image.Image
import org.zeroturnaround.exec.*
import org.zeroturnaround.exec.stream.*
import tornadofx.*
import website.video.downloader.BuildConfig.*
import website.video.downloader.Styles.Companion.glyphLabel
import java.awt.*
import java.io.*
import java.net.*
import java.net.http.*
import java.net.http.HttpResponse.*
import java.util.*
import javax.imageio.*
import kotlin.math.*

fun execYoutubeDl(vararg args: String, progress: (String) -> Unit) = ProcessExecutor()
    .command(youtubeDl + args)
    .redirectOutput(object : LogOutputStream() {
        override fun processLine(line: String) {
            progress(line)
        }
    })
    .execute()!!

fun openOutDirInFiles() = Runtime.getRuntime().exec("$fileManager ${appConfig.downloadDir}")!!

fun imageToBase64(image: Image) = ByteArrayOutputStream().use {
    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", it)
    Base64.getEncoder().encodeToString(it.toByteArray())
}!!

fun EventTarget.button(tooltip: String, image: String, op: Button.() -> Unit = {}) {
    button {
        tooltip { text = tooltip }
        isFocusTraversable = false
        graphic = ImageView(image)
        op(this)
    }
}

fun EventTarget.glyph(value: String, op: Label.() -> Unit = {}) {
    label(value) {
        addClass(glyphLabel)
        op(this)
    }
}

fun String.isYoutubeUrl() = startsWith("https://www.youtube.com/watch") || startsWith("https://youtu.be/")

fun hasUpdates() = run {

    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://video-downloader.website/latest"))
        .build()

    val published = runCatching {
        HttpClient.newHttpClient()
            .send(request, BodyHandlers.ofString())
            .body()
            .toDouble()
    }
        .getOrDefault(0.0)

    published > VERSION.toDouble()
}

val totalTime
    get() = jobs
        .filter { it.duration != null }
        .map { it.duration!!.split(":".toRegex(), 3).map(String::toInt) }
        .map { (h, m, s) -> h * 3600 + m * 60 + s }
        .fold(0) { acc, value -> acc + value }
        .let {
            val x = it % 3600
            val h = floor(it / 3600.0).toInt().toString().padStart(2, '0')
            val m = floor(x / 60.0).toInt().toString().padStart(2, '0')
            val s = (x % 60).toString().padStart(2, '0')

            "$h:$m:$s"
        }

const val APP_NAME = "video-downloader"

val desktop = Desktop.getDesktop()!!
val LINE_SEPARATOR = System.lineSeparator()!!
val USER_HOME = System.getProperty("user.home")!!
val IS_WINDOWS = System.getProperty("os.name").contains("Windows")

val configDir = if (IS_WINDOWS)
    File("$USER_HOME/AppData/Local")
else
    File("$USER_HOME/.config")

private val fileManager = if (IS_WINDOWS)
    "explorer"
else
    "nemo" // todo add support for other managers

private val appRoot = File(Application::class.java.protectionDomain.codeSource.location.toURI()).parentFile.parent
private val youtubeDlLinux = "$appRoot/runtime/bin/youtube-dl"

private val youtubeDl = if (IS_WINDOWS)
    listOf("${youtubeDlLinux}.exe")
else {
    val youtube = if (File(youtubeDlLinux).exists())
        youtubeDlLinux
    else
        "setup/youtube-dl"

    listOf("python3", youtube)
}
