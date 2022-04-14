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

package androidx.compose.animation.graphics.res

import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorComposable
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/**
 * Creates and remembers a [Painter] to render an [AnimatedImageVector]. It renders the image
 * either at the start or the end of all the animations depending on the [atEnd]. Changes to
 * [atEnd] are animated.
 *
 * @param atEnd Whether the animated vector should be rendered at the end of all its animations.
 *
 * @sample androidx.compose.animation.graphics.samples.AnimatedVectorSample
 */
@ExperimentalAnimationGraphicsApi
@Composable
fun rememberAnimatedVectorPainter(
    animatedImageVector: AnimatedImageVector,
    atEnd: Boolean
): Painter {
    return rememberAnimatedVectorPainter(animatedImageVector, atEnd) { group, overrides ->
        RenderVectorGroup(group, overrides)
    }
}

@ExperimentalAnimationGraphicsApi
@Composable
private fun rememberAnimatedVectorPainter(
    animatedImageVector: AnimatedImageVector,
    atEnd: Boolean,
    render: @Composable @VectorComposable (VectorGroup, Map<String, VectorConfig>) -> Unit
): Painter {
    return rememberVectorPainter(
        defaultWidth = animatedImageVector.imageVector.defaultWidth,
        defaultHeight = animatedImageVector.imageVector.defaultHeight,
        viewportWidth = animatedImageVector.imageVector.viewportWidth,
        viewportHeight = animatedImageVector.imageVector.viewportHeight,
        name = animatedImageVector.imageVector.name,
        tintColor = animatedImageVector.imageVector.tintColor,
        tintBlendMode = animatedImageVector.imageVector.tintBlendMode,
        autoMirror = true
    ) { _, _ ->
        val transition = updateTransition(atEnd, label = animatedImageVector.imageVector.name)
        render(
            animatedImageVector.imageVector.root,
            animatedImageVector.targets.associate { target ->
                target.name to target.animator.createVectorConfig(
                    transition,
                    animatedImageVector.totalDuration
                )
            }
        )
    }
}
