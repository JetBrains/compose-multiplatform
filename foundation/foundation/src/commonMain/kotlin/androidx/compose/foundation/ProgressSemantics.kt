/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.format
import kotlin.math.roundToInt

/**
 * Contains the [semantics] required for a determinate progress indicator or the progress part of
 * a slider, that represents progress within [valueRange]. [value] outside of this range will be
 * coerced into this range.
 *
 * @sample androidx.compose.foundation.samples.DeterminateProgressSemanticsSample
 *
 * @param value current value of the ProgressIndicator/Slider. If outside of [valueRange] provided,
 * value will be coerced to this range.
 * @param valueRange range of values that value can take. Passed [value] will be coerced to this
 * range
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, any value from the range specified is allowed.
 * Must not be negative.
 */
@Stable
fun Modifier.progressSemantics(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0
): Modifier {
    val progress = (
        if (valueRange.endInclusive - valueRange.start == 0f) 0f
        else (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        ).coerceIn(0f, 1f)

    // We only display 0% or 100% when it is exactly 0% or 100%.
    val percent = when (progress) {
        0f -> 0
        1f -> 100
        else -> (progress * 100).roundToInt().coerceIn(1, 99)
    }

    return semantics {
        stateDescription = Strings.TemplatePercent.format(percent)
        progressBarRangeInfo =
            ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
    }
}

/**
 * Contains the [semantics] required for an indeterminate progress indicator, that represents the
 * fact of the in-progress operation.
 *
 * If you need determinate progress 0.0 to 1.0, consider using overload with the progress
 * parameter.
 *
 * @sample androidx.compose.foundation.samples.IndeterminateProgressSemanticsSample
 *
 */
@Stable
fun Modifier.progressSemantics(): Modifier {
    return semantics { stateDescription = Strings.InProgress }
}