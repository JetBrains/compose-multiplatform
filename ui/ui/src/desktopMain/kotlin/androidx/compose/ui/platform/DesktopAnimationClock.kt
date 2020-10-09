/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.platform

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.ManualAnimationClock

internal class DesktopAnimationClock(
    private val invalidate: () -> Unit
) : AnimationClockObservable {
    private val manual = ManualAnimationClock(0, dispatchOnSubscribe = false)

    val hasObservers get() = manual.hasObservers

    fun onFrame(nanoTime: Long) {
        manual.clockTimeMillis = nanoTime / 1_000_000L
    }

    override fun subscribe(observer: AnimationClockObserver) {
        manual.subscribe(observer)
        invalidate()
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        manual.unsubscribe(observer)
    }
}