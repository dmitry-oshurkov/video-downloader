package website.video.downloader

import com.typesafe.config.*
import io.github.config4k.*
import java.io.*

data class AppConfig(
    val locale: String,
    val downloadDir: String,
    val urlListenerPort: Int
)

fun loadConfig() = configFile
    .takeIf { it.exists() }
    ?.let { ConfigFactory.parseFile(it) }
    ?.extract<AppConfig>()

fun writeConfig(appConfig: AppConfig) {

    val cfg = mapOf(
        "# ru-RU, en_US locales is supported" to "",
        "locale" to appConfig.locale,
        "# downloaded videos local storage" to "",
        "downloadDir" to appConfig.downloadDir,
        "# port for listen url from browser extension" to "",
        "urlListenerPort" to appConfig.urlListenerPort
    )
        .renderConfig()

    configFile.parentFile.mkdirs()
    configFile.writeText(cfg)
}

private fun Map<String, Any>.renderConfig() =
    map { (key, config) ->
        if (key.startsWith("#") || key.startsWith("//"))
            key
        else
            config.toConfig(key).root().render(renderOptions)
    }
        .joinToString(LINE_SEPARATOR)
        .replace("\n", LINE_SEPARATOR)

private val renderOptions = ConfigRenderOptions.concise().setJson(false).setFormatted(true).setComments(true)

private val configFile = File("$USER_HOME/.config/$APP_NAME/application.conf")
