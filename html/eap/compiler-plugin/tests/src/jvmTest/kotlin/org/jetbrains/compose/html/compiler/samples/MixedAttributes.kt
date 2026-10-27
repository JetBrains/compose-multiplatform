/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.samples

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.required
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.*

@Composable
fun MixedCard(value: String, selected: Boolean, effect: (String) -> String) {
    Div(attrs = {
        classes("card", value)
        id(effect("id"))
        attr("title", "A & B")
        if (selected) {
            attr("aria-current", effect("current"))
            classes("selected")
        }
        attr("data-value", effect("data"))
    }) { Text(effect("body")) }
}

@Composable
fun MixedOverwrite(first: Boolean, explicit: Boolean, value: String, effect: (String) -> String) {
    Div(attrs = {
        if (first) attr("title", effect("first"))
        attr("aria-label", effect("label"))
        classes(value, "card")
        attr("title", effect("last"))
        if (explicit) attr("class", effect("class"))
    }) { Text("body") }
}

@Composable
fun MixedButton(disabled: Boolean, value: String) {
    Button(attrs = {
        classes("button")
        if (disabled) disabled()
        attr("aria-disabled", disabled.toString())
        attr("aria-label", value)
    }) { Text(value) }
}

@Composable
fun MixedFallback(callback: org.jetbrains.compose.web.attributes.AttrsScope<kotlinx.browser.dom.HTMLDivElement>.() -> Unit) {
    Div(attrs = {
        id("before")
        callback()
        attr("title", "after")
    }) { Text("body") }
}

@Composable
fun MixedInvalid(value: String, explicit: Boolean) {
    Div(attrs = {
        classes(value)
        if (explicit) attr("class", "override")
    }) { Text("body") }
}

@Composable
fun MixedInput(value: String?, required: Boolean, disabled: Boolean, checked: Boolean, effect: (String) -> String) {
    Input(type = if (checked) org.jetbrains.compose.web.attributes.InputType.Radio else org.jetbrains.compose.web.attributes.InputType.Text) {
        id(effect("id"))
        name("field")
        value?.let { attr("value", effect(it)) }
        if (required) required()
        if (disabled) disabled()
        if (checked) attr("checked", "")
        attr("aria-required", required.toString())
    }
}

@Composable
fun MixedInputMemberAttributes(value: String, effect: (String) -> String) {
    Input(type = effect("type").let { org.jetbrains.compose.web.attributes.InputType.Text }) {
        title(effect("title") + value)
        lang(effect("lang"))
        inputMode(effect("inputmode"))
    }
}

@Composable
fun MixedControlledInput(checked: Boolean) {
    Input(type = org.jetbrains.compose.web.attributes.InputType.Checkbox) {
        attr("checked", "")
        checked(checked)
    }
}

@Composable
fun MixedBranches(value: String, selected: Boolean) {
    Div(attrs = {
        if (selected) attr("title", value) else attr("title", "other")
        val label = value + " label"
        attr("aria-label", label)
    }) { Text("body") }
}

@Composable
fun MixedEncodedClasses(value: String, selected: Boolean) {
    Div(attrs = {
        classes("a<&\"")
        if (selected) classes("b&")
        classes(value)
    }) { Text("body") }
}

@Composable
fun MixedLiteralPrefix(selected: Boolean) {
    Div(attrs = {
        id("card")
        attr("title", "<&\"")
        if (selected) attr("hidden", "")
        attr("lang", "en")
        style { property("color", "red") }
    }) { Text("") }
}

@Composable
fun MixedTooltip(id: String) {
    Span(attrs = {
        id(id)
        classes("tooltip-text")
        style { property("display", "none") }
    }) { Text("tooltip") }
}

@Composable
fun MixedFractionalStyle(value: Double, reset: Boolean) {
    Div(attrs = {
        style {
            width(value.px)
            opacity(value)
            property("color", "red", true)
            variable("label", "<&\"\r")
            property("display", "none")
            if (reset) {
                property("color", "blue")
                property("display", "")
            }
        }
    }) { Text("") }
}

@Composable
fun MixedStyleBranches(selected: Boolean, value: String?, effect: (String) -> String) {
    Div(attrs = {
        style { property("color", effect("red")) }
        attr("title", effect("title"))
        style { property("color", effect("blue")) }
        if (selected) style { property("display", effect("none")) }
        value?.let { style { variable("label", effect(it)) } }
    }) { Text("") }
}

@Composable
fun MixedStyleOverride(before: Boolean, explicit: Boolean, effect: (String) -> String) {
    Div(attrs = {
        if (before && explicit) attr("style", effect("override"))
        style { property("color", effect("red")) }
        if (!before && explicit) attr("style", effect("override"))
    }) { Text("") }
}

@Composable
fun MixedInputStyle(block: StyleScope.() -> Unit) {
    Input(type = org.jetbrains.compose.web.attributes.InputType.Text) {
        style(block)
    }
}

@Composable
fun MixedSelectStyle(block: StyleScope.() -> Unit) {
    Select(attrs = { style(block) }) { Text("") }
}

@Composable
fun MixedStyleFallback(value: String) {
    Div(attrs = {
        style {
            this@Div.attr("title", value)
            property("color", "red")
        }
    }) { Text("") }
}
