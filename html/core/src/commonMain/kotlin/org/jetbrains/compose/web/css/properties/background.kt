/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-attachment
fun StyleScope.backgroundAttachment(value: String) {
    property("background-attachment", value)
}

fun StyleScope.backgroundClip(value: String) {
    property("background-clip", value)
}

fun StyleScope.backgroundColor(value: CSSColorValue) {
    property("background-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-image
fun StyleScope.backgroundImage(value: String) {
    property("background-image", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-origin
fun StyleScope.backgroundOrigin(value: String) {
    property("background-origin", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-position
fun StyleScope.backgroundPosition(value: String) {
    property("background-position", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-repeat
fun StyleScope.backgroundRepeat(value: String) {
    property("background-repeat", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-size
fun StyleScope.backgroundSize(value: String) {
    property("background-size", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background
fun StyleScope.background(value: String) {
    property("background", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-blend-mode
fun StyleScope.backgroundBlendMode(value: String) {
    property("background-blend-mode", value)
}

