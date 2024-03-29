package website.video.downloader

import javafx.embed.swing.*
import javafx.event.*
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import tornadofx.*
import website.video.downloader.Styles.Companion.glyphLabel
import java.awt.*
import java.io.*
import java.time.*
import java.time.temporal.ChronoUnit.*
import java.util.*
import javax.imageio.*
import kotlin.concurrent.*
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

fun openOutDirInFiles() = Runtime.getRuntime().exec(arrayOf(fileManager, appConfig.downloadDir))!!

fun imageToBase64(image: Image?) =
    if (image != null)
        ByteArrayOutputStream().use {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", it)
            Base64.getEncoder().encodeToString(it.toByteArray())
        }
    else
        null

fun EventTarget.btn(op: Button.() -> Unit = {}) {
    button {
        isFocusTraversable = false
        op(this)
    }
}

fun EventTarget.btn(tooltip: String, image: String, op: Button.() -> Unit = {}) {
    btn {
        tooltip { text = tooltip }
        graphic = imageview(image)
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
fun String.isPHUrl() = startsWith("https://rt.pornhub.com/view_video")
fun String.isDzenUrl() = startsWith("https://dzen.ru/video/watch")

val totalTime
    get() = jobs
        .filter { it.duration != null }
        .map { it.duration!!.padEnd(3, ':').padEnd(4, '0').padEnd(5, '0').padEnd(6, ':').padEnd(7, '0').padEnd(8, '0').split(":").map(String::toInt) }
        .map { (h, m, s) -> h * 3600 + m * 60 + s }
        .fold(0) { acc, value -> acc + value }
        .let {
            val x = it % 3600
            val h = floor(it / 3600.0).toInt().toString().padStart(2, '0')
            val m = floor(x / 60.0).toInt().toString().padStart(2, '0')
            val s = (x % 60).toString().padStart(2, '0')

            "$h:$m:$s"
        }


fun runPlayingTimer() {

    if (playingStarted == null) {
        playingStarted = LocalTime.now()

        fixedRateTimer(daemon = true, startAt = Date(), period = 1.seconds.inWholeMilliseconds) {
            val played = LocalTime.now().minusNanos(playingStarted!!.toNanoOfDay()).truncatedTo(SECONDS)

            if (played.toSecondOfDay() > 0)
                runLater { playingTime.value = "$played" }
        }
    }
}

var playingTime = "00:00:00".toProperty()

private var playingStarted: LocalTime? = null


const val APP_NAME = "video-get"

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
