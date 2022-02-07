/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi

fun interface TransformFunction {
    fun apply(): String
}

interface TransformBuilder {
    fun matrix(a: Number, b: Number, c: Number, d: Number, tx: Number, ty: Number): Boolean
    fun matrix3d(
        a1: Number, b1: Number, c1: Number, d1: Number,
        a2: Number, b2: Number, c2: Number, d2: Number,
        a3: Number, b3: Number, c3: Number, d3: Number,
        a4: Number, b4: Number, c4: Number, d4: Number
    )

    fun perspective(d: CSSLengthValue)
    fun rotate(a: CSSAngleValue)
    fun rotate3d(x: Number, y: Number, z: Number, a: CSSAngleValue)
    fun rotateX(a: CSSAngleValue)
    fun rotateY(a: CSSAngleValue)
    fun rotateZ(a: CSSAngleValue)
    fun scale(sx: Number)
    fun scale(sx: Number, sy: Number)
    fun scale3d(sx: Number, sy: Number, sz: Number)
    fun scaleX(s: Number)
    fun scaleY(s: Number)
    fun scaleZ(s: Number)
    fun skew(ax: CSSAngleValue)
    fun skew(ax: CSSAngleValue, ay: CSSAngleValue)
    fun skewX(a: CSSAngleValue)
    fun skewY(a: CSSAngleValue)
    fun translate(tx: CSSLengthValue)
    fun translate(tx: CSSPercentageValue)
    fun translate(tx: CSSLengthValue, ty: CSSLengthValue)
    fun translate(tx: CSSLengthValue, ty: CSSPercentageValue)
    fun translate(tx: CSSPercentageValue, ty: CSSLengthValue)
    fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue)
    fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue)
    fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue)
    fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue)
    fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue)
    fun translateX(tx: CSSLengthValue)
    fun translateX(tx: CSSPercentageValue)
    fun translateY(ty: CSSLengthValue)
    fun translateY(ty: CSSPercentageValue)
    fun translateZ(tz: CSSLengthValue)
}

private class TransformBuilderImplementation : TransformBuilder {
    private val transformations = mutableListOf<TransformFunction>()

    override fun matrix(a: Number, b: Number, c: Number, d: Number, tx: Number, ty: Number) =
        transformations.add { "matrix($a, $b, $c, $d, $tx, $ty)" }

    override fun matrix3d(
        a1: Number, b1: Number, c1: Number, d1: Number,
        a2: Number, b2: Number, c2: Number, d2: Number,
        a3: Number, b3: Number, c3: Number, d3: Number,
        a4: Number, b4: Number, c4: Number, d4: Number
    ) {
        transformations.add { "matrix3d($a1, $b1, $c1, $d1, $a2, $b2, $c2, $d2, $a3, $b3, $c3, $d3, $a4, $b4, $c4, $d4)" }
    }

    override fun perspective(d: CSSLengthValue) {
        transformations.add { "perspective($d)" }
    }

    override fun rotate(a: CSSAngleValue) {
        transformations.add { "rotate($a)" }
    }

    override fun rotate3d(x: Number, y: Number, z: Number, a: CSSAngleValue) {
        transformations.add({ "rotate3d($x, $y, $z, $a)" })
    }

    override fun rotateX(a: CSSAngleValue) {
        transformations.add { "rotateX($a)" }
    }

    override fun rotateY(a: CSSAngleValue) {
        transformations.add { "rotateY($a)" }
    }

    override fun rotateZ(a: CSSAngleValue) {
        transformations.add { "rotateZ($a)" }
    }

    override fun scale(sx: Number) {
        transformations.add { "scale($sx)" }
    }

    override fun scale(sx: Number, sy: Number) {
        transformations.add { "scale($sx, $sy)" }
    }

    override fun scale3d(sx: Number, sy: Number, sz: Number) {
        transformations.add { "scale3d($sx, $sy, $sz)" }
    }

    override fun scaleX(s: Number) {
        transformations.add { "scaleX($s)" }
    }

    override fun scaleY(s: Number) {
        transformations.add { "scaleY($s)" }
    }

    override fun scaleZ(s: Number) {
        transformations.add { "scaleZ($s)" }
    }

    override fun skew(ax: CSSAngleValue) {
        transformations.add { "skew($ax)" }
    }

    override fun skew(ax: CSSAngleValue, ay: CSSAngleValue) {
        transformations.add { "skew($ax, $ay)" }
    }

    override fun skewX(a: CSSAngleValue) {
        transformations.add { "skewX($a)" }
    }

    override fun skewY(a: CSSAngleValue) {
        transformations.add { "skewY($a)" }
    }

    override fun translate(tx: CSSLengthValue) {
        transformations.add { "translate($tx)" }
    }

    override fun translate(tx: CSSPercentageValue) {
        transformations.add { "translate($tx)" }
    }

    override fun translate(tx: CSSLengthValue, ty: CSSLengthValue) {
        transformations.add { "translate($tx, $ty)" }
    }

    override fun translate(tx: CSSLengthValue, ty: CSSPercentageValue) {
        transformations.add { "translate($tx, $ty)" }
    }

    override fun translate(tx: CSSPercentageValue, ty: CSSLengthValue) {
        transformations.add { "translate($tx, $ty)" }
    }

    override fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue) {
        transformations.add { "translate($tx, $ty)" }
    }

    override fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue) {
        transformations.add { "translate3d($tx, $ty, $tz)" }
    }

    override fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue) {
        transformations.add { "translate3d($tx, $ty, $tz)" }
    }

    override fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue) {
        transformations.add { "translate3d($tx, $ty, $tz)" }
    }

    override fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue) {
        transformations.add { "translate3d($tx, $ty, $tz)" }
    }

    override fun translateX(tx: CSSLengthValue) {
        transformations.add { "translateX($tx)" }
    }

    override fun translateX(tx: CSSPercentageValue) {
        transformations.add { "translateX($tx)" }
    }

    override fun translateY(ty: CSSLengthValue) {
        transformations.add { "translateY($ty)" }
    }

    override fun translateY(ty: CSSPercentageValue) {
        transformations.add { "translateY($ty)" }
    }

    override fun translateZ(tz: CSSLengthValue) {
        transformations.add { "translateZ($tz)" }
    }

    override fun toString(): String {
        return transformations.joinToString(" ") { it.apply() }
    }
}

@ExperimentalComposeWebApi
fun StyleScope.transform(transformContext: TransformBuilder.() -> Unit) {
    val transformBuilder = TransformBuilderImplementation()
    property("transform", transformBuilder.apply(transformContext).toString())
}