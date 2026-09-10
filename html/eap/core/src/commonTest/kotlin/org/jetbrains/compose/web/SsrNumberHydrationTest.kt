package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Style
import org.jetbrains.compose.web.dom.Text

internal const val SSR_NUMBER_HYDRATION_FIXTURE_URL = "/base/kotlin/ssr-number-hydration.html"
internal const val SSR_NUMBER_HYDRATION_STYLE_ID = "ssr-number-hydration-style"
internal const val SSR_NUMBER_HYDRATION_BUTTON_ID = "ssr-number-hydration-button"
internal const val SSR_NUMBER_HYDRATION_RULE_ID = "ssr-number-hydration-rule"
internal const val SSR_NUMBER_HYDRATION_INLINE_ID = "ssr-number-hydration-inline"

/**
 * Values requiring portable CSS formatting to agree in JVM rendering and JS hydration.
 *
 * Kotlin/JS does not narrow [percentage], holding 33.333333333333336 where JVM holds 33.333332.
 * Binary32 serialization keeps the stylesheet and style attribute unchanged during hydration.
 *
 * Each value is deliberately one operation from an integer. After several operations, JS
 * double-precision intermediates diverge from JVM Floats; final formatting cannot fix that.
 */
@Composable
internal fun SsrNumberHydrationContent(
    count: Int,
    increment: () -> Unit,
) {
    // Parameter-derived so neither compiler can fold them into constants.
    val percentage = 100f / (count + 3)
    val alpha = 1f / (count + 3)
    val size = 10f / (count + 3)

    Style(applyAttrs = { id(SSR_NUMBER_HYDRATION_STYLE_ID) }) {
        "#$SSR_NUMBER_HYDRATION_RULE_ID" style {
            backgroundColor(rgba(0, 0, 0, alpha))
            opacity(alpha)
            width(percentage.percent)
            // A Float passed to a Number API (opacity, flex-grow, etc.), beyond Float overloads.
            property("flex-grow", percentage)
        }
    }
    Button(
        attrs = {
            id(SSR_NUMBER_HYDRATION_BUTTON_ID)
            onClick { increment() }
        }
    ) {
        Text("Increment")
    }
    Span(attrs = { id(SSR_NUMBER_HYDRATION_RULE_ID) }) {
        Text("Count: $count")
    }
    // Exercises both CSS hydration paths: raw stylesheet text and a style attribute.
    Div(
        attrs = {
            id(SSR_NUMBER_HYDRATION_INLINE_ID)
            style {
                width(size.px)
                opacity(alpha)
                property("order", count)
            }
        }
    )
}
