package website.video.downloader

import javafx.scene.control.Alert.AlertType.*
import javafx.scene.control.ButtonType.*
import javafx.stage.*
import kotlinx.coroutines.*
import tornadofx.*
import tornadofx.FX.Companion.messages
import website.video.downloader.view.*
import java.net.*
import java.util.*

class Application : App(Main::class, Styles::class) {

    init {
        FX.locale = Locale(appConfig.locale)
    }

    override fun start(stage: Stage) {

        with(stage) {
            maxWidth = 1280.0
            maxHeight = 853.0
            minWidth = 590.0
            minHeight = 160.0

            isMaximized = Prefs.isMaximized
            width = Prefs.width
            height = Prefs.height
            if (!Prefs.x.isNaN())
                x = Prefs.x
            if (!Prefs.y.isNaN())
                y = Prefs.y

            setOnShown {
                if (hasUpdates())
                    alert(INFORMATION, messages["dialog.update.header"], null, YES, NO, owner = this, title = messages["dialog.update.title"]) {
                        if (it == YES)
                            runAsync { desktop.browse(URI("https://video-downloader.website/#download")) }
                    }
            }

            setOnCloseRequest {
                Prefs.isMaximized = isMaximized
                Prefs.x = x
                Prefs.y = y
                Prefs.width = width
                Prefs.height = height
                Prefs.save()
            }
        }

        loadJobs()
        runJobMonitor()
        startRest()

        super.start(stage)
    }

    override fun stop() {
        stopRest()
        super.stop()
    }
}
