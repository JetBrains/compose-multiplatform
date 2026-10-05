package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

// Shared declarations for the JVM-rendered fixture and the JS hydration test.
internal const val SSR_HYDRATION_STATE_FIXTURE_URL = "./ssr-hydration-state.html"
internal const val SSR_HYDRATION_STATE_STREAM_FAST_FIXTURE_URL = "./ssr-hydration-state-stream-fast.html"
internal const val SSR_HYDRATION_STATE_STREAM_STRICT_FIXTURE_URL = "./ssr-hydration-state-stream-strict.html"
internal const val SSR_HYDRATION_STATE_BUTTON_ID = "ssr-hydration-state-button"
internal const val SSR_HYDRATION_STATE_VALUE_ID = "ssr-hydration-state-value"

internal data class SsrHydrationState(
    val label: String,
    val count: Int,
)

internal fun SsrHydrationState.toJson(): String =
    """{"label":"${label.toJsonStringContent()}","count":$count}"""

@Composable
internal fun SsrHydrationStateApplication(
    initialState: SsrHydrationState,
    buttonId: String = SSR_HYDRATION_STATE_BUTTON_ID,
    valueId: String = SSR_HYDRATION_STATE_VALUE_ID,
) {
    var count by remember(initialState) { mutableStateOf(initialState.count) }
    SsrHydrationStateContent(
        label = initialState.label,
        count = count,
        increment = { count++ },
        buttonId = buttonId,
        valueId = valueId,
    )
}

@Composable
internal fun SsrHydrationStateContent(
    label: String,
    count: Int,
    increment: () -> Unit,
    buttonId: String = SSR_HYDRATION_STATE_BUTTON_ID,
    valueId: String = SSR_HYDRATION_STATE_VALUE_ID,
) {
    Button(
        attrs = {
            id(buttonId)
            onClick { increment() }
        }
    ) {
        Text("Increment")
    }
    Span(attrs = { id(valueId) }) {
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
