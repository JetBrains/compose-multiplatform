/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.w3c.dom.css.CSS

fun interface FilterFunction {
    fun apply(): String
}

interface FilterBuilder {
    fun blur(radius: CSSLengthValue)

    fun brightness(amount: Number)
    fun brightness(amount: CSSPercentageValue)

    fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue)
    fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue, blurRadius: CSSLengthValue)
    fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue, color: CSSColorValue)
    fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue, blurRadius: CSSLengthValue, color: CSSColorValue)

    fun contrast(amount: Number)
    fun contrast(amount: CSSPercentageValue)

    fun grayscale(amount: Number)
    fun grayscale(amount: CSSPercentageValue)

    fun hueRotate(angle: CSSAngleValue)

    fun invert(amount: Number)
    fun invert(amount: CSSPercentageValue)

    fun opacity(amount: Number)
    fun opacity(amount: CSSPercentageValue)

    fun saturate(amount: Number)
    fun saturate(amount: CSSPercentageValue)

    fun sepia(amount: Number)
    fun sepia(amount: CSSPercentageValue)
}

private class FilterBuilderImplementation : FilterBuilder {
    private val transformations = mutableListOf<FilterFunction>()

    override fun blur(radius: CSSLengthValue) { transformations.add { "blur($radius)" } }

    override fun brightness(amount: Number) { transformations.add { "brightness($amount)" } }
    override fun brightness(amount: CSSPercentageValue) { transformations.add { "brightness($amount)" } }

    override fun contrast(amount: Number)  { transformations.add { "contrast($amount)" } }
    override fun contrast(amount: CSSPercentageValue) { transformations.add { "contrast($amount)" } }

    override fun grayscale(amount: Number) { transformations.add { "grayscale($amount)" } }
    override fun grayscale(amount: CSSPercentageValue) { transformations.add { "grayscale($amount)" } }

    override fun hueRotate(angle: CSSAngleValue) { transformations.add { "hue-rotate($angle)" } }

    override fun toString(): String {
        return transformations.joinToString(" ") { it.apply() }
    }

    override fun invert(amount: Number) { transformations.add { "invert($amount)" } }
    override fun invert(amount: CSSPercentageValue) { transformations.add { "invert($amount)" } }

    override fun opacity(amount: Number) { transformations.add { "opacity($amount)" } }
    override fun opacity(amount: CSSPercentageValue) { transformations.add { "opacity($amount)" } }

    override fun saturate(amount: Number) { transformations.add { "saturate($amount)" } }
    override fun saturate(amount: CSSPercentageValue) { transformations.add { "saturate($amount)" } }

    override fun sepia(amount: Number) { transformations.add { "sepia($amount)" } }
    override fun sepia(amount: CSSPercentageValue) { transformations.add { "sepia($amount)" } }

    override fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue) {
        transformations.add { "drop-shadow($offsetX $offsetY)" }
    }

    override fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue, blurRadius: CSSLengthValue) {
        transformations.add { "drop-shadow($offsetX $offsetY $blurRadius)" }
    }

    override fun dropShadow(offsetX: CSSLengthValue, offsetY: CSSLengthValue, color: CSSColorValue) {
        transformations.add { "drop-shadow($offsetX $offsetY $color)" }
    }

    override fun dropShadow(
        offsetX: CSSLengthValue,
        offsetY: CSSLengthValue,
        blurRadius: CSSLengthValue,
        color: CSSColorValue
    ) {
        transformations.add { "drop-shadow($offsetX $offsetY $blurRadius $color)" }
    }
}

@ExperimentalComposeWebApi
fun StyleScope.filter(filterContext: FilterBuilder.() -> Unit) {
    val builder = FilterBuilderImplementation()
    property("filter", builder.apply(filterContext).toString())
}


