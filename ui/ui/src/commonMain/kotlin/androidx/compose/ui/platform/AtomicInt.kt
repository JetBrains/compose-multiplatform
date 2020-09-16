/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

// TODO(aelias): Mark the typealiases internal when https://youtrack.jetbrains.com/issue/KT-36695 is fixed.
// Currently, they behave as internal because the actual is internal, even though the expect is public.

expect class AtomicInt(value_: Int) {
    fun addAndGet(delta: Int): Int
    fun compareAndSet(expected: Int, new: Int): Boolean
}