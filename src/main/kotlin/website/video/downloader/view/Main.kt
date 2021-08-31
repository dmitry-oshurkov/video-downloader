package website.video.downloader.view

import javafx.beans.binding.*
import javafx.geometry.Pos.*
import javafx.scene.control.*
import javafx.scene.control.SelectionMode.*
import javafx.scene.input.DataFormat.*
import javafx.scene.layout.Priority.*
import kotlinx.coroutines.*
import tornadofx.*
import website.video.downloader.*
import website.video.downloader.BuildConfig.VERSION
import website.video.downloader.DownloadState.*
import website.video.downloader.Job
import website.video.downloader.Styles.Companion.backImage
import website.video.downloader.Styles.Companion.downloadButton
import website.video.downloader.Styles.Companion.jobTitle
import website.video.downloader.Styles.Companion.main
import website.video.downloader.Styles.Companion.progressLabels
import website.video.downloader.Styles.Companion.videoButton
import java.io.*
import java.net.*

class Main : View() {

    private val coroutineScope = MainScope()
    private val canDownload = false.toProperty()
    private val jobsStatus = stringBinding(jobs) { "${messages["main.status.video.count"]}: ${jobs.size}  |  ${messages["main.status.video.total"]}: $totalTime" }

    init {
        title = messages["main.title"]
        runClipboardMonitor()
    }

    override val root = stackpane {

        imageview("images/background.jpg") {
            addClass(backImage)
            hiddenWhen(primaryStage.maximizedProperty())
        }

        vbox {
            padding = insets(10.0, 10.0, 3.0, 10.0)
            addClass(main)

            hbox {
                prefHeight = 60.0
                minHeight = 60.0
                padding = insets(5.0, 0.0, 0.0, 5.0)
                onDoubleClick { openOutDirInFiles() }

                button(messages["main.btn.paste-link"], imageview("images/youtube.png")) {
                    action {
                        val url = clipboard.getContent(PLAIN_TEXT) as? String
                        placeToQueue(url)
                    }
                    prefHeight = 40.0
                    isFocusTraversable = false
                    addClass(downloadButton)
                    enableWhen(canDownload)
                }

                region { hgrow = ALWAYS }

                checkbox("max quality", Prefs.maxQuality.toProperty()) {
                    action {
                        Prefs.maxQuality = isSelected
                        Prefs.save()
                    }
                    prefHeight = 40.0
                }
            }

            listview(jobs) {
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
                                    it.durationProperty().onChange { jobsStatus.invalidate() }

                                    hbox {
                                        spacing = 15.0

                                        vbox {
                                            spacing = 10.0

                                            hbox {
                                                spacing = 5.0
                                                visibleWhen(it.durationProperty().isNotNull)

                                                glyph("\uf210") {
                                                    prefHeight = 16.0
                                                }
                                                label(it.durationProperty())
                                            }

                                            hbox {
                                                spacing = 5.0
                                                visibleWhen(it.fileSizeTextProperty().isNotNull)

                                                glyph("\uf318") {
                                                    prefHeight = 16.0
                                                }
                                                label(it.fileSizeTextProperty())
                                            }
                                        }

                                        vbox {
                                            spacing = 10.0

                                            hbox {
                                                spacing = 5.0
                                                hiddenWhen(completed.or(it.progressProperty().isNull))

                                                progressbar(it.progressProperty()) {
                                                    prefHeight = 16.0
                                                    prefWidth = 150.0
                                                }

                                                label(it.etaProperty()) {
                                                    addClass(progressLabels)
                                                }
                                            }

                                            hbox {
                                                spacing = 5.0
                                                visibleWhen(it.speedProperty().isNotNull.or(it.formatProperty().isNotNull))

                                                glyph("\uf40b") {
                                                    prefHeight = 16.0
                                                    removeWhen(completed)
                                                }

                                                label(it.speedProperty()) {
                                                    addClass(progressLabels)
                                                    removeWhen(completed)
                                                }

                                                label(it.formatTextProperty()) {
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

                                        btn {
                                            action {
                                                if (it.needReload.value)
                                                    item.reload()
                                                else
                                                    item.showVideo()
                                            }
                                            enableWhen(completed.or(it.needReload))
                                            graphic = imageview(Bindings.`when`(it.needReload).then("images/reload.png").otherwise("images/play.png"))
                                            tooltipProperty().bind(Bindings.`when`(it.needReload).then(Tooltip(messages["main.btn.reload"])).otherwise(Tooltip(messages["main.btn.play"])))
                                            addClass(videoButton)
                                        }

                                        btn(messages["main.btn.browse"], "images/browse.png") {
                                            action { item.browseVideoUrl() }
                                            addClass(videoButton)
                                        }

                                        btn(messages["main.btn.delete"], "images/delete.png") {
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

            hbox {
                label(jobsStatus)
                pane { hgrow = ALWAYS }
                label(VERSION)
            }
        }
    }

    private fun Job.reload() = runAsync { runDownload() }
    private fun Job.showVideo() = runAsync { desktop.open(File(file!!)) }
    private fun Job.browseVideoUrl() = runAsync { desktop.browse(URI(url)) }

    private fun runClipboardMonitor() = runLater {

        while (primaryStage.isShowing) {

            val url = clipboard.getContent(PLAIN_TEXT) as? String
            canDownload.value = url?.isYoutubeUrl()

            runLater(300.millis) { }.completedProperty.awaitUntil()
        }
    }
}
