/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

@Suppress("ModifierInspectorInfo")
internal fun Modifier.textFieldMinSize(style: TextStyle) = composed {
    val density = LocalDensity.current
    val resourceLoader = LocalFontLoader.current
    val layoutDirection = LocalLayoutDirection.current

    val minSizeState = remember { TextFieldSize(layoutDirection, density, resourceLoader, style) }
    minSizeState.update(layoutDirection, density, resourceLoader, style)

    Modifier.layout { measurable, constraints ->
        Modifier.defaultMinSize()
        val minSize = minSizeState.minSize

        val childConstraints = constraints.copy(
            minWidth = minSize.width.coerceIn(constraints.minWidth, constraints.maxWidth),
            minHeight = minSize.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        )
        val measured = measurable.measure(childConstraints)
        layout(measured.width, measured.height) {
            measured.placeRelative(0, 0)
        }
    }
}

private class TextFieldSize(
    var layoutDirection: LayoutDirection,
    var density: Density,
    var resourceLoader: Font.ResourceLoader,
    var style: TextStyle
) {
    var minSize = computeMinSize()
        private set

    fun update(
        layoutDirection: LayoutDirection,
        density: Density,
        resourceLoader: Font.ResourceLoader,
        style: TextStyle
    ) {
        if (layoutDirection != this.layoutDirection ||
            density != this.density ||
            resourceLoader != this.resourceLoader ||
            style != this.style
        ) {
            this.layoutDirection = layoutDirection
            this.density = density
            this.resourceLoader = resourceLoader
            this.style = style
            minSize = computeMinSize()
        }
    }

    private fun computeMinSize(): IntSize {
        return computeSizeForDefaultText(
            style = resolveDefaults(style, layoutDirection),
            density = density,
            resourceLoader = resourceLoader
        )
    }
}