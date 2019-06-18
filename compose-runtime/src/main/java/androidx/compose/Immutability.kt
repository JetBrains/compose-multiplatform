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
 * Just a dummy implementation to prove the behavior for a couple simple cases.
 * TODO: Should return true for deeply immutable objects, frozen objects, primitives, value types, inline classes of immutables, @Model
 * TODO: When we know at compile time, we shouldn't be doing a runtime check for this
 */
@PublishedApi
internal fun isEffectivelyImmutable(value: Any?): Boolean {
    return when (value) {
        is String,
        is Int,
        is Double,
        is Float,
        is Short,
        is Byte,
        is Char,
        is Boolean -> true
        else -> false
    }
}