package website.video.downloader

import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import io.mockk.*
import tornadofx.*

class UtilsSpec : StringSpec({

    "totalTime should return a sum of video times" {

        mockkStatic("website.video.downloader.MainKt")
        every { appConfig.downloadDir } returns ""

        mockkStatic("website.video.downloader.JobsKt")
        every { jobs } returns listOf(
            Job("", "", "00:23:24"),
            Job("", "", "00:21:49"),
            Job("", "", "00:20:42"),
            Job("", "", "00:14:19")
        ).asObservable()

        totalTime shouldBe "01:20:14"
    }
})
