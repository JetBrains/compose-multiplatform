/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-attachment
fun StylePropertyBuilder.backgroundAttachment(value: String) {
    property("background-attachment", value)
}

fun StylePropertyBuilder.backgroundClip(value: String) {
    property("background-clip", value)
}

fun StylePropertyBuilder.backgroundColor(value: String) {
    property("background-color", value)
}

fun StylePropertyBuilder.backgroundColor(value: CSSColorValue) {
    property("background-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-image
fun StylePropertyBuilder.backgroundImage(value: String) {
    property("background-image", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-origin
fun StylePropertyBuilder.backgroundOrigin(value: String) {
    property("background-origin", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-position
fun StylePropertyBuilder.backgroundPosition(value: String) {
    property("background-position", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-repeat
fun StylePropertyBuilder.backgroundRepeat(value: String) {
    property("background-repeat", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-size
fun StylePropertyBuilder.backgroundSize(value: String) {
    property("background-size", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background
fun StylePropertyBuilder.background(value: String) {
    property("background", value)
}

