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

package androidx.compose.animation.core

/**
 * This interface allows AnimationClock to be subscribed and unsubscribed.
 */
// TODO: This is a temporary design for Animation Clock. In long term, we may use a Flow<Long>
// or some other data structure that takes care of the subscribing and unsubscribing
// implementation out of the box.
interface AnimationClockObservable {
    /**
     * Subscribes an observer to the animation clock source.
     *
     * Observers may only be added to a clock once. Calls to add the same observer more than
     * once will be ignored, and a single call to [unsubscribe] will unregister from further
     * callbacks.
     *
     * @param observer The observer that will be notified when animation clock time is updated.
     */
    fun subscribe(observer: AnimationClockObserver)

    /**
     * Unsubscribes an observer from the animation clock.
     *
     * Observers may only be added to a clock once. If [subscribe] has been called multiple times
     * with the same observer, a single call to this method will unregister the observer completely.
     *
     * @param observer The observer to be removed from the subscription list.
     */
    fun unsubscribe(observer: AnimationClockObserver)
}

/**
 * Observer for animation clock changes. The observers will be notified via [onAnimationFrame] when
 * the frame time has been updated in animation clock
 */
interface AnimationClockObserver {
    /**
     * This gets called when animation clock ticks.
     *
     * @param frameTimeMillis The frame time of the new tick in milliseconds
     */
    fun onAnimationFrame(frameTimeMillis: Long)
}
