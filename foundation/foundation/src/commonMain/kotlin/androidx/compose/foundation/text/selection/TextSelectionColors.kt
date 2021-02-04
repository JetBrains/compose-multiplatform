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

package androidx.compose.foundation.text.selection

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Represents the colors used for text selection by text and text field components.
 *
 * See [LocalTextSelectionColors] to provide new values for this throughout the hierarchy.
 *
 * @property handleColor the color used for the selection handles on either side of the
 * selection region.
 * @property backgroundColor the color used to draw the background behind the selected
 * region. This color should have alpha applied to keep the text legible - this alpha is
 * typically 0.4f (40%) but this may need to be reduced in order to meet contrast requirements
 * depending on the color used for text, selection background, and the background behind the
 * selection background.
 */
@Immutable
class TextSelectionColors(
    val handleColor: Color,
    val backgroundColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextSelectionColors) return false

        if (handleColor != other.handleColor) return false
        if (backgroundColor != other.backgroundColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = handleColor.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        return result
    }

    override fun toString(): String {
        return "SelectionColors(selectionHandleColor=$handleColor, " +
            "selectionBackgroundColor=$backgroundColor)"
    }
}

/**
 * CompositionLocal used to change the [TextSelectionColors] used by text and text field
 * components in the hierarchy.
 */
val LocalTextSelectionColors = compositionLocalOf { DefaultTextSelectionColors }

/**
 * Default color used is the blue from the Compose logo, b/172679845 for context
 */
private val DefaultSelectionColor = Color(0xFF4286F4)

@Stable
private val DefaultTextSelectionColors = TextSelectionColors(
    handleColor = DefaultSelectionColor,
    backgroundColor = DefaultSelectionColor.copy(alpha = 0.4f)
)
