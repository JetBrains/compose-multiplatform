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
package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * IntermediateLayoutModifier is a [LayoutModifier] that will be skipped when
 * looking ahead. During measure pass, [measure] will be invoked with the constraints from the
 * look-ahead, as well as the target size.
 */
@ExperimentalComposeUiApi
internal interface IntermediateLayoutModifier : LayoutModifier {
    var targetSize: IntSize
}

@OptIn(ExperimentalComposeUiApi::class)
internal class LookaheadIntermediateLayoutModifierImpl(
    val measureBlock: MeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
        lookaheadSize: IntSize
    ) -> MeasureResult,
    inspectorInfo: InspectorInfo.() -> Unit
) : IntermediateLayoutModifier, InspectorValueInfo(inspectorInfo) {

    override var targetSize: IntSize = IntSize.Zero

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult =
        measureBlock(measurable, constraints, targetSize)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LookaheadIntermediateLayoutModifierImpl) return false

        return measureBlock == other.measureBlock && targetSize == other.targetSize
    }

    override fun hashCode(): Int {
        return measureBlock.hashCode() * 31 + targetSize.hashCode()
    }
}
