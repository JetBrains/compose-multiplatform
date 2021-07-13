/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.w3c.dom.css.CSS

fun StyleBuilder.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", flexDirection.value)
}

fun StyleBuilder.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", flexWrap.value)
}

fun StyleBuilder.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        "${flexDirection.value} ${flexWrap.value}"
    )
}

fun StyleBuilder.justifyContent(justifyContent: JustifyContent) {
    property(
        "justify-content",
        justifyContent.value
    )
}
fun StyleBuilder.alignSelf(alignSelf: AlignSelf) {
    property(
        "align-self",
        alignSelf.value
    )
}

fun StyleBuilder.alignItems(alignItems: AlignItems) {
    property(
        "align-items",
        alignItems.value
    )
}

fun StyleBuilder.alignContent(alignContent: AlignContent) {
    property(
        "align-content",
        alignContent.value
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
