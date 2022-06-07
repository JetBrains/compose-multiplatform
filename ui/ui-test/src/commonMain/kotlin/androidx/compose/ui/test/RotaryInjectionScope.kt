/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test

/**
 * The receiver scope of rotary input injection lambda from [performRotaryScrollInput].
 *
 * A rotary event can be sent with [rotateToScrollVertically] or [rotateToScrollHorizontally].
 * All events sent by these methods are batched together and sent as a whole after
 * [performRotaryScrollInput] has executed its code block.
 *
 * Example of performing a scroll with three events:
 * @sample androidx.compose.ui.test.samples.rotaryInputScroll
 */
@ExperimentalTestApi
interface RotaryInjectionScope : InjectionScope {
    /**
     * Sends a scroll event that represents a rotation that will result in a scroll distance of
     * [horizontalScrollPixels]. The event will be sent at the current event time. Positive
     * [horizontalScrollPixels] values will correspond to rotating the scroll wheel clockwise,
     * negative values correspond to rotating the scroll wheel anticlockwise.
     *
     * @param horizontalScrollPixels The amount of scroll, in pixels
     */
    fun rotateToScrollHorizontally(horizontalScrollPixels: Float)

    /**
     * Sends a scroll event that represents a rotation that will result in a scroll distance of
     * [verticalScrollPixels]. The event will be sent at the current event time. Positive
     * [verticalScrollPixels] values will correspond to rotating the scroll wheel clockwise,
     * negative values correspond to rotating the scroll wheel anticlockwise.
     *
     * @param verticalScrollPixels The amount of scroll, in pixels
     */
    fun rotateToScrollVertically(verticalScrollPixels: Float)
}

@ExperimentalTestApi
internal class RotaryInjectionScopeImpl(
    private val baseScope: MultiModalInjectionScopeImpl
) : RotaryInjectionScope, InjectionScope by baseScope {
    private val inputDispatcher get() = baseScope.inputDispatcher

    override fun rotateToScrollHorizontally(horizontalScrollPixels: Float) {
        inputDispatcher.enqueueRotaryScrollHorizontally(horizontalScrollPixels)
    }

    override fun rotateToScrollVertically(verticalScrollPixels: Float) {
        inputDispatcher.enqueueRotaryScrollVertically(verticalScrollPixels)
    }
}
