/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.compose.web.css

interface StylePropertyEnum : StylePropertyString {
    override fun toString(): String
}

inline val StylePropertyEnum.name get() = toString()
inline val StylePropertyEnum.value get() = toString()

interface DisplayStyle : StylePropertyEnum {
    companion object {
        inline val Block get() = DisplayStyle("block")
        inline val Inline get() = DisplayStyle("inline")
        inline val InlineBlock get() = DisplayStyle("inline-block")
        inline val Flex get() = DisplayStyle("flex")
        inline val LegacyInlineFlex get() = DisplayStyle("inline-flex")
        inline val Grid get() = DisplayStyle("grid")
        inline val LegacyInlineGrid get() = DisplayStyle("inline-grid")
        inline val FlowRoot get() = DisplayStyle("flow-root")

        inline val None get() = DisplayStyle("none")
        inline val Contents get() = DisplayStyle("contents")

// TODO(shabunc): This properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far
//    BlockFlow("block flow")
//    InlineFlow("inline flow")
//    InlineFlowRoot("inline flow-root")
//    BlocklFlex("block flex")
//    InlineFlex("inline flex")
//    BlockGrid("block grid")
//    InlineGrid("inline grid")
//    BlockFlowRoot("block flow-root")

        inline val Table get() = DisplayStyle("table")
        inline val TableRow get() = DisplayStyle("table-row")
        inline val ListItem get() = DisplayStyle("list-item")

        inline val Inherit get() = DisplayStyle("inherit")
        inline val Initial get() = DisplayStyle("initial")
        inline val Unset get() = DisplayStyle("unset")
    }
}

@PublishedApi
internal expect fun createDisplayStyle(value: String): DisplayStyle

inline fun DisplayStyle(value: String): DisplayStyle = createDisplayStyle(value)
