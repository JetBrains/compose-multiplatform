/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.text

/**
 * On CI we only track a subset of metrics, to run a full suite for analysis set this to true.
 */
internal const val DoFullBenchmark = false

fun <T> List<Array<T>>.filterForCi(
    selector: List<Array<T>>.() -> Array<T> = { first() }
): List<Array<T>> = if (DoFullBenchmark) {
    this
} else {
    listOf(selector())
}

@Suppress("UNCHECKED_CAST")
fun Array<Int>.filterForCi(
    selector: Array<Int>.() -> Int = { min() }
): Array<Any> = if (DoFullBenchmark) {
    this as Array<Any>
} else {
    arrayOf(selector())
}