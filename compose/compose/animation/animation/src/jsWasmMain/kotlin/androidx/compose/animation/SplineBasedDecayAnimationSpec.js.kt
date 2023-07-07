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

package androidx.compose.animation

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

internal actual val platformFlingScrollFriction = 0.015f

@Composable
actual fun <T> rememberSplineBasedDecay(): DecayAnimationSpec<T> {
    // This function will internally update the calculation of fling decay when the density changes,
    // but the reference to the returned spec will not change across calls.
    val density = LocalDensity.current
    return remember(density.density) {
        SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()
    }
}
