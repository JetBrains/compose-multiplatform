/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.foundation.newtext.text.DefaultMinLines
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow

@ExperimentalComposeUiApi
internal data class SelectableTextAnnotatedStringElement(
    private val text: AnnotatedString,
    private val style: TextStyle,
    private val fontFamilyResolver: FontFamily.Resolver,
    private val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    private val overflow: TextOverflow = TextOverflow.Clip,
    private val softWrap: Boolean = true,
    private val maxLines: Int = Int.MAX_VALUE,
    private val minLines: Int = DefaultMinLines,
    private val placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
    private val onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
    private val selectionController: SelectionController? = null
) : ModifierNodeElement<SelectableTextAnnotatedStringNode>() {

    override fun create(): SelectableTextAnnotatedStringNode = SelectableTextAnnotatedStringNode(
        text,
        style,
        fontFamilyResolver,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        placeholders,
        onPlaceholderLayout,
        selectionController
    )

    override fun update(
        node: SelectableTextAnnotatedStringNode
    ): SelectableTextAnnotatedStringNode {
        node.update(
            text = text,
            style = style,
            placeholders = placeholders,
            minLines = minLines,
            maxLines = maxLines,
            softWrap = softWrap,
            fontFamilyResolver = fontFamilyResolver,
            overflow = overflow,
            onTextLayout = onTextLayout,
            onPlaceholderLayout = onPlaceholderLayout,
            selectionController = selectionController
        )
        return node
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is SelectableTextAnnotatedStringElement) return false

        // these three are most likely to actually change
        if (text != other.text) return false
        if (style != other.style) return false
        if (placeholders != other.placeholders) return false

        // these are equally unlikely to change
        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (onTextLayout != other.onTextLayout) return false
        if (overflow != other.overflow) return false
        if (softWrap != other.softWrap) return false
        if (maxLines != other.maxLines) return false
        if (minLines != other.minLines) return false

        // these never change, but check anyway for correctness
        if (onPlaceholderLayout != other.onPlaceholderLayout) return false
        if (selectionController != other.selectionController) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + (onTextLayout?.hashCode() ?: 0)
        result = 31 * result + overflow.hashCode()
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        result = 31 * result + (placeholders?.hashCode() ?: 0)
        result = 31 * result + (onPlaceholderLayout?.hashCode() ?: 0)
        result = 31 * result + (selectionController?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}