/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.Options
import org.w3c.dom.HTMLTextAreaElement

class TextAreaAttrsBuilder : AttrsBuilder<HTMLTextAreaElement>() {

    fun onInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        addEventListener(INPUT, options) {
            val text = it.nativeEvent.target.asDynamic().value.unsafeCast<String>()
            listener(SyntheticInputEvent(text, it.nativeEvent))
        }
    }
}
