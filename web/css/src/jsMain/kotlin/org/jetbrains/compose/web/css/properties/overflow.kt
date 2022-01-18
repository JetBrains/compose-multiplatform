/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-x
fun StyleBuilder.overflowX(value: String) {
    property("overflow-x", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-y
fun StyleBuilder.overflowY(value: String) {
    property("overflow-y", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/overflow
fun StyleBuilder.overflow(value: String) {
    property("overflow", value)
}


