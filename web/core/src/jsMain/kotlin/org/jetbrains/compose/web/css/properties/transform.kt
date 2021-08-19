/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

fun interface TransformFunction {
    fun apply(): String
}

class TransformBuilder {
    private val transformations = mutableListOf<TransformFunction>()

    fun matrix(a: Number, b: Number, c: Number, d: Number, tx: Number, ty: Number) =
        transformations.add { "matrix($a, $b, $c, $d, $tx, $ty)" }

    fun matrix3d(
        a1: Number, b1: Number, c1: Number, d1: Number,
        a2: Number, b2: Number, c2: Number, d2: Number,
        a3: Number, b3: Number, c3: Number, d3: Number,
        a4: Number, b4: Number, c4: Number, d4: Number
    ) =
        transformations.add { "matrix3d($a1, $b1, $c1, $d1, $a2, $b2, $c2, $d2, $a3, $b3, $c3, $d3, $a4, $b4, $c4, $d4)" }

    fun perspective(d: CSSLengthValue) = transformations.add { "perspective($d)" }

    fun rotate(a: CSSAngleValue) = transformations.add { "rotate($a)" }
    fun rotate3d(x: Number, y: Number, z: Number, a: CSSAngleValue) =
        transformations.add({ "rotate3d($x, $y, $z, $a)" })

    fun rotateX(a: CSSAngleValue) = transformations.add { "rotateX($a)" }
    fun rotateY(a: CSSAngleValue) = transformations.add { "rotateY($a)" }
    fun rotateZ(a: CSSAngleValue) = transformations.add { "rotateZ($a)" }

    fun scale(sx: Number) = transformations.add { "scale($sx)" }
    fun scale(sx: Number, sy: Number) = transformations.add { "scale($sx, $sy)" }
    fun scale3d(sx: Number, sy: Number, sz: Number) =
        transformations.add { "scale3d($sx, $sy, $sz)" }

    fun scaleX(s: Number) = transformations.add { "scaleX($s)" }
    fun scaleY(s: Number) = transformations.add { "scaleY($s)" }
    fun scaleZ(s: Number) = transformations.add { "scaleZ($s)" }

    fun skew(ax: CSSAngleValue) = transformations.add { "skew($ax)" }
    fun skew(ax: CSSAngleValue, ay: CSSAngleValue) = transformations.add { "skew($ax, $ay)" }
    fun skewX(a: CSSAngleValue) = transformations.add { "skewX($a)" }
    fun skewY(a: CSSAngleValue) = transformations.add { "skewY($a)" }

    fun translate(tx: CSSLengthValue) = transformations.add { "translate($tx)" }
    fun translate(tx: CSSPercentageValue) = transformations.add { "translate($tx)" }

    fun translate(tx: CSSLengthValue, ty: CSSLengthValue) =
        transformations.add { "translate($tx, $ty)" }

    fun translate(tx: CSSLengthValue, ty: CSSPercentageValue) =
        transformations.add { "translate($tx, $ty)" }

    fun translate(tx: CSSPercentageValue, ty: CSSLengthValue) =
        transformations.add { "translate($tx, $ty)" }

    fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue) =
        transformations.add { "translate($tx, $ty)" }

    fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue) =
        transformations.add { "translate3d($tx, $ty, $tz)" }

    fun translateX(tx: CSSLengthValue) = transformations.add { "translateX($tx)" }
    fun translateX(tx: CSSPercentageValue) = transformations.add { "translateX($tx)" }

    fun translateY(ty: CSSLengthValue) = transformations.add { "translateY($ty)" }
    fun translateY(ty: CSSPercentageValue) = transformations.add { "translateY($ty)" }

    fun translateZ(tz: CSSLengthValue) = transformations.add { "translateZ($tz)" }

    override fun toString(): String {
        return transformations.joinToString(" ") { it.apply() }
    }
}

fun StyleBuilder.transform(transformFunction: TransformBuilder.() -> Unit) {
    val transformBuilder = TransformBuilder()
    property("transform", transformBuilder.apply(transformFunction).toString())
}