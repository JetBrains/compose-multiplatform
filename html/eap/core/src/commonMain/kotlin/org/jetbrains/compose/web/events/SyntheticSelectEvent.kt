/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import org.jetbrains.compose.web.internal.selectionEndCompat
import org.jetbrains.compose.web.internal.selectionStartCompat
import org.jetbrains.compose.web.internal.targetValueCompat

class SyntheticSelectEvent<Element : EventTarget> internal constructor(
    nativeEvent: Event,
) : SyntheticEvent<Element>(nativeEvent) {
    val selectionStart: Int = nativeEvent.selectionStartCompat()
    val selectionEnd: Int = nativeEvent.selectionEndCompat()

    fun selection(): String =
        nativeEvent.targetValueCompat()?.substring(selectionStart, selectionEnd) ?: ""
}
