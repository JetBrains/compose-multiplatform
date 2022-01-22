/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.compose.common

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Progress
import org.w3c.dom.HTMLElement

@Composable
actual fun Text(modifier: Modifier, text: String) {
    org.jetbrains.compose.web.dom.Text(value = text)
}

@Composable
actual fun Column(modifier: Modifier, builder: @Composable () -> Unit) {
    Div(attrs = {
        modifier.applyAttrs(this)
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        builder()
    }
}

@Composable
actual fun Box(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Div(attrs = { modifier.applyAttrs(this) }) {
        val boxScope = object : BoxScope {
            override fun Modifier.align(alignment: Alignment): Modifier {
                return style {
                    when (alignment) {
                        Alignment.Center -> alignSelf("center")
                    }
                }
            }
        }
        boxScope.content()
    }
}

@Composable
actual fun ProgressBar(modifier: Modifier) {
    Progress(attrs = { modifier.applyAttrs(this) })
}

@Composable
actual fun TextField(modifier: Modifier, label: String, value: String, onValueChange: (String) -> Unit) {
    Input(InputType.Text) {
        this.placeholder(label)
        this.value(value)
        this.onInput { onValueChange(it.value) }
        modifier.applyAttrs(this)
    }
}

@Composable
actual fun Button(modifier: Modifier, text: String, onClick: () -> Unit) {
    Button(
        attrs = {
            modifier.applyAttrs(this)
            this.onClick { onClick() }
        }
    ) {
        org.jetbrains.compose.web.dom.Text(value = text)
    }
}

actual interface Modifier {
    val attrs: List<AttrsBuilder<out HTMLElement>.() -> Unit>

    fun applyAttrs(builder: AttrsBuilder<out HTMLElement>) {
        attrs.forEach { it(builder) }
    }

    fun builder(block: AttrsBuilder<out HTMLElement>.() -> Unit): Modifier {
        val newAttrs = attrs + block
        return object : Modifier {
            override val attrs: List<AttrsBuilder<out HTMLElement>.() -> Unit> = newAttrs
        }
    }

    fun style(block: StyleBuilder.() -> Unit): Modifier {
        return builder { this.style(block) }
    }

    actual companion object : Modifier {
        override val attrs: List<AttrsBuilder<out HTMLElement>.() -> Unit> = emptyList()
    }
}

actual fun Modifier.padding(top: Int, bottom: Int, start: Int, end: Int): Modifier {
    return style {
        paddingTop(top.px)
        paddingBottom(bottom.px)
        paddingLeft(start.px)
        paddingRight(end.px)
    }
}

actual fun Modifier.width(value: Int): Modifier {
    return style {
        width(value.px)
    }
}

actual fun Modifier.height(value: Int): Modifier {
    return style { this.height(value.px) }
}

actual fun Modifier.fillMaxSize(): Modifier {
    return style {
        width(100.percent)
        height(100.percent)
    }
}

actual fun Modifier.fillMaxWidth(): Modifier {
    return style {
        width(100.percent)
    }
}
