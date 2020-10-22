package website.video.downloader

import com.sun.net.httpserver.*
import tornadofx.*
import java.io.*
import java.net.*

fun startRest() = with(httpServer) {
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

fun stopRest() {
    httpServer.stop(0)
}


private val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", appConfig.urlListenerPort), 0)
