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
    ): Boolean

    fun perspective(d: CSSLengthValue): Boolean
    fun rotate(a: CSSAngleValue): Boolean
    fun rotate3d(x: Number, y: Number, z: Number, a: CSSAngleValue): Boolean
    fun rotateX(a: CSSAngleValue): Boolean
    fun rotateY(a: CSSAngleValue): Boolean
    fun rotateZ(a: CSSAngleValue): Boolean
    fun scale(sx: Number): Boolean
    fun scale(sx: Number, sy: Number): Boolean
    fun scale3d(sx: Number, sy: Number, sz: Number): Boolean
    fun scaleX(s: Number): Boolean
    fun scaleY(s: Number): Boolean
    fun scaleZ(s: Number): Boolean
    fun skew(ax: CSSAngleValue): Boolean
    fun skew(ax: CSSAngleValue, ay: CSSAngleValue): Boolean
    fun skewX(a: CSSAngleValue): Boolean
    fun skewY(a: CSSAngleValue): Boolean
    fun translate(tx: CSSLengthValue): Boolean
    fun translate(tx: CSSPercentageValue): Boolean
    fun translate(tx: CSSLengthValue, ty: CSSLengthValue): Boolean
    fun translate(tx: CSSLengthValue, ty: CSSPercentageValue): Boolean
    fun translate(tx: CSSPercentageValue, ty: CSSLengthValue): Boolean
    fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue): Boolean
    fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue): Boolean
    fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue): Boolean
    fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue): Boolean
    fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue): Boolean
    fun translateX(tx: CSSLengthValue): Boolean
    fun translateX(tx: CSSPercentageValue): Boolean
    fun translateY(ty: CSSLengthValue): Boolean
    fun translateY(ty: CSSPercentageValue): Boolean
    fun translateZ(tz: CSSLengthValue): Boolean
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
    ) =
        transformations.add { "matrix3d($a1, $b1, $c1, $d1, $a2, $b2, $c2, $d2, $a3, $b3, $c3, $d3, $a4, $b4, $c4, $d4)" }

    override fun perspective(d: CSSLengthValue) = transformations.add { "perspective($d)" }

    override fun rotate(a: CSSAngleValue) = transformations.add { "rotate($a)" }
    override fun rotate3d(x: Number, y: Number, z: Number, a: CSSAngleValue) =
        transformations.add({ "rotate3d($x, $y, $z, $a)" })

    override fun rotateX(a: CSSAngleValue) = transformations.add { "rotateX($a)" }
    override fun rotateY(a: CSSAngleValue) = transformations.add { "rotateY($a)" }
    override fun rotateZ(a: CSSAngleValue) = transformations.add { "rotateZ($a)" }

    override fun scale(sx: Number) = transformations.add { "scale($sx)" }
    override fun scale(sx: Number, sy: Number) = transformations.add { "scale($sx, $sy)" }
    override fun scale3d(sx: Number, sy: Number, sz: Number) =
        transformations.add { "scale3d($sx, $sy, $sz)" }

    override fun scaleX(s: Number) = transformations.add { "scaleX($s)" }
    override fun scaleY(s: Number) = transformations.add { "scaleY($s)" }
    override fun scaleZ(s: Number) = transformations.add { "scaleZ($s)" }

    override fun skew(ax: CSSAngleValue) = transformations.add { "skew($ax)" }
    override fun skew(ax: CSSAngleValue, ay: CSSAngleValue) = transformations.add { "skew($ax, $ay)" }
    override fun skewX(a: CSSAngleValue) = transformations.add { "skewX($a)" }
    override fun skewY(a: CSSAngleValue) = transformations.add { "skewY($a)" }

    override fun translate(tx: CSSLengthValue) = transformations.add { "translate($tx)" }
    override fun translate(tx: CSSPercentageValue) = transformations.add { "translate($tx)" }

    override fun translate(tx: CSSLengthValue, ty: CSSLengthValue) =
        transformations.add { "translate($tx, $ty)" }

    override fun translate(tx: CSSLengthValue, ty: CSSPercentageValue) =
        transformations.add { "translate($tx, $ty)" }

    override fun translate(tx: CSSPercentageValue, ty: CSSLengthValue) =
        transformations.add { "translate($tx, $ty)" }

    override fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue) =
        transformations.add { "translate($tx, $ty)" }

    override fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    override fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    override fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    override fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    override fun translateX(tx: CSSLengthValue) = transformations.add { "translateX($tx)" }
    override fun translateX(tx: CSSPercentageValue) = transformations.add { "translateX($tx)" }

    override fun translateY(ty: CSSLengthValue) = transformations.add { "translateY($ty)" }
    override fun translateY(ty: CSSPercentageValue) = transformations.add { "translateY($ty)" }

    override fun translateZ(tz: CSSLengthValue) = transformations.add { "translateZ($tz)" }

    override fun toString(): String {
        return transformations.joinToString(" ") { it.apply() }
    }
}

@ExperimentalComposeWebApi
fun StyleBuilder.transform(transformFunction: TransformBuilder.() -> Unit) {
    val transformBuilder = TransformBuilderImplementation()
    property("transform", transformBuilder.apply(transformFunction).toString())
}