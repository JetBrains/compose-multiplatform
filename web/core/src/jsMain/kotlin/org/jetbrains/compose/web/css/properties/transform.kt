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
        transformations.add(TransformFunction { "matrix($a, $b, $c, $d, $tx, $ty)" })

    fun matrix3d(
        a1: Number, b1: Number, c1: Number, d1: Number,
        a2: Number, b2: Number, c2: Number, d2: Number,
        a3: Number, b3: Number, c3: Number, d3: Number,
        a4: Number, b4: Number, c4: Number, d4: Number
    ) =
        transformations.add(TransformFunction { "matrix3d($a1, $b1, $c1, $d1, $a2, $b2, $c2, $d2, $a3, $b3, $c3, $d3, $a4, $b4, $c4, $d4)" })

    fun perspective(d: CSSLengthValue) = transformations.add(TransformFunction { "perspective($d)" })

    fun rotate(a: CSSAngleValue) = transformations.add(TransformFunction { "rotate($a)" })
    fun rotate3d(x: Number, y: Number, z: Number, a: CSSAngleValue) =
        transformations.add(TransformFunction { "rotate3d($x, $y, $z, $a)" })

    fun rotateX(a: CSSAngleValue) = transformations.add(TransformFunction { "rotateX($a)" })
    fun rotateY(a: CSSAngleValue) = transformations.add(TransformFunction { "rotateY($a)" })
    fun rotateZ(a: CSSAngleValue) = transformations.add(TransformFunction { "rotateZ($a)" })

    fun scale(sx: Number) = transformations.add(TransformFunction { "scale($sx)" })
    fun scale(sx: Number, sy: Number) = transformations.add(TransformFunction { "scale($sx, $sy)" })
    fun scale3d(sx: Number, sy: Number, sz: Number) =
        transformations.add(TransformFunction { "scale3d($sx, $sy, $sz)" })

    fun scaleX(s: Number) = transformations.add(TransformFunction { "scaleX($s)" })
    fun scaleY(s: Number) = transformations.add(TransformFunction { "scaleY($s)" })
    fun scaleZ(s: Number) = transformations.add(TransformFunction { "scaleZ($s)" })

    fun skew(ax: CSSAngleValue) = transformations.add(TransformFunction { "skew($ax)" })
    fun skew(ax: CSSAngleValue, ay: CSSAngleValue) = transformations.add(TransformFunction { "skew($ax, $ay)" })
    fun skewX(a: CSSAngleValue) = transformations.add(TransformFunction { "skewX($a)" })
    fun skewY(a: CSSAngleValue) = transformations.add(TransformFunction { "skewY($a)" })

    fun translate(tx: CSSLengthValue) = transformations.add(TransformFunction { "translate($tx)" })
    fun translate(tx: CSSPercentageValue) = transformations.add(TransformFunction { "translate($tx)" })

    fun translate(tx: CSSLengthValue, ty: CSSLengthValue) =
        transformations.add(TransformFunction { "translate($tx, $ty)" })

    fun translate(tx: CSSLengthValue, ty: CSSPercentageValue) =
        transformations.add(TransformFunction { "translate($tx, $ty)" })

    fun translate(tx: CSSPercentageValue, ty: CSSLengthValue) =
        transformations.add(TransformFunction { "translate($tx, $ty)" })

    fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue) =
        transformations.add(TransformFunction { "translate($tx, $ty)" })

    fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue) =
        transformations.add(TransformFunction { "translate3d($tx, $ty, $tz)" })

    fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue) =
        transformations.add(TransformFunction { "translate3d($tx, $ty, $tz)" })

    fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue) =
        transformations.add(TransformFunction { "translate3d($tx, $ty, $tz)" })

    fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue) =
        transformations.add(TransformFunction { "translate3d($tx, $ty, $tz)" })

    fun translateX(tx: CSSLengthValue) = transformations.add(TransformFunction { "translateX($tx)" })
    fun translateX(tx: CSSPercentageValue) = transformations.add(TransformFunction { "translateX($tx)" })

    fun translateY(ty: CSSLengthValue) = transformations.add(TransformFunction { "translateY($ty)" })
    fun translateY(ty: CSSPercentageValue) = transformations.add(TransformFunction { "translateY($ty)" })

    fun translateZ(tz: CSSLengthValue) = transformations.add(TransformFunction { "translateZ($tz)" })

    fun apply(): String {
        return transformations.joinToString(", ") { it.apply() }
    }
}

fun StyleBuilder.transform(transformFunction: TransformBuilder.() -> Unit) {
    property("transform", TransformBuilder().let { transformBuilder ->
        transformFunction.invoke(transformBuilder)
        transformBuilder.apply()
    })
}