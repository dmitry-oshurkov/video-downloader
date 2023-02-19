package website.video.downloader

import javafx.scene.effect.*
import javafx.scene.effect.BlurType.*
import javafx.scene.paint.Color.*
import javafx.scene.text.FontWeight.*
import tornadofx.*
import tornadofx.FXVisibility.*

class Styles : Stylesheet() {

    companion object {
        val glyphs = loadFont("/fonts/webhostinghub-glyphs.ttf", 10.0)!!
        val main by cssclass()
        val backImage by cssclass()
        val jobTitle by cssclass()
        val channel by cssclass()
        val progressLabels by cssclass()
        val downloadButton by cssclass()
        val videoButton by cssclass()
        val glyphLabel by cssclass()
    }

    init {

        scrollBar {

            backgroundColor += TRANSPARENT

            s(incrementButton, decrementButton, incrementArrow, decrementArrow) { visibility = HIDDEN }

            thumb {

                backgroundColor += LIGHTGRAY
                backgroundRadius += box(0.px)
                backgroundInsets += box(0.px)

                and(hover) { backgroundColor += DARKGRAY }
            }
        }

        main {
            backgroundColor += TRANSPARENT
        }

        backImage {
            opacity = 0.08
        }

        progressLabels {
            fontFamily = "DejaVu Sans Mono"
        }

        val toolButton = mixin {
            fontSize = 14.px
            fontWeight = BOLD

            and(hover) { effect = hoverButtonShadow() }
            and(pressed) { effect = pressedButtonShadow() }
        }

        downloadButton {
            +toolButton
        }

        videoButton {
            and(hover) { effect = hoverButtonShadow() }
            and(pressed) { effect = pressedButtonShadow() }
        }

        jobTitle {
            fontSize = 10.pt
            fontWeight = BOLD
        }

        channel {
            fontSize = 10.pt
            fontWeight = BOLD
            textFill = c("#75659f")
        }

        listView {
            backgroundColor += TRANSPARENT
            opacity = 1.0
        }

        listCell {
            backgroundRadius += box(4.px)
            and(even) { backgroundColor += c(0.56078434, 0.7372549, 0.56078434, 0.08) }
            and(odd, empty) { backgroundColor += TRANSPARENT }
            and(selected) {
                label { textFill = BLACK }
                jobTitle { textFill = MIDNIGHTBLUE }
                channel { textFill = SLATEBLUE }
                backgroundColor += c(0.56078434, 0.7372549, 0.56078434, 0.25)
            }
        }

        glyphLabel {
            font = glyphs
            textFill = DIMGREY
        }
    }

    private fun hoverButtonShadow() = DropShadow(GAUSSIAN, DEEPSKYBLUE, 15.0, 0.0, shadowOffsetX, shadowOffsetX)
    private fun pressedButtonShadow() = DropShadow(GAUSSIAN, LIME, 15.0, 0.0, shadowOffsetX, shadowOffsetX)

    private val shadowOffsetX = 2.0
}
