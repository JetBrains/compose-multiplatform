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

package androidx.compose.ui.node

import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints

@Deprecated("MeasureBlocks was deprecated. Please use MeasurePolicy instead.")
internal interface MeasureBlocks {
    /**
     * The function used to measure the child. It must call [MeasureScope.layout] before
     * completing.
     */
    fun measure(
        measureScope: MeasureScope,
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
     */
    fun minIntrinsicWidth(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurables: List<IntrinsicMeasurable>,
        h: Int
    ): Int

    /**
     * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
     */
    fun minIntrinsicHeight(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurables: List<IntrinsicMeasurable>,
        w: Int
    ): Int

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
     */
    fun maxIntrinsicWidth(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurables: List<IntrinsicMeasurable>,
        h: Int
    ): Int

    /**
     * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
     */
    fun maxIntrinsicHeight(
        intrinsicMeasureScope: IntrinsicMeasureScope,
        measurables: List<IntrinsicMeasurable>,
        w: Int
    ): Int
}