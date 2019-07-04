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
 * A [CompositionReference] is an opaque type that is used to logically "link" two compositions
 * together. The [CompositionReference] instance represents a reference to the "parent" composition
 * in a specific position of that composition's tree, and the instance can then be given to a new
 * "child" composition. This reference ensures that invalidations and ambients flow logically
 * through the two compositions as if they were not separate.
 *
 * @see compositionReference
 * @see Compose.composeInto
 */
interface CompositionReference {
    fun <T> getAmbient(key: Ambient<T>): T
    fun invalidate(sync: Boolean)
    fun <T> invalidateConsumers(key: Ambient<T>)
    fun <N> registerComposer(composer: Composer<N>)
}