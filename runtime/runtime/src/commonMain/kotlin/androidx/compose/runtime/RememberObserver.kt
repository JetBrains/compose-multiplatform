/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

/**
 * Objects implementing this interface are notified when they are initially used in a composition
 * and when they are no longer being used.
 *
 * An object is [remembered][onRemembered] by the composition if it is [remember]ed in at least
 * one place in a composition. Compose may implicitly [remember] an object if doing so is
 * required to restart the composition later, such as for composable function parameters. An
 * object is [forgotten][onForgotten] when it is no longer [remember]ed anywhere in that
 * composition. If a single instance is [remember]ed in more than one location in
 * the same composition, its [onRemembered] and [onForgotten] will be called for each location in
 * the composition.
 *
 * When objects implementing this interface is remmembered and forgotten together,
 * the order of [onForgotten] is guaranteed to be called in the opposite order of [onRemembered].
 * For example, if two objects, A and B are [remember]ed together, A followed by B,
 * [onRemembered] will be called first on A then on B. If they forgotten together then
 * [onForgotten] will be called on B first then on A.
 *
 * Implementations of [RememberObserver] should generally not expose those object references
 * outside of their immediate usage sites. A [RememberObserver] reference that is propagated to
 * multiple parts of a composition might remain present in the composition for longer than
 * expected if it is remembered (explicitly or implicitly) elsewhere, and a [RememberObserver]
 * that appears more once can have its callback methods called multiple times in no meaningful
 * order and on multiple threads.
 *
 * An object remembered in only one place in only one composition is guaranteed to,
 *
 * 1. have either [onRemembered] or [onAbandoned] called
 * 2. if [onRemembered] is called, [onForgotten] will eventually be called
 */
@Suppress("CallbackName")
interface RememberObserver {
    /**
     * Called when this object is successfully remembered by a composition. This method is called on
     * the composition's **apply thread.**
     */
    fun onRemembered()

    /**
     * Called when this object is forgotten by a composition. This method is called on the
     * composition's **apply thread.**
     */
    fun onForgotten()

    /**
     * Called when this object is returned by the callback to `remember` but is not successfully
     * remembered by a composition.
     */
    fun onAbandoned()
}