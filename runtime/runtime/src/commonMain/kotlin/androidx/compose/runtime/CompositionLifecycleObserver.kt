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

package androidx.compose.runtime

/**
 * Objects implementing this interface are notified when they are initially
 * used in a composition and when they are no longer being used.
 *
 * An object [enters][onEnter] the composition if it is [remember]ed in at least one place in a
 * composition. Compose may implicitly [remember] an object if doing so is required to restart
 * the composition later, such as for composable function parameters. An object [leaves][onLeave]
 * a composition when it is no longer [remember]ed anywhere in that composition.
 *
 * Implementations of [CompositionLifecycleObserver] should generally not expose those object
 * references outside of their immediate usage sites. A [CompositionLifecycleObserver] reference
 * that is propagated to multiple parts of a composition might remain present in the composition
 * for longer than expected if it is remembered (explicitly or implicitly) elsewhere in the same
 * composition, and a [CompositionLifecycleObserver] that appears in more than one composition may
 * have its lifecycle callback methods called multiple times in no meaningful order and on
 * multiple threads. Implementations intending to support this use case should implement atomic
 * reference counting or a similar strategy to correctly manage resources.
 */
interface CompositionLifecycleObserver {
    /**
     * Called when this object successfully enters a composition.
     * This method is called on the composition's **apply thread.**
     */
    fun onEnter() {}

    /**
     * Called when this object leaves a composition.
     * This method is called on the composition's **apply thread.**
     */
    fun onLeave() {}
}

/**
 * Holds an instance of a CompositionLifecycleObserver and a count of how many times it is
 * used during composition.
 *
 * The holder can be used as a key for the identity of the instance.
 */
internal class CompositionLifecycleObserverHolder(val instance: CompositionLifecycleObserver) {
    var count: Int = 0
    override fun equals(other: Any?): Boolean =
        other is CompositionLifecycleObserverHolder && instance === other.instance
    override fun hashCode(): Int = identityHashCode(instance)
}