package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Style
import org.jetbrains.compose.web.dom.Text

internal const val SSR_HYDRATION_FIXTURE_URL = "/base/kotlin/ssr-hydration.html"
internal const val SSR_HYDRATION_STYLE_ID = "ssr-hydration-style"
internal const val SSR_HYDRATION_BUTTON_ID = "ssr-hydration-button"
internal const val SSR_HYDRATION_COUNT_ID = "ssr-hydration-count"
internal const val SSR_HYDRATION_SCRIPT_ID = "ssr-hydration-script"
internal const val SSR_HYDRATION_SCRIPT_CONTENT = "first\r\nsecond\rthird\u0000fourth"
internal const val SSR_HYDRATION_NORMALIZED_SCRIPT_CONTENT = "first\nsecond\nthird\uFFFDfourth"
internal const val SSR_HYDRATION_RENDERED_AT_ID = "ssr-hydration-rendered-at"
internal const val SSR_HYDRATION_SERVER_RENDERED_AT = "2026-08-31T10:15:30Z"
internal const val SSR_HYDRATION_CLIENT_RENDERED_AT = "2026-08-31T10:15:42Z"

// Used by both JVM rendering and JS hydration
@Composable
internal fun SsrHydrationContent(
    count: Int,
    renderedAt: String,
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
    Script(InlineScript(SSR_HYDRATION_SCRIPT_CONTENT)) {
        id(SSR_HYDRATION_SCRIPT_ID)
        type(ScriptType.TextPlain)
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
    // The server clock cannot be reproduced by the client, so its text is not hydrated strictly.
    Span(attrs = {
        id(SSR_HYDRATION_RENDERED_AT_ID)
        allowHydrationMismatch()
        attr("data-rendered-at", renderedAt)
    }) {
        Text("Rendered at $renderedAt")
    }
}
