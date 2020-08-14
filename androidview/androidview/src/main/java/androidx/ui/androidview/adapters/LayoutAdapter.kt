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

package androidx.ui.androidview.adapters

import android.R
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.reflect.KMutableProperty

private var registered = false
private val View.layoutBuilder: LayoutBuilder
    get() {
        if (!registered) {
            registerHandlers()
        }
        return getOrAddLayoutBuilderAdapter()
    }

private fun setOrError(params: Any, value: Any?, name: String) {
    val klass = params::class
    try {
        val prop = klass.members.find { it.name == name }
        when (prop) {
            is KMutableProperty<*> -> {
                prop.setter.call(params, value)
            }
            else -> error("$name not possible to be set on ${klass.java.name}")
        }
    } catch (e: Exception) {
        error("$name not possible to be set on ${klass.java.name}")
    }
}

private fun registerHandlers() {
    registerIntLayoutHandler(R.attr.layout_width) {
        width = it
    }
    registerIntLayoutHandler(R.attr.layout_height) {
        height = it
    }
    registerFloatLayoutHandler(R.attr.layout_weight) {
        when (this) {
            is LinearLayout.LayoutParams -> weight = it
            else -> setOrError(this, it, "weight")
        }
    }
    registerIntLayoutHandler(R.attr.layout_gravity) {
        when (this) {
            is LinearLayout.LayoutParams -> gravity = it
            is FrameLayout.LayoutParams -> gravity = it
            else -> setOrError(this, it, "gravity")
        }
    }
    registerIntLayoutHandler(R.attr.layout_margin) {
        when (this) {
            is ViewGroup.MarginLayoutParams -> setMargins(it, it, it, it)
            else -> error("margin not possible to be set on ${this::class.java.simpleName}")
        }
    }
    registerIntLayoutHandler(R.attr.layout_marginTop) {
        when (this) {
            is ViewGroup.MarginLayoutParams -> topMargin = it
            else -> error("marginTop not possible to be set on ${this::class.java.simpleName}")
        }
    }
    registerIntLayoutHandler(R.attr.layout_marginLeft) {
        when (this) {
            is ViewGroup.MarginLayoutParams -> leftMargin = it
            else -> error("marginLeft not possible to be set on ${this::class.java.simpleName}")
        }
    }
    registerIntLayoutHandler(R.attr.layout_marginBottom) {
        when (this) {
            is ViewGroup.MarginLayoutParams -> bottomMargin = it
            else -> error("marginBottom not possible to be set on ${this::class.java.simpleName}")
        }
    }
    registerIntLayoutHandler(R.attr.layout_marginRight) {
        when (this) {
            is ViewGroup.MarginLayoutParams -> rightMargin = it
            else -> error("marginRight not possible to be set on ${this::class.java.simpleName}")
        }
    }
    registered = true
}

private fun View.setPixelLayoutWidth(width: Int) =
    layoutBuilder.set(android.R.attr.layout_width, width)
private fun View.setPixelLayoutHeight(height: Int) =
    layoutBuilder.set(android.R.attr.layout_height, height)
private fun View.setPixelMarginTop(pixels: Int) =
    layoutBuilder.set(android.R.attr.layout_marginTop, pixels)
private fun View.setPixelMarginLeft(pixels: Int) =
    layoutBuilder.set(android.R.attr.layout_marginLeft, pixels)
private fun View.setPixelMarginBottom(pixels: Int) =
    layoutBuilder.set(android.R.attr.layout_marginBottom, pixels)
private fun View.setPixelMarginRight(pixels: Int) =
    layoutBuilder.set(android.R.attr.layout_marginRight, pixels)
private fun View.setPixelMarginHorizontal(pixels: Int) {
    setPixelMarginLeft(pixels)
    setPixelMarginRight(pixels)
}
private fun View.setPixelMarginVertical(pixels: Int) {
    setPixelMarginTop(pixels)
    setPixelMarginBottom(pixels)
}

fun View.setLayoutWidth(dim: Dimension) = setPixelLayoutWidth(dim.toIntPixels(metrics))
fun View.setLayoutWidth(width: Int) {
    if (width == -1 || width == -2) {
        // It is either MATCH_PARENT, FILL_PARENT or WRAP_CONTENT
        setPixelLayoutWidth(width)
    } else {
        // It is a dimension resource ID.
        setPixelLayoutWidth(resources.getDimensionPixelSize(width.assertDimensionRes()))
    }
}

fun View.setLayoutHeight(dim: Dimension) = setPixelLayoutHeight(dim.toIntPixels(metrics))
fun View.setLayoutHeight(height: Int) {
    if (height == -1 || height == -2) {
        setPixelLayoutHeight(height)
    } else {
        setPixelLayoutHeight(resources.getDimensionPixelSize(height.assertDimensionRes()))
    }
}

fun View.setLayoutGravity(gravity: Int) = layoutBuilder.set(android.R.attr.layout_gravity, gravity)
fun View.setLayoutWeight(weight: Float) = layoutBuilder.set(android.R.attr.layout_weight, weight)

fun View.setMarginTop(resId: Int) =
    setPixelMarginTop(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginLeft(resId: Int) =
    setPixelMarginLeft(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginBottom(resId: Int) =
    setPixelMarginBottom(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginRight(resId: Int) =
    setPixelMarginRight(resources.getDimensionPixelSize(resId.assertDimensionRes()))

fun View.setMarginTop(dim: Dimension) = setPixelMarginTop(dim.toIntPixels(metrics))
fun View.setMarginLeft(dim: Dimension) = setPixelMarginLeft(dim.toIntPixels(metrics))
fun View.setMarginBottom(dim: Dimension) = setPixelMarginBottom(dim.toIntPixels(metrics))
fun View.setMarginRight(dim: Dimension) = setPixelMarginRight(dim.toIntPixels(metrics))

fun View.setMarginHorizontal(resId: Int) =
    setPixelMarginHorizontal(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginVertical(resId: Int) =
    setPixelMarginVertical(resources.getDimensionPixelSize(resId.assertDimensionRes()))
fun View.setMarginHorizontal(dim: Dimension) = setPixelMarginHorizontal(dim.toIntPixels(metrics))
fun View.setMarginVertical(dim: Dimension) = setPixelMarginVertical(dim.toIntPixels(metrics))
