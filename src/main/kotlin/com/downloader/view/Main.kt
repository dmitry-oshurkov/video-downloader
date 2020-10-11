package com.downloader.view

import com.downloader.*
import com.downloader.DownloadState.*
import com.downloader.Styles.Companion.backImage
import com.downloader.Styles.Companion.downloadButton
import com.downloader.Styles.Companion.jobTitle
import com.downloader.Styles.Companion.main
import com.downloader.Styles.Companion.progressLabels
import com.downloader.Styles.Companion.videoButton
import javafx.beans.property.*
import javafx.geometry.Pos.*
import javafx.scene.control.*
import javafx.scene.control.SelectionMode.*
import javafx.scene.input.DataFormat.*
import javafx.scene.layout.Priority.*
import kotlinx.coroutines.*
import tornadofx.*
import java.awt.*
import java.io.*
import java.net.*

class Main : View("Загрузка видео") {

    private val desktop = Desktop.getDesktop()
    private var canDownload: BooleanProperty = false.toProperty()
    private var list: ListView<DownloadJob> by singleAssign()

    init {
        loadJobs()
        runJobMonitor()
        runClipboardMonitor()
    }

    override val root = stackpane {

        imageview("images/background.jpg") {
            addClass(backImage)
            hiddenWhen(primaryStage.maximizedProperty())
        }

        vbox {
            prefWidth = 800.0
            prefHeight = 800.0
            padding = insets(10.0)
            addClass(main)

            hbox {
                prefHeight = 60.0
                padding = insets(5.0, 0.0, 0.0, 5.0)
                onDoubleClick { openOutDirInFiles() }

                button("Вставить ссылку") {
                    action {
                        val url = clipboard.getContent(PLAIN_TEXT) as? String
                        placeToQueue(url)
                    }
                    prefHeight = 40.0
                    isFocusTraversable = false
                    addClass(downloadButton)
                    enableWhen(canDownload)
                }
            }

            list = listview(jobs) {

                vgrow = ALWAYS
                selectionModel.selectionMode = SINGLE
                selectionModel.selectFirst()

                items.onChange {
                    while (it.next()) {
                        if (it.wasAdded()) {
                            val added = it.addedSubList.first()
                            selectionModel.select(added)
                            scrollTo(added)
                        }
                    }
                }

                cellFormat {

                    graphic = hbox {
                        padding = insets(5.0)

                        tooltip { textProperty().bind(it.tooltipProperty()) }

                        imageview(it.thumbnailImageProperty()) {
                            fitHeight = 70.0
                            fitWidth = 124.0
                        }

                        anchorpane {
                            hgrow = ALWAYS

                            vbox {
                                spacing = 12.0
                                padding = insets(0.0, 0.0, 0.0, 10.0)
                                prefWidth = 0.0 // for label autosize
                                prefHeight = 0.0 // for label autosize
                                anchorpaneConstraints {
                                    rightAnchor = 0
                                    leftAnchor = 0
                                }

                                label(it.titleProperty()) {
                                    addClass(jobTitle)
                                }

                                anchorpane {

                                    val completed = it.stateProperty().isEqualTo(COMPLETED)

                                    hbox {
                                        spacing = 15.0

                                        vbox {
                                            spacing = 10.0

                                            hbox {
                                                spacing = 5.0
                                                visibleWhen(it.durationProperty().isNotNull)

                                                glyph("\uf210")
                                                label(it.durationProperty())
                                            }

                                            hbox {
                                                spacing = 5.0
                                                visibleWhen(it.fileSizeProperty().isNotNull)

                                                glyph("\uf318")
                                                label(it.fileSizeProperty())
                                            }
                                        }

                                        vbox {
                                            spacing = 5.0

                                            hbox {
                                                spacing = 5.0
                                                hiddenWhen(completed.or(it.progressProperty().isNull))

                                                progressbar(it.progressProperty()) {
                                                    prefWidth = 150.0
                                                }

                                                label(it.etaProperty()) {
                                                    addClass(progressLabels)
                                                }
                                            }

                                            hbox {
                                                spacing = 5.0
                                                visibleWhen(it.speedProperty().isNotNull.or(it.videoFormatProperty().isNotNull))

                                                glyph("\uf40b") {
                                                    removeWhen(completed)
                                                }

                                                label(it.speedProperty()) {
                                                    addClass(progressLabels)
                                                    removeWhen(completed)
                                                }

                                                label(it.videoFormatProperty()) {
                                                    visibleWhen(completed)
                                                }
                                            }
                                        }
                                    }

                                    hbox {
                                        spacing = 3.0
                                        alignment = BOTTOM_RIGHT
                                        anchorpaneConstraints {
                                            rightAnchor = 0
                                            bottomAnchor = 0
                                        }

                                        button("Просмотр", "images/play-icon.png") {
                                            enableWhen(completed)
                                            action { item.showVideo() }
                                            addClass(videoButton)
                                        }

                                        button("Открыть ссылку", "images/web-browser-icon.png") {
                                            action { item.browseVideoUrl() }
                                            addClass(videoButton)
                                        }

                                        button("Удалить", "images/remove-icon.png") {
                                            action { item.delete() }
                                            addClass(videoButton)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                onDoubleClick {
                    selectedItem
                        ?.takeIf { it.state == COMPLETED }
                        ?.also { it.showVideo() }
                }
            }
        }
    }

    private fun DownloadJob.showVideo() = runAsync { desktop.open(File(file!!)) }
    private fun DownloadJob.browseVideoUrl() = runAsync { desktop.browse(URI(url)) }

    private fun runClipboardMonitor() = GlobalScope.launch {

        while (isActive) {

            runLater {
                val url = clipboard.getContent(PLAIN_TEXT) as? String
                canDownload.value = url?.isYoutubeUrl()
            }

            delay(300)
        }
    }
}
