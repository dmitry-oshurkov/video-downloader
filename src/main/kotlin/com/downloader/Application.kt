package com.downloader

import com.downloader.view.*
import com.sun.net.httpserver.*
import javafx.stage.*
import tornadofx.*
import java.io.*
import java.net.*

class Application : App(Main::class, Styles::class) {

    private val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 9533), 0)

    override fun start(stage: Stage) {

        with(stage) {
            maxWidth = 1280.0
            maxHeight = 853.0
            minWidth = 590.0
            minHeight = 160.0

            isMaximized = Prefs.isMaximized
            x = Prefs.x
            y = Prefs.y
            width = Prefs.width
            height = Prefs.height

            setOnCloseRequest {
                Prefs.isMaximized = isMaximized
                Prefs.x = x
                Prefs.y = y
                Prefs.width = width
                Prefs.height = height
                Prefs.save()
            }
        }

        startRest()

        super.start(stage)
    }

    private fun startRest() = with(httpServer) {
        createContext("/api/queue") { http ->

            if (http.requestMethod == "POST") {
                val url = http.requestBody.readAllBytes().decodeToString()
                runLater { placeToQueue(url) }
            }
            http.sendResponseHeaders(200, 0)
            PrintWriter(http.responseBody).use { }
        }
        start()
    }

    override fun stop() {
        httpServer.stop(0)
        super.stop()
    }
}
