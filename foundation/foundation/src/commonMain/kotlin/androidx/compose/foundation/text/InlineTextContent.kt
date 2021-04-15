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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder

/** The annotation tag used by inline content. */
internal const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"
// A string that contains a replacement character specified by unicode. It's used as the default
// value of alternate text.
private const val REPLACEMENT_CHAR = "\uFFFD"
/**
 * Used to insert composables into the text layout. This method can be used together with the
 * inlineContent parameter of [BasicText]. It will append the [alternateText] to this
 * [AnnotatedString] and also mark this range of text to be replaced by a composable.
 * [BasicText] will try to find an [InlineTextContent] in the map defined by inlineContent whose
 * key equals to [id], and it will use the [InlineTextContent.children] to replace this range of
 * text.
 *
 * @sample androidx.compose.foundation.samples.InlineTextContentSample
 * @see InlineTextContent
 * @see BasicText
 *
 * @param id The id used to look up the [InlineTextContent], it is referred by the inlineContent
 * parameter of [BasicText] to replace the [alternateText] to the corresponding composable.
 * @param alternateText The text to be replaced by the inline content. It's displayed when
 * the inlineContent parameter of [BasicText] doesn't contain [id].
 * Accessibility features will also use this text to describe the inline content.
 * @throws IllegalArgumentException if [alternateText] has zero length.
 */
fun AnnotatedString.Builder.appendInlineContent(
    id: String,
    alternateText: String = REPLACEMENT_CHAR
) {
    require(alternateText.isNotEmpty()) {
        "alternateText can't be an empty string."
    }
    pushStringAnnotation(INLINE_CONTENT_TAG, id)
    append(alternateText)
    pop()
}

/**
 * A data class that stores a composable to be inserted into the text layout.
 *
 * Different from a regular composable, a [Placeholder] is also needed for text layout to reserve
 * space. In this [placeholder], the size of the content and how it will be aligned within the
 * text line is defined. When the children composable is measured, its size given in
 * [Placeholder.width] and [Placeholder.height] will be converted into
 * [androidx.compose.ui.unit.Constraints] and passed through [androidx.compose.ui.layout.Layout].
 *
 * @sample androidx.compose.foundation.samples.InlineTextContentSample
 * @see BasicText
 * @see Placeholder
 */
@Immutable
class InlineTextContent(
    /**
     * The setting object that defines the size and vertical alignment of this composable in the
     * text line. This is different from the measure of Layout
     * @see Placeholder
     */
    val placeholder: Placeholder,
    /**
     * The composable to be inserted into the text layout.
     * The string parameter passed to it will the alternateText given to [appendInlineContent].
     */
    val children: @Composable (String) -> Unit
)