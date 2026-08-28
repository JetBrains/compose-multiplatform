package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Style
import org.jetbrains.compose.web.dom.Text

internal const val SSR_HYDRATION_FIXTURE_URL = "/base/kotlin/ssr-hydration.html"
internal const val SSR_HYDRATION_STYLE_ID = "ssr-hydration-style"
internal const val SSR_HYDRATION_BUTTON_ID = "ssr-hydration-button"
internal const val SSR_HYDRATION_COUNT_ID = "ssr-hydration-count"

// Used by both JVM rendering and JS hydration
@Composable
internal fun SsrHydrationContent(
    count: Int,
    increment: () -> Unit,
) {
    Style(applyAttrs = { id(SSR_HYDRATION_STYLE_ID) }) {
        "#$SSR_HYDRATION_COUNT_ID" style {
            color(if (count == 0) Color.red else Color.green)
            opacity(0.5)
            width(33.percent)
            property("flex-grow", 1)
        }
    }
    Button(
        attrs = {
            id(SSR_HYDRATION_BUTTON_ID)
            onClick { increment() }
        }
    ) {
        Text("Increment")
    }
    Span(attrs = { id(SSR_HYDRATION_COUNT_ID) }) {
        Text("Count: $count")
    }
}
