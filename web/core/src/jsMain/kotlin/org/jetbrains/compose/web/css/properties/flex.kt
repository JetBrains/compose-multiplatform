/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

fun StylePropertyBuilder.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", flexDirection.value)
}

fun StylePropertyBuilder.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", flexWrap.value)
}

fun StylePropertyBuilder.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        "${flexDirection.value} ${flexWrap.value}"
    )
}

fun StylePropertyBuilder.justifyContent(justifyContent: JustifyContent) {
    property(
        "justify-content",
        justifyContent.value
    )
}
fun StylePropertyBuilder.alignSelf(alignSelf: AlignSelf) {
    property(
        "align-self",
        alignSelf.value
    )
}

fun StylePropertyBuilder.alignItems(alignItems: AlignItems) {
    property(
        "align-items",
        alignItems.value
    )
}

fun StylePropertyBuilder.alignContent(alignContent: AlignContent) {
    property(
        "align-content",
        alignContent.value
    )
}

fun StylePropertyBuilder.order(value: Int) {
    property("order", value)
}

fun StylePropertyBuilder.flexGrow(value: Number) {
    property("flex-grow", value)
}

fun StylePropertyBuilder.flexShrink(value: Number) {
    property("flex-shrink", value)
}


