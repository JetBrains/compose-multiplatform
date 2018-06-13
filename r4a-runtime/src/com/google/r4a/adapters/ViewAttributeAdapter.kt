@file:Suppress("UNUSED_PARAMETER", "unused")

package com.google.r4a.adapters

import android.graphics.Color
import android.view.View
import com.google.r4a.adapters.Utils.stringToIntPx
import com.google.r4a.annotations.Aesthetic
import com.google.r4a.annotations.ColorString
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.DimensionString


// NOTE: these attributes are added to every view/component so that we can have a "key" attribute
// that the users define to preserve state across composes. In the long run, we may decide to
// use namespaces for these attributes, but for now, the grammar does not allow that and this
// is a simple stop-gap solution.
fun View.setKey(key: Any) {}

@ConflictsWith("paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "paddingHorizontal", "paddingHorizontal")
fun View.setPadding(padding: Int) = setPadding(padding, padding, padding, padding)

@ConflictsWith("paddingLeft", "paddingRight")
fun View.setPaddingHorizontal(padding: Int) = setPadding(padding, paddingTop, padding, paddingBottom)

@ConflictsWith("paddingTop", "paddingBottom")
fun View.setPaddingVertical(padding: Int) = setPadding(paddingLeft, padding, paddingRight, padding)

fun View.setPaddingLeft(padding: Int) = setPadding(padding, paddingTop, paddingRight, paddingBottom)

fun View.setPaddingTop(padding: Int) = setPadding(paddingLeft, padding, paddingRight, paddingBottom)

fun View.setPaddingRight(padding: Int) = setPadding(paddingLeft, paddingTop, padding, paddingBottom)

fun View.setPaddingBottom(padding: Int) = setPadding(paddingLeft, paddingTop, paddingRight, padding)

// Dimension Setters

@ConflictsWith("paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "paddingHorizontal", "paddingHorizontal")
fun View.setPadding(padding: Dimension) = setPadding(padding.toIntPixels(metrics))

@ConflictsWith("paddingLeft", "paddingRight")
fun View.setPaddingHorizontal(padding: Dimension) = setPaddingHorizontal(padding.toIntPixels(metrics))

@ConflictsWith("paddingTop", "paddingBottom")
fun View.setPaddingVertical(padding: Dimension) = setPaddingVertical(padding.toIntPixels(metrics))

fun View.setPaddingLeft(padding: Dimension) = setPaddingLeft(padding.toIntPixels(metrics))

fun View.setPaddingTop(padding: Dimension) = setPaddingTop(padding.toIntPixels(metrics))

fun View.setPaddingRight(padding: Dimension) = setPaddingRight(padding.toIntPixels(metrics))

fun View.setPaddingBottom(padding: Dimension) = setPaddingBottom(padding.toIntPixels(metrics))

// String-based compatibility setters

@Aesthetic
@DimensionString
@ConflictsWith("paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "paddingHorizontal", "paddingHorizontal")
fun View.setPadding(padding: String) = setPadding(stringToIntPx(padding, metrics))

@Aesthetic
@DimensionString
@ConflictsWith("paddingLeft", "paddingRight")
fun View.setPaddingHorizontal(padding: String) = setPaddingHorizontal(stringToIntPx(padding, metrics))

@Aesthetic
@DimensionString
@ConflictsWith("paddingTop", "paddingBottom")
fun View.setPaddingVertical(padding: String) = setPaddingVertical(stringToIntPx(padding, metrics))

@Aesthetic
@DimensionString
fun View.setPaddingLeft(padding: String) = setPaddingLeft(stringToIntPx(padding, metrics))

@Aesthetic
@DimensionString
fun View.setPaddingTop(padding: String) = setPaddingTop(stringToIntPx(padding, metrics))

@Aesthetic
@DimensionString
fun View.setPaddingRight(padding: String) = setPaddingRight(stringToIntPx(padding, metrics))

@Aesthetic
@DimensionString
fun View.setPaddingBottom(padding: String) = setPaddingBottom(stringToIntPx(padding, metrics))

@ColorString
fun View.setBackgroundColor(color: String) = setBackgroundColor(Color.parseColor(color))


fun View.setPivotX(pivotX: Dimension) = setPivotX(pivotX.toFloatPixels(metrics))
fun View.setPivotY(pivotY: Dimension) = setPivotY(pivotY.toFloatPixels(metrics))
fun View.setTranslationX(translationX: Dimension) = setTranslationX(translationX.toFloatPixels(metrics))
fun View.setTranslationY(translationY: Dimension) = setTranslationY(translationY.toFloatPixels(metrics))
fun View.setX(x: Dimension) = setX(x.toFloatPixels(metrics))
fun View.setY(y: Dimension) = setY(y.toFloatPixels(metrics))


fun View.setBottom(bottom: Dimension) = setBottom(bottom.toIntPixels(metrics))
fun View.setFadingEdgeLength(fadingEdgeLength: Dimension) = setFadingEdgeLength(fadingEdgeLength.toIntPixels(metrics))
fun View.setLeft(left: Dimension) = setLeft(left.toIntPixels(metrics))
fun View.setMinimumHeight(minimumHeight: Dimension) = setMinimumHeight(minimumHeight.toIntPixels(metrics))
fun View.setMinimumWidth(minimumWidth: Dimension) = setMinimumWidth(minimumWidth.toIntPixels(metrics))
fun View.setRight(right: Dimension) = setRight(right.toIntPixels(metrics))
fun View.setScrollX(scrollX: Dimension) = setScrollX(scrollX.toIntPixels(metrics))
fun View.setScrollY(scrollY: Dimension) = setScrollY(scrollY.toIntPixels(metrics))
fun View.setTop(top: Dimension) = setTop(top.toIntPixels(metrics))


// TODO(lmr): introduced in newer SDK
//fun View.setElevation(elevation: Dimension) = setElevation(elevation.toFloatPixels(metrics))
//fun View.setTranslationZ(translationZ: Dimension) = setTranslationZ(translationZ.toFloatPixels(metrics))
//fun View.setZ(z: Dimension) = setZ(z.toFloatPixels(metrics))