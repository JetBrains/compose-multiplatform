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

package androidx.compose.foundation.text.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import kotlinx.coroutines.flow.collectLatest

private val UnspecifiedAnimationVector2D = AnimationVector2D(Float.NaN, Float.NaN)

/** Like `Offset.VectorConverter` but propagates [Offset.Unspecified] values. */
private val UnspecifiedSafeOffsetVectorConverter = TwoWayConverter<Offset, AnimationVector2D>(
    convertToVector = {
        if (it.isSpecified) {
            AnimationVector2D(it.x, it.y)
        } else {
            UnspecifiedAnimationVector2D
        }
    },
    convertFromVector = { Offset(it.v1, it.v2) }
)

private val OffsetDisplacementThreshold = Offset(
    Spring.DefaultDisplacementThreshold,
    Spring.DefaultDisplacementThreshold
)

/**
 * The magnifier jumps between discreet cursor positions, so animate it to make it easier to
 * visually follow while dragging.
 */
@Suppress("ModifierInspectorInfo")
internal fun Modifier.animatedSelectionMagnifier(
    magnifierCenter: () -> Offset,
    platformMagnifier: (animatedCenter: () -> Offset) -> Modifier
): Modifier = composed {
    val animatedCenter by rememberAnimatedDerivedStateOf(
        // Can't use Offset.VectorConverter because we need to handle Unspecified specially.
        typeConverter = UnspecifiedSafeOffsetVectorConverter,
        visibilityThreshold = OffsetDisplacementThreshold,
        targetCalculation = magnifierCenter
    )

    return@composed platformMagnifier { animatedCenter }
}

/**
 * Remembers and returns a [State] that will smoothly animate to the result of [targetCalculation]
 * any time the result of [targetCalculation] changes due to any state values it reads change.
 */
@Composable
private fun <T, V : AnimationVector> rememberAnimatedDerivedStateOf(
    typeConverter: TwoWayConverter<T, V>,
    visibilityThreshold: T? = null,
    animationSpec: AnimationSpec<T> = SpringSpec(visibilityThreshold = visibilityThreshold),
    targetCalculation: () -> T,
): State<T> {
    val targetValue by remember { derivedStateOf(targetCalculation) }
    val animatable = remember {
        Animatable(targetValue, typeConverter, visibilityThreshold)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { targetValue }
            .collectLatest {
                animatable.animateTo(it, animationSpec)
            }
    }
    return animatable.asState()
}
