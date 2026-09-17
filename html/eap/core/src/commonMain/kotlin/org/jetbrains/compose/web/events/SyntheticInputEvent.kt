/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import org.jetbrains.compose.web.internal.inputDataCompat
import org.jetbrains.compose.web.internal.inputDataTransferCompat
import org.jetbrains.compose.web.internal.inputIsComposingCompat
import org.jetbrains.compose.web.internal.inputTypeCompat

// @param nativeEvent: Event - we don't use [org.w3c.dom.events.InputEvent] here,
// since for cases it can be just [org.w3c.dom.events.Event]
class SyntheticInputEvent<ValueType, Element : EventTarget> internal constructor(
    val value: ValueType,
    nativeEvent: Event
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
) {
    val data: String? = nativeEvent.inputDataCompat()
    val dataTransfer: DataTransfer? = nativeEvent.inputDataTransferCompat()
    val inputType: String? = nativeEvent.inputTypeCompat()
    val isComposing: Boolean = nativeEvent.inputIsComposingCompat()
}
