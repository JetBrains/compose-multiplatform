/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text

/**
 * Layout constraints for [Paragraph] objects.
 *
 * Instances of this class are typically used with [Paragraph].
 *
 * The only constraint that can be specified is the [width]. See the discussion
 * at [width] for more details.
 *
 * Creates constraints for laying out a paragraph.
 *
 * The [width] argument must not be null.
 *
 * The width the paragraph should use whey computing the positions of glyphs.
 *
 * If possible, the paragraph will select a soft line break prior to reaching
 * this width. If no soft line break is available, the paragraph will select
 * a hard line break prior to reaching this width. If that would force a line
 * break without any characters having been placed (i.e. if the next
 * character to be laid out does not fit within the given width constraint)
 * then the next character is allowed to overflow the width constraint and a
 * forced line break is placed after it (even if an explicit line break
 * follows).
 *
 * The width influences how ellipses are applied.
 *
 * This width is also used to position glyphs according to the text alignment
 * described in the [ParagraphStyle.textAlign] to create [Paragraph].
 */
data class ParagraphConstraints(val width: Float) {
    override fun toString(): String {
        return "ParagraphConstraints(width: $width)"
    }
}
