/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.ChangeEventListener
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.BEFOREINPUT
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.INPUT
import org.jetbrains.compose.web.attributes.InputEventListener
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.SelectEventListener
import org.jetbrains.compose.web.attributes.setInputValue
import org.jetbrains.compose.web.attributes.setTextAreaDefaultValue
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.w3c.dom.HTMLTextAreaElement

@Deprecated(
    message = "Renamed to TextAreaAttrsScope",
    replaceWith = ReplaceWith("TextAreaAttrsScope", "org.jetbrains.compose.web.attributes.builders.TextAreaAttrsScope")
)
typealias TextAreaAttrsBuilder = TextAreaAttrsScope

class TextAreaAttrsScope(attrsScope: AttrsScope<HTMLTextAreaElement>) : AttrsScope<HTMLTextAreaElement> by attrsScope {

    fun value(value: String): AttrsScope<HTMLTextAreaElement> {
        prop(setInputValue, value)
        return this
    }

    fun defaultValue(value: String): AttrsScope<HTMLTextAreaElement> {
        prop(setTextAreaDefaultValue, value)
        return this
    }

    fun onInput(
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        registerEventListener(InputEventListener(INPUT, InputType.Text, listener))
    }

    fun onChange(
        listener: (SyntheticChangeEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        registerEventListener(ChangeEventListener(InputType.Text, listener))
    }

    fun onBeforeInput(
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        registerEventListener(InputEventListener(BEFOREINPUT, InputType.Text, listener))
    }

    fun onSelect(
        listener: (SyntheticSelectEvent<HTMLTextAreaElement>) -> Unit
    ) {
        registerEventListener(SelectEventListener(listener))
    }
}
