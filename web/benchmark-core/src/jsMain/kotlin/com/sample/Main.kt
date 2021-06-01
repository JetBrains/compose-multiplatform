package com.sample

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*
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