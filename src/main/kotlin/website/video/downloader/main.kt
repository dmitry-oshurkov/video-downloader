package website.video.downloader

import tornadofx.*
import java.io.*
import java.nio.file.*
import java.util.*

fun main() {

    appConfig = loadConfig() ?: run {

        val downloadDir = if (IS_WINDOWS)
            File("$USER_HOME/Downloads/.$APP_NAME").apply {
                mkdirs()
                Files.setAttribute(toPath(), "dos:hidden", true)
            }
        else
            File("$configDir/user-dirs.dirs").readText()
                .let { """XDG_DOWNLOAD_DIR="([^"]+)"""".toRegex().find(it)?.groupValues?.last()?.replace("\$HOME", USER_HOME) }
                .let { File("$it/.$APP_NAME") }

        writeConfig(AppConfig(
            locale = Locale.getDefault().toLanguageTag(),
            downloadDir = downloadDir.absolutePath,
            urlListenerPort = 9533
        ))

        loadConfig()!!
    }

    launch<Application>()
}

lateinit var appConfig: AppConfig
