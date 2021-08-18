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

fun StyleBuilder.transform(transformFunction: TransformFunction) {
    property("transform", transformFunction.apply())
}