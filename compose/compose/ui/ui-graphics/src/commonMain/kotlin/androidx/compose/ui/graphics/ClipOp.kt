/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable

/**
 * Defines how a new clip region should be merged with the existing clip
 * region.
 *
 * Used by [Canvas.clipRect].
 */
@Immutable
@kotlin.jvm.JvmInline
value class ClipOp internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /** Subtract the new region from the existing region. */
        val Difference = ClipOp(0)

        /** Intersect the new region from the existing region. */
        val Intersect = ClipOp(1)
    }

    override fun toString() = when (this) {
        Difference -> "Difference"
        Intersect -> "Intersect"
        else -> "Unknown"
    }
}
