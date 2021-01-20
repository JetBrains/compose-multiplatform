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

internal open class StateImpl<T>(
    val name: T
) : TransitionDefinition.MutableTransitionState, TransitionState {

    internal val props: MutableMap<PropKey<Any, AnimationVector>, Any> = mutableMapOf()

    override operator fun <T, V : AnimationVector> set(propKey: PropKey<T, V>, prop: T) {
        @Suppress("UNCHECKED_CAST")
        propKey as PropKey<Any, AnimationVector>
        if (props[propKey] != null) {
            throw IllegalArgumentException("prop name $propKey already exists")
        }

        props[propKey] = prop as Any
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T, V : AnimationVector> get(propKey: PropKey<T, V>): T {
        propKey as PropKey<Any, AnimationVector>
        return props[propKey] as T
    }
}

/**
 * [TransitionState] holds a number of property values. The value of a property can be queried via
 * [get], providing its property key.
 */
interface TransitionState {
    operator fun <T, V : AnimationVector> get(propKey: PropKey<T, V>): T
}