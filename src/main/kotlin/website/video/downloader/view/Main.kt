package website.video.downloader.view

import javafx.beans.binding.*
import javafx.geometry.Pos.*
import javafx.scene.control.*
import javafx.scene.control.SelectionMode.*
import javafx.scene.input.DataFormat.*
import javafx.scene.layout.Priority.*
import tornadofx.*
import website.video.downloader.*
import website.video.downloader.BuildConfig.VERSION
import website.video.downloader.DownloadState.*
import website.video.downloader.Styles.Companion.backImage
import website.video.downloader.Styles.Companion.channel
import website.video.downloader.Styles.Companion.downloadButton
import website.video.downloader.Styles.Companion.jobTitle
import website.video.downloader.Styles.Companion.main
import website.video.downloader.Styles.Companion.progressLabels
import website.video.downloader.Styles.Companion.videoButton
import java.io.*
import java.net.*
import java.time.*

class Main : View() {

    private val canDownload = false.toProperty()
    private val jobsStatus = stringBinding(jobs) { "${messages["main.status.video.count"]}: ${jobs.size}  |  ${messages["main.status.video.total"]}: $totalTime" }
    private val playingStatus = stringBinding(playingTime) { "${messages["main.status.playing.time"]}: ${playingTime.value}  |  " }

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

                label(readyCount) {
                    visibleWhen { readyCount.greaterThan(0).or(readyCount.lessThan(0)) }
                }

                checkbox("max quality", Prefs.maxQuality.toProperty()) {
                    action {
                        Prefs.maxQuality = isSelected
                        Prefs.save()
                    }
                    prefHeight = 40.0
                    isFocusTraversable = false
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

                onDoubleClick {
                    selectedItem
                        ?.takeIf { it.state == COMPLETED }
                        ?.also { it.showVideo() }
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
                                spacing = 8.0
                                padding = insets(0.0, 0.0, 0.0, 10.0)
                                prefWidth = 0.0 // for label autosize
                                prefHeight = 0.0 // for label autosize
                                minHeight = 70.0
                                anchorpaneConstraints {
                                    rightAnchor = 0
                                    leftAnchor = 0
                                }

                                label(it.titleProperty()) {
                                    addClass(jobTitle)
                                }

                                anchorpane {

                                    val completed = it.stateProperty().isEqualTo(COMPLETED)
                                    val canReload = it.stateProperty().isEqualTo(IN_PROGRESS).or(it.needReload)
                                    it.durationProperty().onChange { jobsStatus.invalidate() }

                                    gridpane {

                                        row {

                                            hbox {
                                                spacing = 5.0
                                                minWidth = 100.0
                                                minHeight = 21.0
                                                alignment = CENTER_LEFT
                                                visibleWhen(it.durationProperty().isNotNull)

                                                glyph("\uf210") { prefHeight = 16.0 }
                                                label(it.durationProperty())
                                            }

                                            label(it.uploaderProperty()) {
                                                addClass(channel)
                                            }
                                        }

                                        row {

                                            hbox {
                                                spacing = 5.0
                                                minHeight = 21.0
                                                alignment = CENTER_LEFT
                                                visibleWhen(it.fileSizeTextProperty().isNotNull)

                                                glyph("\uf318") { prefHeight = 16.0 }
                                                label(it.fileSizeTextProperty())
                                            }

                                            hbox {
                                                spacing = 5.0
                                                alignment = CENTER_LEFT

                                                glyph("\uf40b") {
                                                    prefHeight = 16.0
                                                    removeWhen(completed.or(it.remote))
                                                }

                                                label(it.speedProperty()) {
                                                    addClass(progressLabels)
                                                    removeWhen(completed.or(it.remote))
                                                }

                                                progressbar(it.progressProperty()) {
                                                    removeWhen(completed)
                                                    prefHeight = 16.0
                                                    prefWidth = 150.0
                                                }

                                                label(it.etaProperty()) {
                                                    addClass(progressLabels)
                                                    removeWhen(completed.or(it.remote))
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
                                                if (canReload.value)
                                                    item.reload()
                                                else
                                                    item.showVideo()
                                            }
                                            enableWhen(canReload.or(completed))
                                            graphic = imageview(Bindings.`when`(canReload).then("images/reload.png").otherwise("images/play.png"))
                                            tooltipProperty().bind(Bindings.`when`(canReload).then(Tooltip(messages["main.btn.reload"])).otherwise(Tooltip(messages["main.btn.play"])))
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
            }

            hbox {
                label(jobsStatus)
                pane { hgrow = ALWAYS }
                label(playingStatus)
                label(VERSION)
            }
        }
    }

    private fun Job.reload() = run {
        stop()
        stateProperty().set(NEW)
    }

    private fun Job.showVideo() = runAsync {
        desktop.open(File(file!!))
        runPlayingTimer()
    }

    private fun Job.browseVideoUrl() = runAsync { desktop.browse(URI("$url&t=${LocalTime.parse(this@browseVideoUrl.duration!!).toSecondOfDay() - 15}")) }

    private fun runClipboardMonitor() = runLater {

        while (primaryStage.isShowing) {

            val url = clipboard.getContent(PLAIN_TEXT) as? String
            canDownload.value = url?.isYoutubeUrl() == true || url?.isPHUrl() == true || url?.isDzenUrl() == true

            runLater(800.millis) { }.completedProperty.awaitUntil()
        }
    }
}
