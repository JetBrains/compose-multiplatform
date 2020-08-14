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

@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.ui.androidview.adapters

import android.view.View
import androidx.annotation.DimenRes
import androidx.ui.androidview.annotations.ConflictsWith

// NOTE: these attributes are added to every view/component so that we can have a "key" attribute
// that the users define to preserve state across composes. In the long run, we may decide to
// use namespaces for these attributes, but for now, the grammar does not allow that and this
// is a simple stop-gap solution.
fun View.setKey(key: Any) {}

internal fun @receiver:DimenRes Int.assertDimensionRes(): Int {
    // TODO(jdemeulenaere): This only checks that `this` is a resource ID. Check if there is a
    // reliable way to assert it is a dimension resource.
    check((this ushr 28) == 0x7f)
    return this
}

private fun View.setPixelPadding(padding: Int) = setPadding(padding, padding, padding, padding)

private fun View.setPixelPaddingHorizontal(padding: Int) =
    setPadding(padding, paddingTop, padding, paddingBottom)

private fun View.setPixelPaddingVertical(padding: Int) =
    setPadding(paddingLeft, padding, paddingRight, padding)

private fun View.setPixelPaddingLeft(padding: Int) =
    setPadding(padding, paddingTop, paddingRight, paddingBottom)

private fun View.setPixelPaddingTop(padding: Int) =
    setPadding(paddingLeft, padding, paddingRight, paddingBottom)

private fun View.setPixelPaddingRight(padding: Int) =
    setPadding(paddingLeft, paddingTop, padding, paddingBottom)

private fun View.setPixelPaddingBottom(padding: Int) =
    setPadding(paddingLeft, paddingTop, paddingRight, padding)

// Int resource setters

@ConflictsWith(
    "paddingLeft",
    "paddingRight",
    "paddingTop",
    "paddingBottom",
    "paddingHorizontal",
    "paddingHorizontal"
)
fun View.setPadding(@DimenRes paddingResId: Int) {
    setPixelPadding(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

@ConflictsWith("paddingLeft", "paddingRight")
fun View.setPaddingHorizontal(@DimenRes paddingResId: Int) {
    setPixelPaddingHorizontal(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

@ConflictsWith("paddingTop", "paddingBottom")
fun View.setPaddingVertical(@DimenRes paddingResId: Int) {
    setPixelPaddingVertical(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

fun View.setPaddingLeft(@DimenRes paddingResId: Int) {
    setPixelPaddingLeft(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

fun View.setPaddingTop(@DimenRes paddingResId: Int) {
    setPixelPaddingTop(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

fun View.setPaddingRight(@DimenRes paddingResId: Int) {
    setPixelPaddingRight(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

fun View.setPaddingBottom(@DimenRes paddingResId: Int) {
    setPixelPaddingBottom(resources.getDimensionPixelSize(paddingResId.assertDimensionRes()))
}

// Dimension Setters

@ConflictsWith(
    "paddingLeft",
    "paddingRight",
    "paddingTop",
    "paddingBottom",
    "paddingHorizontal",
    "paddingHorizontal"
)
fun View.setPadding(padding: Dimension) = setPixelPadding(padding.toIntPixels(metrics))

@ConflictsWith("paddingLeft", "paddingRight")
fun View.setPaddingHorizontal(padding: Dimension) =
    setPixelPaddingHorizontal(padding.toIntPixels(metrics))

@ConflictsWith("paddingTop", "paddingBottom")
fun View.setPaddingVertical(padding: Dimension) =
    setPixelPaddingVertical(padding.toIntPixels(metrics))

fun View.setPaddingLeft(padding: Dimension) = setPixelPaddingLeft(padding.toIntPixels(metrics))

fun View.setPaddingTop(padding: Dimension) = setPixelPaddingTop(padding.toIntPixels(metrics))

fun View.setPaddingRight(padding: Dimension) = setPixelPaddingRight(padding.toIntPixels(metrics))

fun View.setPaddingBottom(padding: Dimension) = setPixelPaddingBottom(padding.toIntPixels(metrics))

fun View.setPivotX(pivotX: Dimension) = setPivotX(pivotX.toFloatPixels(metrics))
fun View.setPivotY(pivotY: Dimension) = setPivotY(pivotY.toFloatPixels(metrics))
fun View.setTranslationX(translationX: Dimension) =
    setTranslationX(translationX.toFloatPixels(metrics))
fun View.setTranslationY(translationY: Dimension) =
    setTranslationY(translationY.toFloatPixels(metrics))
fun View.setX(x: Dimension) = setX(x.toFloatPixels(metrics))
fun View.setY(y: Dimension) = setY(y.toFloatPixels(metrics))

fun View.setBottom(bottom: Dimension) = setBottom(bottom.toIntPixels(metrics))
fun View.setFadingEdgeLength(fadingEdgeLength: Dimension) =
    setFadingEdgeLength(fadingEdgeLength.toIntPixels(metrics))
fun View.setLeft(left: Dimension) = setLeft(left.toIntPixels(metrics))
fun View.setMinimumHeight(minimumHeight: Dimension) =
    setMinimumHeight(minimumHeight.toIntPixels(metrics))
fun View.setMinimumWidth(minimumWidth: Dimension) =
    setMinimumWidth(minimumWidth.toIntPixels(metrics))
fun View.setRight(right: Dimension) = setRight(right.toIntPixels(metrics))
fun View.setScrollX(scrollX: Dimension) = setScrollX(scrollX.toIntPixels(metrics))
fun View.setScrollY(scrollY: Dimension) = setScrollY(scrollY.toIntPixels(metrics))
fun View.setTop(top: Dimension) = setTop(top.toIntPixels(metrics))

// TODO: Necessary because the IR doesn't support SAM conversion yet
fun View.setOnClick(lambda: () -> Unit) { this.setOnClickListener { lambda() } }

// TODO(lmr): introduced in newer SDK
// fun View.setElevation(elevation: Dimension) = setElevation(elevation.toFloatPixels(metrics))
// fun View.setTranslationZ(translationZ: Dimension) =
//     setTranslationZ(translationZ.toFloatPixels(metrics))
// fun View.setZ(z: Dimension) = setZ(z.toFloatPixels(metrics))