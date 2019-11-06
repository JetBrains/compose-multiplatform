/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

import android.view.Choreographer

private class AndroidRecomposer : Recomposer() {

    private var frameScheduled = false

    private val frameCallback = Choreographer.FrameCallback {
        frameScheduled = false
        dispatchRecomposes()
    }

    override fun scheduleChangesDispatch() {
        if (!frameScheduled) {
            frameScheduled = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun hasPendingChanges(): Boolean = frameScheduled

    override fun recomposeSync() {
        if (frameScheduled) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            frameCallback.doFrame(0)
        }
    }
}

internal actual fun createRecomposer(): Recomposer {
    return AndroidRecomposer()
}