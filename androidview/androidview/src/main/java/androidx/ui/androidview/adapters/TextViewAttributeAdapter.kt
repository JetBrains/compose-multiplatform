/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("unused", "UNUSED_PARAMETER", "UsePropertyAccessSyntax")

package androidx.ui.androidview.adapters

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView

fun TextView.setTextSize(size: Dimension) =
    setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloatPixels(metrics))

fun TextView.setFontFamily(fontFamily: String) {
    typeface = Typeface.create(fontFamily, typeface.style)
}

fun TextView.setFontStyle(style: Int) {
    typeface = Typeface.create(typeface, style)
}

fun TextView.setBufferType(bufferType: TextView.BufferType) {
    // TODO(lmr): this goes with setText. Not quite sure how to represent this. Wonder if
    // we should expose a bufferType property on TextView
}

fun TextView.setCompoundDrawablePadding(compoundDrawablePadding: Dimension) =
    setCompoundDrawablePadding(compoundDrawablePadding.toIntPixels(metrics))

fun TextView.setHeight(height: Dimension) = setHeight(height.toIntPixels(metrics))
fun TextView.setWidth(width: Dimension) = setWidth(width.toIntPixels(metrics))
fun TextView.setMaxHeight(maxHeight: Dimension) = setMaxHeight(maxHeight.toIntPixels(metrics))
fun TextView.setMaxWidth(maxWidth: Dimension) = setMaxWidth(maxWidth.toIntPixels(metrics))
fun TextView.setMinHeight(minHeight: Dimension) = setMinHeight(minHeight.toIntPixels(metrics))
fun TextView.setMinWidth(minWidth: Dimension) = setMinWidth(minWidth.toIntPixels(metrics))