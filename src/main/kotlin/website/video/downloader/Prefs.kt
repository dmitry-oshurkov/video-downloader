package website.video.downloader

import java.lang.Double.*
import java.util.prefs.Preferences.*

object Prefs {

    /**
     * [userPrefs file](file:///home/dmitry/.java/.userPrefs/video-get/prefs.xml)
     */
    private val userPrefs = userRoot().node(APP_NAME)!!

    var isMaximized: Boolean
    var x: Double
    var y: Double
    var width: Double
    var height: Double
    var maxQuality: Boolean

    init {

        userPrefs.run {

            isMaximized = getBoolean("isMaximized", false)
            x = getDouble("x", NaN)
            y = getDouble("y", NaN)
            width = getDouble("width", 600.0)
            height = getDouble("height", 550.0)
            maxQuality = getBoolean("maxQuality", false)
        }
    }

    fun save() {

        userPrefs.run {

            putBoolean("isMaximized", isMaximized)
            putDouble("x", x)
            putDouble("y", y)
            putDouble("width", width)
            putDouble("height", height)
            putBoolean("maxQuality", maxQuality)
        }
    }
}
