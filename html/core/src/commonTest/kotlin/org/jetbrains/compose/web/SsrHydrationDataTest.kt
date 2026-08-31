package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

internal const val SSR_HYDRATION_DATA_FIXTURE_URL = "/base/kotlin/ssr-hydration-data.html"
internal const val SSR_HYDRATION_DATA_ROOT_ID = "ssr-hydration-data-root"
internal const val SSR_HYDRATION_DATA_BUTTON_ID = "ssr-hydration-data-button"
internal const val SSR_HYDRATION_DATA_VALUE_ID = "ssr-hydration-data-value"

internal data class SsrHydrationData(
    val label: String,
    val count: Int,
)

internal fun SsrHydrationData.toJson(): String =
    """{"label":"${label.toJsonStringContent()}","count":$count}"""

@Composable
internal fun SsrHydrationDataContent(
    label: String,
    count: Int,
    increment: () -> Unit,
) {
    Button(
        attrs = {
            id(SSR_HYDRATION_DATA_BUTTON_ID)
            onClick { increment() }
        }
    ) {
        Text("Increment")
    }
    Span(attrs = { id(SSR_HYDRATION_DATA_VALUE_ID) }) {
        Text("$label: $count")
    }
}

private fun String.toJsonStringContent(): String = buildString {
    this@toJsonStringContent.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
}
