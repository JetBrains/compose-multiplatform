/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.w3c.dom.css.CSS

fun StyleBuilder.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", flexDirection)
}

fun StyleBuilder.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", flexWrap)
}

fun StyleBuilder.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        "${flexDirection} ${flexWrap}"
    )
}

fun <T : ContentPositioning> StyleBuilder.justifyContent(justifyContent: T) {
    property(
        "justify-content",
        justifyContent
    )
}
fun <T : ContentPositioning> StyleBuilder.alignSelf(alignSelf: T) {
    property(
        "align-self",
        alignSelf
    )
}

fun <T : ContentPositioning> StyleBuilder.alignItems(alignItems: T) {
    property(
        "align-items",
        alignItems
    )
}

fun <T : ContentPositioning> StyleBuilder.alignContent(alignContent: T) {
    property(
        "align-content",
        alignContent
    )
}

fun StyleBuilder.order(value: Int) {
    property("order", value)
}

fun StyleBuilder.flexGrow(value: Number) {
    property("flex-grow", value)
}

fun StyleBuilder.flexShrink(value: Number) {
    property("flex-shrink", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/flex-basis
fun StyleBuilder.flexBasis(value: String) {
    property("flex-basis", value)
}

fun StyleBuilder.flexBasis(value: CSSNumeric) {
    property("flex-basis", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/flex
fun StyleBuilder.flex(value: String) {
    property("flex", value)
}

fun StyleBuilder.flex(value: Int) {
    property("flex", value)
}

fun StyleBuilder.flex(value: CSSNumeric) {
    property("flex", value)
}

fun StyleBuilder.flex(flexGrow: Int, flexBasis: CSSNumeric) {
    property("flex", "${flexGrow} ${flexBasis}")
}

fun StyleBuilder.flex(flexGrow: Int, flexShrink: Int) {
    property("flex", "${flexGrow} ${flexShrink}")
}

fun StyleBuilder.flex(flexGrow: Int, flexShrink: Int, flexBasis: CSSNumeric) {
    property("flex", "${flexGrow} ${flexShrink} ${flexBasis}")
}

