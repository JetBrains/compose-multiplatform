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

package androidx.compose.foundation.animation

import androidx.compose.animation.core.TargetAnimation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AmbientDensity

@Composable
internal actual fun actualFlingConfig(adjustTarget: (Float) -> TargetAnimation?): FlingConfig {
    // This function will internally update the calculation of fling decay when the density changes,
    // but the reference to the returned FlingConfig will not change across calls.
    val density = AmbientDensity.current
    return remember(density.density) {
        val decayAnimation = DesktopFlingDecaySpec(density)
        FlingConfig(
            decayAnimation = decayAnimation,
            adjustTarget = adjustTarget
        )
    }
}
