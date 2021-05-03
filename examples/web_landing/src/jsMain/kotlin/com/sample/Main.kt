package com.sample

import androidx.compose.web.css.Style
import androidx.compose.web.renderComposable
import com.sample.components.*
import com.sample.content.*
import com.sample.style.AppStylesheet
import org.w3c.dom.HTMLElement


fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStylesheet)

        Layout {
            Header()
            MainContentLayout {
                Intro()
                ComposeWebLibraries()
                GetStarted()
                CodeSamples()
                JoinUs()
            }
            PageFooter()
        }
    }
}