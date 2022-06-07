/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Sets the width to that of [insets] at the [start][androidx.compose.ui.Alignment.Start]
 * of the screen, using either [left][WindowInsets.getLeft] or [right][WindowInsets.getRight],
 * depending on the [LayoutDirection].
 *
 * When used, the [WindowInsets][android.view.WindowInsets] will respect the consumed
 * insets from [windowInsetsPadding] and [consumeWindowInsets], but won't consume any
 * insets.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsStartWidthSample
 */
@Suppress("ModifierInspectorInfo")
@Stable
fun Modifier.windowInsetsStartWidth(insets: WindowInsets) = this.then(
    DerivedWidthModifier(insets, debugInspectorInfo {
        name = "insetsStartWidth"
        properties["insets"] = insets
    }) { layoutDirection, density ->
        if (layoutDirection == LayoutDirection.Ltr) {
            getLeft(density, layoutDirection)
        } else {
            getRight(density, layoutDirection)
        }
    }
)

/**
 * Sets the width to that of [insets] at the [end][androidx.compose.ui.Alignment.End]
 * of the screen, using either [left][WindowInsets.getLeft] or [right][WindowInsets.getRight],
 * depending on the [LayoutDirection].
 *
 * When used, the [WindowInsets][android.view.WindowInsets] will respect the consumed
 * insets from [windowInsetsPadding] and [consumeWindowInsets], but won't consume any
 * insets.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsEndWidthSample
 */
@Suppress("ModifierInspectorInfo")
@Stable
fun Modifier.windowInsetsEndWidth(insets: WindowInsets) = this.then(
    DerivedWidthModifier(insets, debugInspectorInfo {
        name = "insetsEndWidth"
        properties["insets"] = insets
    }) { layoutDirection, density ->
        if (layoutDirection == LayoutDirection.Rtl) {
            getLeft(density, layoutDirection)
        } else {
            getRight(density, layoutDirection)
        }
    }
)

/**
 * Sets the height to that of [insets] at the [top][WindowInsets.getTop] of the screen.
 *
 * When used, the [WindowInsets][android.view.WindowInsets] will respect the consumed
 * insets from [windowInsetsPadding] and [consumeWindowInsets], but won't consume any
 * insets.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsTopHeightSample
 */
@Suppress("ModifierInspectorInfo")
@Stable
fun Modifier.windowInsetsTopHeight(insets: WindowInsets) = this.then(
    DerivedHeightModifier(insets, debugInspectorInfo {
        name = "insetsTopHeight"
        properties["insets"] = insets
    }) {
        getTop(it)
    }
)

/**
 * Sets the height to that of [insets] at the [bottom][WindowInsets.getBottom] of the screen.
 *
 * When used, the [WindowInsets][android.view.WindowInsets] will respect the consumed
 * insets from [windowInsetsPadding] and [consumeWindowInsets], but won't consume any
 * insets.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsBottomHeightSample
 */
@Suppress("ModifierInspectorInfo")
@Stable
fun Modifier.windowInsetsBottomHeight(insets: WindowInsets) = this.then(
    DerivedHeightModifier(insets, debugInspectorInfo {
        name = "insetsBottomHeight"
        properties["insets"] = insets
    }) {
        getBottom(it)
    }
)

/**
 * Sets the width based on [widthCalc]. If the width is 0, the height will also always be 0
 * and the content will not be placed.
 */
@Stable
private class DerivedWidthModifier(
    private val insets: WindowInsets,
    inspectorInfo: InspectorInfo.() -> Unit,
    private val widthCalc: WindowInsets.(LayoutDirection, Density) -> Int
) : LayoutModifier, ModifierLocalConsumer, InspectorValueInfo(inspectorInfo) {
    private var unconsumedInsets: WindowInsets by mutableStateOf(insets)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val width = unconsumedInsets.widthCalc(layoutDirection, this)
        if (width == 0) {
            return layout(0, 0) { }
        }
        // check for height first
        val childConstraints = constraints.copy(minWidth = width, maxWidth = width)
        val placeable = measurable.measure(childConstraints)
        return layout(width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        unconsumedInsets = insets.exclude(ModifierLocalConsumedWindowInsets.current)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DerivedWidthModifier) {
            return false
        }
        return insets == other.insets && widthCalc == other.widthCalc
    }

    override fun hashCode(): Int = 31 * insets.hashCode() + widthCalc.hashCode()
}

/**
 * Sets the height based on [heightCalc]. If the height is 0, the width will also always be 0
 * and the content will not be placed.
 */
@Stable
private class DerivedHeightModifier(
    private val insets: WindowInsets,
    inspectorInfo: InspectorInfo.() -> Unit,
    private val heightCalc: WindowInsets.(Density) -> Int
) : LayoutModifier, ModifierLocalConsumer, InspectorValueInfo(inspectorInfo) {
    private var unconsumedInsets: WindowInsets by mutableStateOf(insets)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val height = unconsumedInsets.heightCalc(this)
        if (height == 0) {
            return layout(0, 0) { }
        }
        // check for height first
        val childConstraints = constraints.copy(minHeight = height, maxHeight = height)
        val placeable = measurable.measure(childConstraints)
        return layout(placeable.width, height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        unconsumedInsets = insets.exclude(ModifierLocalConsumedWindowInsets.current)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DerivedHeightModifier) {
            return false
        }
        return insets == other.insets && heightCalc == other.heightCalc
    }

    override fun hashCode(): Int = 31 * insets.hashCode() + heightCalc.hashCode()
}