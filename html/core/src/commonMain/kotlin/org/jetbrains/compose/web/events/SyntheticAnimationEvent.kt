/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import org.jetbrains.compose.web.internal.animationElapsedTimeCompat
import org.jetbrains.compose.web.internal.animationNameCompat
import org.jetbrains.compose.web.internal.animationPseudoElementCompat

class SyntheticAnimationEvent internal constructor(
    nativeEvent: Event,
) : SyntheticEvent<EventTarget>(nativeEvent) {
    val animationName: String = nativeEvent.animationNameCompat()
    val elapsedTime: Number = nativeEvent.animationElapsedTimeCompat()
    val pseudoElement: String = nativeEvent.animationPseudoElementCompat()
}
