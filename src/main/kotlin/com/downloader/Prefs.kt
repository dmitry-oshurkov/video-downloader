package com.downloader

import java.util.prefs.Preferences.*

object Prefs {

    private val userPrefs = userRoot().node(APP_NAME)!!

    var isMaximized: Boolean
    var x: Double
    var y: Double
    var width: Double
    var height: Double

    init {

        userPrefs.run {

            isMaximized = getBoolean("isMaximized", false)
            x = getDouble("x", 0.0)
            y = getDouble("y", 0.0)
            width = getDouble("width", 600.0)
            height = getDouble("height", 800.0)
        }
    }

    fun save() {

        userPrefs.run {

            putBoolean("isMaximized", isMaximized)
            putDouble("x", x)
            putDouble("y", y)
            putDouble("width", width)
            putDouble("height", height)
        }
    }
}
