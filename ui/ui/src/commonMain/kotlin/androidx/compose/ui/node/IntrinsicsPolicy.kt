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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.MeasurePolicy

/**
 * Calculates intrinsic measurements. The queries are backed by state depending on the layout
 * node's [MeasurePolicy], such that when the policy is changing, ancestors depending on the
 * result of these intrinsic measurements have their own layout recalculated.
 */
internal class IntrinsicsPolicy(val layoutNode: LayoutNode) {
    private var measurePolicyState: MutableState<MeasurePolicy>? = null

    private var pendingMeasurePolicy: MeasurePolicy? = null

    fun updateFrom(measurePolicy: MeasurePolicy) {
        if (measurePolicyState != null) {
            measurePolicyState!!.value = measurePolicy
        } else {
            pendingMeasurePolicy = measurePolicy
        }
    }

    fun minIntrinsicWidth(height: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.minIntrinsicWidth(layoutNode.children, height)
    }

    fun minIntrinsicHeight(width: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.minIntrinsicHeight(layoutNode.children, width)
    }

    fun maxIntrinsicWidth(height: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.maxIntrinsicWidth(layoutNode.children, height)
    }

    fun maxIntrinsicHeight(width: Int) = with(measurePolicyFromState()) {
        layoutNode.measureScope.maxIntrinsicHeight(layoutNode.children, width)
    }

    private fun measurePolicyFromState(): MeasurePolicy {
        val currentState = measurePolicyState
            ?: mutableStateOf(pendingMeasurePolicy ?: error(NoPolicyError))
        measurePolicyState = currentState
        return currentState.value
    }

    private companion object {
        private const val NoPolicyError =
            "Intrinsic size is queried but there is no measure policy in place."
    }
}
