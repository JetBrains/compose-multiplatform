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

package androidx.compose.ui.util.annotation

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class ColorInt()

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class FloatRange(
    val from: Double,
    val to: Double,
    val fromInclusive: Boolean = true,
    val toInclusive: Boolean = true
)

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class IntRange(val from: Long = Long.MIN_VALUE, val to: Long = Long.MAX_VALUE)

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class Size(
    val value: Long = -1,
    val min: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE,
    val multiple: Long = 1
)

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class GuardedBy(
    val value: String
)

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class VisibleForTesting(
    val otherwise: Int = 2
)

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class CallSuper()

@OptionalExpectation
@ExperimentalMultiplatform
expect annotation class MainThread()