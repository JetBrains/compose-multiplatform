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
package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.web.elements.Input
import androidx.compose.web.attributes.InputType

@Composable
actual fun SliderActual(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier,
) {
    val stepCount = if (steps == 0) 100 else steps
    val step = (valueRange.endInclusive - valueRange.start) / stepCount

    Input(
        type = InputType.Range,
        value = value.toString(),
        attrs = {
            attr("min", valueRange.start.toString())
            attr("max", valueRange.endInclusive.toString())
            attr("step", step.toString())
            onRangeInput {
                val value: String = it.nativeEvent.target.asDynamic().value
                onValueChange(value.toFloat())
            }
        }
    ) {}
}