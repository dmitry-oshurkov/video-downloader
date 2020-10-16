@file:Suppress("unused", "PropertyName")

package website.video.downloader

import javafx.beans.property.*
import javafx.embed.swing.*
import javafx.scene.image.*
import tornadofx.*
import website.video.downloader.DownloadState.*
import java.io.*
import java.util.*
import javax.imageio.*

class DownloadJob(
    val url: String,
    title: String? = null,
    duration: String? = null,
    file: String? = null,
    fileSize: String? = null,
    videoFormat: String? = null,
    state: DownloadState = NEW,
    thumbnail: String? = null,
    progress: Number? = null,
    speed: String? = null,
    eta: String = "__:__",
) {
    var title: String by property(title)
    fun titleProperty() = getProperty(DownloadJob::title)

    var duration: String by property(duration)
    fun durationProperty() = getProperty(DownloadJob::duration)

    var file: String? by property(file)
    fun fileProperty() = getProperty(DownloadJob::file)

    var fileSize: String by property(fileSize)
    fun fileSizeProperty() = getProperty(DownloadJob::fileSize)

    var videoFormat: String by property(videoFormat)
    fun videoFormatProperty() = getProperty(DownloadJob::videoFormat)

    var state: DownloadState by property(state)
    fun stateProperty() = getProperty(DownloadJob::state)

    var thumbnail: String by property(thumbnail)
    fun thumbnailProperty() = getProperty(DownloadJob::thumbnail)


    private var thumbnailImage by property {
        val image = if (thumbnail != null) ByteArrayInputStream(Base64.getDecoder().decode(thumbnail)).use {
            SwingFXUtils.toFXImage(ImageIO.read(it), null)
        } else
            null
        SimpleObjectProperty(image as? Image)
    }

    fun thumbnailImageProperty() = getProperty(DownloadJob::thumbnailImage)

    private var progress by property(progress)
    fun progressProperty() = getProperty(DownloadJob::progress)

    private var speed by property(speed)
    fun speedProperty() = getProperty(DownloadJob::speed)

    private var eta by property(eta)
    fun etaProperty() = getProperty(DownloadJob::eta)

    fun tooltipProperty() = stringBinding(titleProperty(), url.toProperty()) { "$title\n\n$url" }

    override fun toString() = title
}

enum class DownloadState { NEW, IN_PROGRESS, COMPLETED, ERROR }

class YoutubeVideo {
    var upload_date: String? = null
    var extractor: String? = null
    var series: Any? = null
    var format: String? = null
    var vbr: Any? = null
    var chapters: Any? = null
    var height = 0
    var like_count = 0
    var duration = 0
    var fulltitle: String? = null
    var playlist_index: Any? = null
    var album: Any? = null
    var view_count = 0
    var playlist: Any? = null
    var title: String? = null
    var _filename: String? = null
    var creator: Any? = null
    var ext: String? = null
    var id: String? = null
    var dislike_count = 0
    var average_rating = 0.0
    var abr = 0
    var uploader_url: String? = null
    var categories: Any? = null
    var fps = 0
    var stretched_ratio: Any? = null
    var season_number: Any? = null
    var annotations: Any? = null
    var webpage_url_basename: String? = null
    var acodec: String? = null
    var display_id: String? = null
    var requested_formats: List<RequestedFormat>? = null
    var automatic_captions: Any? = null
    var description: String? = null
    var tags: List<Any>? = null
    var track: Any? = null
    var requested_subtitles: Any? = null
    var start_time: Any? = null
    var uploader: String? = null
    var format_id: String? = null
    var episode_number: Any? = null
    var uploader_id: String? = null
    var subtitles: Any? = null
    var release_year: Any? = null
    var thumbnails: List<Thumbnail>? = null
    var license: Any? = null
    var artist: Any? = null
    var extractor_key: String? = null
    var release_date: Any? = null
    var alt_title: Any? = null
    var thumbnail: String? = null
    var channel_id: String? = null
    var is_live: Any? = null
    var width = 0
    var end_time: Any? = null
    var webpage_url: String? = null
    var formats: List<Format>? = null
    var channel_url: String? = null
    var resolution: Any? = null
    var vcodec: String? = null
    var age_limit = 0
}

class DownloaderOptions {
    var http_chunk_size = 0
}

class Format {
    var asr = 0
    var tbr = 0.0
    var protocol: String? = null
    var format: String? = null
    var url: String? = null
    var vcodec: String? = null
    var format_note: String? = null
    var abr = 0
    var player_url: Any? = null
    var downloader_options: DownloaderOptions? = null
    var width = 0
    var ext: String? = null
    var filesize: Any? = null
    var fps = 0
    var format_id: String? = null
    var height = 0
    var http_headers: HttpHeaders? = null
    var acodec: String? = null
    var container: String? = null
}

class HttpHeaders {
    var accept_Charset: String? = null
    var accept_Language: String? = null
    var accept_Encoding: String? = null
    var accept: String? = null
    var user_Agent: String? = null
}

class RequestedFormat {
    var asr = 0
    var tbr = 0.0
    var protocol: String? = null
    var format: String? = null
    var url: String? = null
    var vcodec: String? = null
    var format_note: String? = null
    var height: Any? = null
    var downloader_options: DownloaderOptions? = null
    var width: Any? = null
    var ext: String? = null
    var filesize = 0L
    var fps: Any? = null
    var format_id: String? = null
    var player_url: Any? = null
    var http_headers: HttpHeaders? = null
    var acodec: String? = null
    var abr = 0
}

class Thumbnail {
    var url: String? = null
    var id: String? = null
}