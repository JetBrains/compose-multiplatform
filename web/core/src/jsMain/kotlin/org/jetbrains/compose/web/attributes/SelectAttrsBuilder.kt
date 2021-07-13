/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.Options
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.w3c.dom.HTMLSelectElement

class SelectAttrsBuilder : AttrsBuilder<HTMLSelectElement>() {

    fun onInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        addEventListener(INPUT, options) {
            val value = it.nativeEvent.target?.asDynamic().value?.toString()
            listener(SyntheticInputEvent(value, it.nativeEvent))
        }
    }

    fun onChange(
        options: Options = Options.DEFAULT,
        listener: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        addEventListener(CHANGE, options) {
            val value = it.nativeEvent.target?.asDynamic().value?.toString()
            listener(SyntheticChangeEvent(value, it.nativeEvent))
        }
    }

    fun onBeforeInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        addEventListener(BEFOREINPUT, options) {
            val value = it.nativeEvent.target?.asDynamic().value?.toString()
            listener(SyntheticInputEvent(value, it.nativeEvent))
        }
    }
}
