/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-image
fun StyleScope.listStyleImage(value: String) {
    property("list-style-image", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-position
fun StyleScope.listStylePosition(value: String) {
    property("list-style-position", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-type
fun StyleScope.listStyleType(value: String) {
    property("list-style-type", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style
fun StyleScope.listStyle(value: String) {
    property("list-style", value)
}

