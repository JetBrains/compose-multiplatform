/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.w3c.dom.HTMLTextAreaElement

class TextAreaAttrsBuilder : AttrsBuilder<HTMLTextAreaElement>() {

    fun onInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(InputEventListener(INPUT, options, InputType.Text, listener))
    }

    fun onChange(
        options: Options = Options.DEFAULT,
        listener: (SyntheticChangeEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(ChangeEventListener(options, InputType.Text, listener))
    }

    fun onBeforeInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(InputEventListener(BEFOREINPUT, options, InputType.Text, listener))
    }

    fun onSelect(
        options: Options = Options.DEFAULT,
        listener: (SyntheticSelectEvent<HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(SelectEventListener(options, listener))
    }
}
