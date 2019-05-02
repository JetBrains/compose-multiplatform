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

/**
 * Instances of classes implementing this interface are notified when they are initially
 * used during composition and when they are no longer being used.
 */
interface CompositionLifecycleObserver {
    /**
     * Called when the instance is used in composition.
     */
    fun onEnter()

    /**
     * Called when the instance is no longer used in the composition.
     */
    fun onLeave()
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
        other === instance || other is CompositionLifecycleObserverHolder &&
                instance === other.instance
    override fun hashCode(): Int = System.identityHashCode(instance)
}
