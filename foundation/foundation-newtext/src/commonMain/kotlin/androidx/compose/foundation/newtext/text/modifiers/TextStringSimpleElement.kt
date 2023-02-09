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
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow

@ExperimentalComposeUiApi
internal class TextStringSimpleElement(
    private val text: String,
    private val style: TextStyle,
    private val fontFamilyResolver: FontFamily.Resolver,
    private val overflow: TextOverflow = TextOverflow.Clip,
    private val softWrap: Boolean = true,
    private val maxLines: Int = Int.MAX_VALUE,
    private val minLines: Int = DefaultMinLines,
) : ModifierNodeElement<TextStringSimpleNode>(inspectorInfo = debugInspectorInfo { }) {
    override fun create(): TextStringSimpleNode = TextStringSimpleNode(
        text,
        style,
        fontFamilyResolver,
        overflow,
        softWrap,
        maxLines,
        minLines
    )

    override fun update(node: TextStringSimpleNode): TextStringSimpleNode {
        node.doInvalidations(
            textChanged = node.updateText(
                text = text
            ),
            layoutChanged = node.updateLayoutRelatedArgs(
                style = style,
                minLines = minLines,
                maxLines = maxLines,
                softWrap = softWrap,
                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow
            )
        )
        return node
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is TextStringSimpleElement) return false

        // these three are most likely to actually change
        if (text != other.text) return false
        if (style != other.style) return false

        // these are equally unlikely to change
        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (overflow != other.overflow) return false
        if (softWrap != other.softWrap) return false
        if (maxLines != other.maxLines) return false
        if (minLines != other.minLines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + overflow.hashCode()
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        return result
    }
}