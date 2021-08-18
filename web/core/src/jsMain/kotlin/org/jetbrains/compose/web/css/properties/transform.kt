/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css


fun interface TransformFunction {
    fun apply(): String
}

fun matrixTransform(a: Number, b: Number, c: Number, d: Number, tx: Number, ty: Number)
    = TransformFunction { "matrix($a, $b, $c, $d, $tx, $ty)" }

fun matrix3dTransform(
    a1: Number, b1: Number, c1: Number, d1: Number,
    a2: Number, b2: Number, c2: Number, d2: Number,
    a3: Number, b3: Number, c3: Number, d3: Number,
    a4: Number, b4: Number, c4: Number, d4: Number
) = TransformFunction { "matrix3d($a1, $b1, $c1, $d1, $a2, $b2, $c2, $d2, $a3, $b3, $c3, $d3, $a4, $b4, $c4, $d4)" }

fun perspectiveTransform(d: CSSLengthValue) = TransformFunction { "perspective($d)" }

fun rotateTransform(a: CSSAngleValue) = TransformFunction { "rotate($a)" }
fun rotate3dTransform(x: Number, y: Number, z: Number, a: CSSAngleValue) = TransformFunction { "rotate3d($x, $y, $z, $a)" }
fun rotateXTransform(a: CSSAngleValue) = TransformFunction { "rotateX($a)" }
fun rotateYTransform(a: CSSAngleValue) = TransformFunction { "rotateY($a)" }
fun rotateZTransform(a: CSSAngleValue) = TransformFunction { "rotateZ($a)" }

fun scaleTransform(sx: Number) = TransformFunction { "scale($sx)" }
fun scaleTransform(sx: Number, sy: Number) = TransformFunction { "scale($sx, $sy)" }
fun scale3dTransform(sx: Number, sy: Number, sz: Number) = TransformFunction { "scale3d($sx, $sy, $sz)" }
fun scaleXTransform(s: Number) = TransformFunction { "scaleX($s)" }
fun scaleYTransform(s: Number) = TransformFunction { "scaleY($s)" }
fun scaleZTransform(s: Number) = TransformFunction { "scaleZ($s)" }

fun skewTransform(ax: CSSAngleValue) = TransformFunction { "skew($ax)" }
fun skewTransform(ax: CSSAngleValue, ay: CSSAngleValue) = TransformFunction { "skew($ax, $ay)" }
fun skewXTransform(a: CSSAngleValue) = TransformFunction { "skewX($a)" }
fun skewYTransform(a: CSSAngleValue) = TransformFunction { "skewY($a)" }

fun translate(tx: CSSLengthValue) = TransformFunction { "translate($tx)" }
fun translate(tx: CSSPercentageValue) = TransformFunction { "translate($tx)" }

fun translate(tx: CSSLengthValue, ty: CSSLengthValue) = TransformFunction { "translate($tx, $ty)" }
fun translate(tx: CSSLengthValue, ty: CSSPercentageValue) = TransformFunction { "translate($tx, $ty)" }
fun translate(tx: CSSPercentageValue, ty: CSSLengthValue) = TransformFunction { "translate($tx, $ty)" }
fun translate(tx: CSSPercentageValue, ty: CSSPercentageValue) = TransformFunction { "translate($tx, $ty)" }

fun translate3d(tx: CSSLengthValue, ty: CSSLengthValue, tz: CSSLengthValue) = TransformFunction { "translate3d($tx, $ty, $tz)" }
fun translate3d(tx: CSSLengthValue, ty: CSSPercentageValue, tz: CSSLengthValue) = TransformFunction { "translate3d($tx, $ty, $tz)" }
fun translate3d(tx: CSSPercentageValue, ty: CSSLengthValue, tz: CSSLengthValue) = TransformFunction { "translate3d($tx, $ty, $tz)" }
fun translate3d(tx: CSSPercentageValue, ty: CSSPercentageValue, tz: CSSLengthValue) = TransformFunction { "translate3d($tx, $ty, $tz)" }

fun translateXTransform(tx: CSSLengthValue) = TransformFunction { "translateX($tx)" }
fun translateXTransform(tx: CSSPercentageValue) = TransformFunction { "translateX($tx)" }

fun translateYTransform(ty: CSSLengthValue) = TransformFunction { "translateY($ty)" }
fun translateYTransform(ty: CSSPercentageValue) = TransformFunction { "translateY($ty)" }

fun translateZTransform(tz: CSSLengthValue) = TransformFunction { "translateZ($tz)" }

fun StyleBuilder.transform(vararg transformFunction: TransformFunction) {
    property("transform", transformFunction.joinToString(" ") { it.apply() })
}