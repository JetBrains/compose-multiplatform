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
 * Effects are positionally memoized which means that the "resolving" of them depends on execution order and the fact that the
 * resolve happens inside of composition. As a result, we want to use a DslMarker to try and prevent common mistakes of people
 * trying to resolve effects outside of composition.
 *
 * For example, the following should be illegal:
 *
 *     +onCommit {
 *       val x = +state { 123 }
 *     }
 *
 * The `+state` call is illegal because the onCommit callback does not execute during composition, but it would compile without
 * any error if @EffectsDsl wasn't used.
 */
@DslMarker
annotation class EffectsDsl
