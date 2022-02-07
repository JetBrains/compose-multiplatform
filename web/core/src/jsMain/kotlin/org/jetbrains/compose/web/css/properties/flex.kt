/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.w3c.dom.css.CSS

fun StyleScope.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", flexDirection.value)
}

fun StyleScope.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", flexWrap.value)
}

fun StyleScope.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        "${flexDirection.value} ${flexWrap.value}"
    )
}

fun StyleScope.justifyContent(justifyContent: JustifyContent) {
    property(
        "justify-content",
        justifyContent.value
    )
}
fun StyleScope.alignSelf(alignSelf: AlignSelf) {
    property(
        "align-self",
        alignSelf.value
    )
}

fun StyleScope.alignItems(alignItems: AlignItems) {
    property(
        "align-items",
        alignItems.value
    )
}

fun StyleScope.alignContent(alignContent: AlignContent) {
    property(
        "align-content",
        alignContent.value
    )
}

fun StyleScope.order(value: Int) {
    property("order", value)
}

fun StyleScope.flexGrow(value: Number) {
    property("flex-grow", value)
}

fun StyleScope.flexShrink(value: Number) {
    property("flex-shrink", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/flex-basis
fun StyleScope.flexBasis(value: String) {
    property("flex-basis", value)
}

fun StyleScope.flexBasis(value: CSSNumeric) {
    property("flex-basis", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/flex
fun StyleScope.flex(value: String) {
    property("flex", value)
}

fun StyleScope.flex(value: Int) {
    property("flex", value)
}

fun StyleScope.flex(value: CSSNumeric) {
    property("flex", value)
}

fun StyleScope.flex(flexGrow: Int, flexBasis: CSSNumeric) {
    property("flex", "${flexGrow} ${flexBasis}")
}

fun StyleScope.flex(flexGrow: Int, flexShrink: Int) {
    property("flex", "${flexGrow} ${flexShrink}")
}

fun StyleScope.flex(flexGrow: Int, flexShrink: Int, flexBasis: CSSNumeric) {
    property("flex", "${flexGrow} ${flexShrink} ${flexBasis}")
}

