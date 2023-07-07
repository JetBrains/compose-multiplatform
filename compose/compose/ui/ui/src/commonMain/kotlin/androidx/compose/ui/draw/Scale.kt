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

package androidx.compose.ui.draw

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Scale the contents of the composable by the following scale factors along the horizontal
 * and vertical axis respectively. Negative scale factors can be used to mirror content
 * across the corresponding horizontal or vertical axis.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.ScaleNonUniformSample
 *
 * Usage of this API renders this composable into a separate graphics layer
 * @see graphicsLayer
 *
 * @param scaleX Multiplier to scale content along the horizontal axis
 * @param scaleY Multiplier to scale content along the vertical axis
 */
@Stable
fun Modifier.scale(scaleX: Float, scaleY: Float) =
    if (scaleX != 1.0f || scaleY != 1.0f) {
        graphicsLayer(scaleX = scaleX, scaleY = scaleY)
    } else {
        this
    }

/**
 * Scale the contents of both the horizontal and vertical axis uniformly by the same scale factor.
 *
 * Usage of this API renders this composable into a separate graphics layer
 * @see graphicsLayer
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.ScaleUniformSample
 *
 * @param scale Multiplier to scale content along the horizontal and vertical axis
 */
@Stable
fun Modifier.scale(scale: Float) = scale(scale, scale)
