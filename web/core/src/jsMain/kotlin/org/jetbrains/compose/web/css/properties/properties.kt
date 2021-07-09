/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

fun StyleBuilder.opacity(value: Number) {
    property("opacity", value)
}

fun StyleBuilder.opacity(value: CSSSizeValue<CSSUnit.percent>) {
    property("opacity", (value.value / 100))
}

interface DisplayProperty  {
    fun block()
    fun contents()
    fun flex()
    fun grid()
    fun inherit()
    fun initial()
    fun inlineBlock()
    fun inline()
    fun legacyFlowRoot()
    fun legacyInlineFlex()
    fun legacyInlineGrid()
    fun listItem()
    fun none()
    fun table()
    fun tableRow()
    fun unset()
}

val StyleBuilder.display: DisplayProperty
    get() = object : NamedProperty(this, "display"), DisplayProperty {
        override fun block() = property("block")
        override fun contents() = property("contents")
        override fun flex() = property("flex")
        override fun grid() = property("grid")
        override fun inherit() = property("inherit")
        override fun initial() = property("initial")
        override fun inlineBlock() = property("inline-block")
        override fun inline() = property("inline")
        override fun legacyFlowRoot() = property("flow-root")
        override fun legacyInlineFlex() = property("inline-flex")
        override fun legacyInlineGrid() = property("inline-grid")
        override fun listItem() = property("list-item")
        override fun none() = property("none")
        override fun table() = property("table")
        override fun tableRow() = property("table-row")
        override fun unset() = property("unset")
    }

