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
 * The Emittable interface tells Compose that the implementing class represents a
 * primitive node/type in the view hierarchy produced as a result of composition.
 * Conceptually similar to a RenderObject in flutter.  The result of composition is
 * an updated tree of Emittables, which Compose will maintain/mutate over time as
 * subsequent reconciliations are calculated.
 */
interface Emittable {
    fun emitInsertAt(index: Int, instance: Emittable)
    fun emitRemoveAt(index: Int, count: Int)
    fun emitMove(from: Int, to: Int, count: Int)
}