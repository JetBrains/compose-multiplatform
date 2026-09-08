/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.FocusEvent

class SyntheticFocusEvent internal constructor(
    nativeEvent: FocusEvent,
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val relatedTarget: EventTarget? = nativeEvent.relatedTarget
}
