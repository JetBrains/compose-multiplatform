package com.sample

import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import com.sample.components.*
import com.sample.content.*
import com.sample.style.AppStylesheet

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