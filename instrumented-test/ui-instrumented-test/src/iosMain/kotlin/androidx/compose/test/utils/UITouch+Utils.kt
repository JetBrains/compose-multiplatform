/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.utils

import androidx.compose.ui.unit.DpOffset
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
internal fun UIWindow.touchDown(location: DpOffset): UITouch {
    return UITouch.touchAtPoint(
        point = location.toCGPoint(),
        inWindow = this,
        tapCount = 1L,
        fromEdge = false
    ).also {
        it.send()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun UITouch.moveToLocationOnWindow(location: DpOffset) {
    setLocationInWindow(location.toCGPoint())
    setPhase(UITouchPhase.UITouchPhaseMoved)
    send()
}

@OptIn(ExperimentalForeignApi::class)
internal fun UITouch.hold(): UITouch {
    setPhase(UITouchPhase.UITouchPhaseStationary)
    send()
    return this
}

@OptIn(ExperimentalForeignApi::class)
internal fun UITouch.up(): UITouch {
    setPhase(UITouchPhase.UITouchPhaseEnded)
    send()
    return this
}
