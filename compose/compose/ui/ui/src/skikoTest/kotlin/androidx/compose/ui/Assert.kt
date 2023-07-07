/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui

import kotlin.test.assertEquals

internal class AssertThat<T>(val t: T)

internal fun <T> AssertThat<T>.isEqualTo(a: Any?) {
    assertEquals(a, t)
}

internal fun AssertThat<Boolean>.isTrue() = t

internal fun AssertThat<Boolean>.isFalse() = !t

internal fun <T> assertThat(t: T): AssertThat<T> {
    return AssertThat(t)
}
