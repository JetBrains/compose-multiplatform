/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

fun StyleBuilder.color(value: String) {
    property("color", value)
}

fun StyleBuilder.color(value: CSSColorValue) {
    // color hasn't Typed OM yet
    property("color", value)
}