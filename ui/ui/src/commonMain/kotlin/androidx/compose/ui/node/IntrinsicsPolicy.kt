/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.MeasurePolicy

/**
 * Calculates intrinsic measurements. The queries are backed by state depending on the layout
 * node's [MeasurePolicy], such that when the policy is changing, ancestors depending on the
 * result of these intrinsic measurements have their own layout recalculated.
 */
internal class IntrinsicsPolicy(val layoutNode: LayoutNode) {
    private var measurePolicyState: MeasurePolicy? by mutableStateOf(null)

    fun updateFrom(measurePolicy: MeasurePolicy) {
        measurePolicyState = measurePolicy
    }

    fun minIntrinsicWidth(height: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.minIntrinsicWidth(layoutNode.childMeasurables, height)
    }

    fun minIntrinsicHeight(width: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.minIntrinsicHeight(layoutNode.childMeasurables, width)
    }

    fun maxIntrinsicWidth(height: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.maxIntrinsicWidth(layoutNode.childMeasurables, height)
    }

    fun maxIntrinsicHeight(width: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.maxIntrinsicHeight(layoutNode.childMeasurables, width)
    }

    fun minLookaheadIntrinsicWidth(height: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.minIntrinsicWidth(layoutNode.childLookaheadMeasurables, height)
    }

    fun minLookaheadIntrinsicHeight(width: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.minIntrinsicHeight(layoutNode.childLookaheadMeasurables, width)
    }

    fun maxLookaheadIntrinsicWidth(height: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.maxIntrinsicWidth(layoutNode.childLookaheadMeasurables, height)
    }

    fun maxLookaheadIntrinsicHeight(width: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.maxIntrinsicHeight(layoutNode.childLookaheadMeasurables, width)
    }

    private fun measurePolicyFromState(): MeasurePolicy {
        return measurePolicyState ?: error(NoPolicyError)
    }

    private companion object {
        private const val NoPolicyError =
            "Intrinsic size is queried but there is no measure policy in place."
    }
}
