/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.TouchEvent
import kotlinx.browser.dom.TouchList
import kotlinx.browser.dom.events.EventTarget

class SyntheticTouchEvent(
    nativeEvent: TouchEvent,
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val altKey: Boolean = nativeEvent.altKey
    val changedTouches: TouchList = nativeEvent.changedTouches
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val metaKey: Boolean = nativeEvent.metaKey
    val shiftKey: Boolean = nativeEvent.shiftKey
    val touches: TouchList = nativeEvent.touches
}
