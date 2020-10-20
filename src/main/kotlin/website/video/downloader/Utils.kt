package website.video.downloader

import javafx.embed.swing.*
import javafx.event.*
import javafx.scene.control.*
import javafx.scene.image.*
import org.zeroturnaround.exec.*
import org.zeroturnaround.exec.stream.*
import tornadofx.*
import website.video.downloader.Styles.Companion.glyphLabel
import java.io.*
import java.util.*
import javax.imageio.*

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


const val APP_NAME = "video-downloader"

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

private val youtubeDl = if (IS_WINDOWS)
    listOf("$appRoot/bin/youtube-dl.exe")
else {
    val youtube = if (File("$appRoot/bin/youtube-dl").exists())
        "$appRoot/bin/youtube-dl"
    else
        "setup/youtube-dl"

    listOf("python3", youtube)
}
