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

package androidx.compose.foundation.newtext.text

import androidx.compose.ui.util.fastForEachIndexed
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalContracts::class)
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
internal inline fun <T, R> List<T>.fastMapIndexedNotNull(
    transform: (index: Int, T) -> R?
): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEachIndexed { index, e ->
        transform(index, e)?.let { target += it }
    }
    return target
}

internal fun Float.ceilToIntPx(): Int = ceil(this).roundToInt()

internal const val DefaultMinLines = 1
