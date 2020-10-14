package com.downloader

import com.downloader.Styles.Companion.glyphLabel
import javafx.embed.swing.*
import javafx.event.*
import javafx.scene.control.*
import javafx.scene.image.*
import org.zeroturnaround.exec.*
import org.zeroturnaround.exec.stream.*
import tornadofx.*
import java.io.*
import java.util.*
import javax.imageio.*

fun execYoutubeDl(vararg args: String, progress: (String) -> Unit) = ProcessExecutor()
    .command(listOf("python3", "/usr/share/video-downloader/bin/youtube-dl", *args)) // todo detect youtube-dl path
    .redirectOutput(object : LogOutputStream() {
        override fun processLine(line: String) {
            progress(line)
        }
    })
    .execute()!!

fun openOutDirInFiles() = Runtime.getRuntime().exec("nemo ${appConfig.downloadDir}")!!

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
