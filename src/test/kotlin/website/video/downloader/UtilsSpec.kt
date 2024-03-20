package website.video.downloader

import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import io.mockk.*
import tornadofx.*

class UtilsSpec : StringSpec({

    appConfig = AppConfig(locale = "libero", downloadDir = "potenti", urlListenerPort = 1983)

    "totalTime should return a sum of video times" {

        mockkStatic(::main)
        every { appConfig.downloadDir } returns ""

        mockkStatic(::loadJobs)
        every { jobs } returns listOf(
            Job(false, "", "", "", duration = "00:23:24"),
            Job(false, "", "", "", duration = "00:21:49"),
            Job(false, "", "", "", duration = "00:20:42"),
            Job(false, "", "", "", duration = "00:14:19")
        ).asObservable()

        totalTime shouldBe "01:20:14"
    }
})
