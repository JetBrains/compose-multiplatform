/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.events.WrappedTextInputEvent
import org.w3c.dom.HTMLTextAreaElement

class TextAreaAttrsBuilder : AttrsBuilder<HTMLTextAreaElement>() {

    fun onInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        addEventListener(INPUT, options) {
            val text = it.nativeEvent.target.asDynamic().value.unsafeCast<String>()
            listener(SyntheticInputEvent(text, it.nativeEvent.target as HTMLTextAreaElement, it.nativeEvent))
        }
    }

    @Deprecated(
        message = "It's not reliable as it can be applied to any input type.",
        replaceWith = ReplaceWith("onInput(options, listener)"),
        level = DeprecationLevel.WARNING
    )
    fun onTextInput(options: Options = Options.DEFAULT, listener: (WrappedTextInputEvent) -> Unit) {
        listeners.add(TextInputEventListener(options, listener))
    }
}
