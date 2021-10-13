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

package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.util.fastMaxBy

/**
 * Animated vector graphics object that is generated as a result of
 * [androidx.compose.animation.graphics.res.loadAnimatedVectorResource].
 * It can be composed and rendered by [painterFor].
 *
 * @param imageVector The [ImageVector] to be animated. This is represented with the
 * `android:drawable` parameter of an `<animated-vector>` element.
 */
@ExperimentalAnimationGraphicsApi
@Immutable
class AnimatedImageVector internal constructor(
    val imageVector: ImageVector,
    // The list of [AnimatedVectorTarget]s that specify animations for each of the elements in the
    // drawable. This is represented with `<target>` elements in `<animated-vector>`. This list is
    // expected to be *immutable*.
    internal val targets: List<AnimatedVectorTarget>
) {

    /**
     * The total duration of all the animations in this image, including start delays and repeats.
     */
    val totalDuration = targets.fastMaxBy {
        it.animator.totalDuration
    }?.animator?.totalDuration ?: 0

    /**
     * Creates and remembers a [Painter] to render this [AnimatedImageVector]. It renders the image
     * either at the start or the end of all the animations depending on the [atEnd]. Changes to
     * [atEnd] are animated.
     *
     * @param atEnd Whether the animated vector should be rendered at the end of all its animations.
     *
     * @sample androidx.compose.animation.graphics.samples.AnimatedVectorSample
     */
    @Composable
    fun painterFor(atEnd: Boolean): Painter {
        return painterFor(atEnd) { group, overrides ->
            RenderVectorGroup(group, overrides)
        }
    }

    @Composable
    internal fun painterFor(
        atEnd: Boolean,
        render: @Composable (VectorGroup, Map<String, VectorConfig>) -> Unit
    ): Painter {
        return rememberVectorPainter(
            defaultWidth = imageVector.defaultWidth,
            defaultHeight = imageVector.defaultHeight,
            viewportWidth = imageVector.viewportWidth,
            viewportHeight = imageVector.viewportHeight,
            name = imageVector.name,
            tintColor = imageVector.tintColor,
            tintBlendMode = imageVector.tintBlendMode,
        ) { _, _ ->
            val transition = updateTransition(atEnd, label = imageVector.name)
            render(
                imageVector.root,
                targets.associate { target ->
                    target.name to target.animator.createVectorConfig(transition, totalDuration)
                }
            )
        }
    }
}

/**
 * Definition of animation to one of the elements in a [AnimatedImageVector].
 */
@Immutable
internal class AnimatedVectorTarget(
    val name: String,
    val animator: Animator
)
