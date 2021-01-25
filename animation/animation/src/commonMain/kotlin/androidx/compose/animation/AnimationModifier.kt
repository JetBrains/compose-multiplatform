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

@file:Suppress("DEPRECATION")

package androidx.compose.animation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * This modifier animates its own size when its child modifier (or the child composable if it
 * is already at the tail of the chain) changes size. This allows the parent modifier to observe
 * a smooth size change, resulting in an overall continuous visual change.
 *
 * An [AnimationSpec] can be optionally specified for the size change animation. By default,
 * [SpringSpec] will be used. Clipping defaults to true, such that the content outside of animated
 * size will not be shown.
 *
 * An optional [endListener] can be supplied to get notified when the size change animation is
 * finished. Since the content size change can be dynamic in many cases, both start size and end
 * size will be passed to the [endListener]. __Note:__ if the animation is interrupted, the start
 * size will be the size at the point of interruption. This is intended to help determine the
 * direction of the size change (i.e. expand or collapse in x and y dimensions).
 *
 * @sample androidx.compose.animation.samples.AnimateContent
 *
 * @param animSpec the animation that will be used to animate size change
 * @param clip whether content outside of animated size should be clipped
 * @param endListener optional listener to be called when the content change animation is completed.
 */
fun Modifier.animateContentSize(
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true,
    endListener: ((startSize: IntSize, endSize: IntSize) -> Unit)? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animateContentSize"
        properties["animSpec"] = animSpec
        properties["clip"] = clip
        properties["endListener"] = endListener
    }
) {
    // TODO: Listener could be a fun interface after 1.4
    val clock = AmbientAnimationClock.current.asDisposableClock()
    val animModifier = remember {
        SizeAnimationModifier(animSpec, clock)
    }
    animModifier.listener = endListener

    if (clip) {
        this.clipToBounds().then(animModifier)
    } else {
        this.then(animModifier)
    }
}

/**
 * This class creates a [LayoutModifier] that measures children, and responds to children's size
 * change by animating to that size. The size reported to parents will be the animated size.
 */
private class SizeAnimationModifier(
    val animSpec: AnimationSpec<IntSize>,
    val clock: AnimationClockObservable
) : LayoutModifier {
    var listener: ((startSize: IntSize, endSize: IntSize) -> Unit)? = null

    data class AnimData(
        val anim: AnimatedValueModel<IntSize, AnimationVector2D>,
        var startSize: IntSize
    )

    var animData: AnimData? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val placeable = measurable.measure(constraints)

        val measuredSize = IntSize(placeable.width, placeable.height)

        val (width, height) = animateTo(measuredSize)
        return layout(width, height) {
            placeable.placeRelative(0, 0)
        }
    }

    fun animateTo(targetSize: IntSize): IntSize {
        val data = animData?.apply {
            if (targetSize != anim.targetValue) {
                startSize = anim.value
                anim.animateTo(targetSize, animSpec) { reason, endSize ->
                    if (reason == AnimationEndReason.TargetReached) {
                        listener?.invoke(startSize, endSize)
                    }
                }
            }
        } ?: AnimData(
            AnimatedValueModel(
                targetSize, IntSize.VectorConverter, clock, IntSize(1, 1)
            ),
            targetSize
        )

        animData = data
        return data.anim.value
    }
}
