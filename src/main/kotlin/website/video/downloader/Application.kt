package website.video.downloader

import javafx.stage.*
import tornadofx.*
import website.video.downloader.view.*
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
