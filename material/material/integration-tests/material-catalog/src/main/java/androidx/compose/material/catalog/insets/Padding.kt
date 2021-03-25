/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * TODO: Move to depending on Accompanist with prebuilts when we hit a stable version
 * https://github.com/google/accompanist/blob/main/insets/src/main/java/com/google/accompanist
 * /insets/Padding.kt
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

@file:JvmName("ComposeInsets")
@file:JvmMultifileClass

package androidx.compose.material.catalog.insets

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Selectively apply additional space which matches the width/height of any system bars present
 * on the respective edges of the screen.
 *
 * @param enabled Whether to apply padding using the system bars dimensions on the respective edges.
 * Defaults to `true`.
 */
@SuppressLint("ModifierInspectorInfo")
fun Modifier.systemBarsPadding(
    enabled: Boolean = true
): Modifier = composed {
    InsetsPaddingModifier(
        insetsType = LocalWindowInsets.current.systemBars,
        applyLeft = enabled,
        applyTop = enabled,
        applyRight = enabled,
        applyBottom = enabled
    )
}

/**
 * Apply additional space which matches the height of the status bars height along the top edge
 * of the content.
 */
@SuppressLint("ModifierInspectorInfo")
fun Modifier.statusBarsPadding(): Modifier = composed {
    InsetsPaddingModifier(
        insetsType = LocalWindowInsets.current.statusBars,
        applyTop = true
    )
}

/**
 * Apply additional space which matches the height of the navigation bars height
 * along the [bottom] edge of the content, and additional space which matches the width of
 * the navigation bars on the respective [left] and [right] edges.
 *
 * @param bottom Whether to apply padding to the bottom edge, which matches the navigation bars
 * height (if present) at the bottom edge of the screen. Defaults to `true`.
 * @param left Whether to apply padding to the left edge, which matches the navigation bars width
 * (if present) on the left edge of the screen. Defaults to `true`.
 * @param right Whether to apply padding to the right edge, which matches the navigation bars width
 * (if present) on the right edge of the screen. Defaults to `true`.
 */
@SuppressLint("ModifierInspectorInfo")
fun Modifier.navigationBarsPadding(
    bottom: Boolean = true,
    left: Boolean = true,
    right: Boolean = true
): Modifier = composed {
    InsetsPaddingModifier(
        insetsType = LocalWindowInsets.current.navigationBars,
        applyLeft = left,
        applyRight = right,
        applyBottom = bottom
    )
}

/**
 * Apply additional space which matches the height of the [WindowInsets.ime] (on-screen keyboard)
 * height along the bottom edge of the content.
 *
 * This method has no special handling for the [WindowInsets.navigationBars], which usually
 * intersect the [WindowInsets.ime]. Most apps will usually want to use the
 * [Modifier.navigationBarsWithImePadding] modifier.
 */
@SuppressLint("ModifierInspectorInfo")
fun Modifier.imePadding(): Modifier = composed {
    InsetsPaddingModifier(
        insetsType = LocalWindowInsets.current.ime,
        applyLeft = true,
        applyRight = true,
        applyBottom = true,
    )
}

/**
 * Apply additional space which matches the height of the [WindowInsets.ime] (on-screen keyboard)
 * height and [WindowInsets.navigationBars]. This is what apps should use to handle any insets
 * at the bottom of the screen.
 */
@SuppressLint("ModifierInspectorInfo")
fun Modifier.navigationBarsWithImePadding(): Modifier = composed {
    InsetsPaddingModifier(
        insetsType = LocalWindowInsets.current.ime,
        minimumInsetsType = LocalWindowInsets.current.navigationBars,
        applyLeft = true,
        applyRight = true,
        applyBottom = true,
    )
}

private data class InsetsPaddingModifier(
    private val insetsType: InsetsType,
    private val minimumInsetsType: InsetsType? = null,
    private val applyLeft: Boolean = false,
    private val applyTop: Boolean = false,
    private val applyRight: Boolean = false,
    private val applyBottom: Boolean = false,
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val transformedInsets = if (minimumInsetsType != null) {
            // If we have a minimum insets, coerce each dimensions
            insetsType.coerceEachDimensionAtLeast(minimumInsetsType)
        } else insetsType

        val left = if (applyLeft) transformedInsets.left else 0
        val top = if (applyTop) transformedInsets.top else 0
        val right = if (applyRight) transformedInsets.right else 0
        val bottom = if (applyBottom) transformedInsets.bottom else 0
        val horizontal = left + right
        val vertical = top + bottom

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        val width = (placeable.width + horizontal)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height + vertical)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            placeable.place(left, top)
        }
    }
}

/**
 * Returns the current insets converted into a [PaddingValues].
 *
 * @param start Whether to apply the inset on the start dimension.
 * @param top Whether to apply the inset on the top dimension.
 * @param end Whether to apply the inset on the end dimension.
 * @param bottom Whether to apply the inset on the bottom dimension.
 * @param additionalHorizontal Value to add to the start and end dimensions.
 * @param additionalVertical Value to add to the top and bottom dimensions.
 */
@Composable
inline fun InsetsType.toPaddingValues(
    start: Boolean = true,
    top: Boolean = true,
    end: Boolean = true,
    bottom: Boolean = true,
    additionalHorizontal: Dp = 0.dp,
    additionalVertical: Dp = 0.dp,
) = toPaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom,
    additionalStart = additionalHorizontal,
    additionalTop = additionalVertical,
    additionalEnd = additionalHorizontal,
    additionalBottom = additionalVertical
)

/**
 * Returns the current insets converted into a [PaddingValues].
 *
 * @param start Whether to apply the inset on the start dimension.
 * @param top Whether to apply the inset on the top dimension.
 * @param end Whether to apply the inset on the end dimension.
 * @param bottom Whether to apply the inset on the bottom dimension.
 * @param additionalStart Value to add to the start dimension.
 * @param additionalTop Value to add to the top dimension.
 * @param additionalEnd Value to add to the end dimension.
 * @param additionalBottom Value to add to the bottom dimension.
 */
@Composable
fun InsetsType.toPaddingValues(
    start: Boolean = true,
    top: Boolean = true,
    end: Boolean = true,
    bottom: Boolean = true,
    additionalStart: Dp = 0.dp,
    additionalTop: Dp = 0.dp,
    additionalEnd: Dp = 0.dp,
    additionalBottom: Dp = 0.dp,
): PaddingValues = with(LocalDensity.current) {
    val layoutDirection = LocalLayoutDirection.current
    PaddingValues(
        start = additionalStart + when {
            start && layoutDirection == LayoutDirection.Ltr -> this@toPaddingValues.left.toDp()
            start && layoutDirection == LayoutDirection.Rtl -> this@toPaddingValues.right.toDp()
            else -> 0.dp
        },
        top = additionalTop + when {
            top -> this@toPaddingValues.top.toDp()
            else -> 0.dp
        },
        end = additionalEnd + when {
            end && layoutDirection == LayoutDirection.Ltr -> this@toPaddingValues.right.toDp()
            end && layoutDirection == LayoutDirection.Rtl -> this@toPaddingValues.left.toDp()
            else -> 0.dp
        },
        bottom = additionalBottom + when {
            bottom -> this@toPaddingValues.bottom.toDp()
            else -> 0.dp
        }
    )
}
