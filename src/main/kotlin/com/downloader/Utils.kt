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
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission.*
import java.util.*
import javax.imageio.*

fun execYoutubeDl(vararg args: String, progress: (String) -> Unit) = ProcessExecutor()
    .command(listOf("youtube-dl", *args))
    .redirectOutput(object : LogOutputStream() {
        override fun processLine(line: String) {
            progress(line)
        }
    })
    .execute()!!

fun openOutDirInFiles() = Runtime.getRuntime().exec("nemo $outDir")!!

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


private infix fun String.copyResourceTo(path: Path) {

    Application::class.java.getResourceAsStream(this)
        .use { Files.copy(it, path) }

    Files.setPosixFilePermissions(path, HashSet(listOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)))
}


const val APP_NAME = "video-downloader"

private val userHome = System.getProperty("user.home")

val localShare = "$userHome/.local/share"
val outDir = File("$userHome/Загрузки/.$APP_NAME")

private val configDir = File("$userHome/.config/$APP_NAME")
private val appLink = File("$localShare/applications/$APP_NAME.desktop")
