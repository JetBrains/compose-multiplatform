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

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

@RequiresOptIn(message = "This is an experimental animation API.")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalAnimationApi

/**
 * [EnterTransition] defines how an [AnimatedVisibility] Composable appears on screen as it
 * becomes visible. The 3 categories of EnterTransitions available are:
 * 1. fade [fadeIn])
 * 2. slide: [slideIn], [slideInHorizontally], [slideInVertically]
 * 3. expand: [expandIn], [expandHorizontally], [expandVertically]
 * They can be combined using plus operator,  for example:
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * __Note__: [fadeIn] and [slideIn] do not affect the size of the [AnimatedVisibility]
 * composable. In contrast, [expandIn] will grow the clip bounds to reveal the whole content. This
 * will automatically animate other layouts out of the way, very much like [animateContentSize].
 *
 * @see fadeIn
 * @see slideIn
 * @see slideInHorizontally
 * @see slideInVertically
 * @see expandIn
 * @see expandHorizontally
 * @see expandVertically
 * @see AnimatedVisibility
 */
@ExperimentalAnimationApi
@Immutable
sealed class EnterTransition {
    internal abstract val data: TransitionData

    /**
     * Combines different enter transitions. The order of the [EnterTransition]s being combined
     * does not matter, as these [EnterTransition]s will start simultaneously.
     *
     * @sample androidx.compose.animation.samples.FullyLoadedTransition
     *
     * @param enter another [EnterTransition] to be combined
     */
    @Stable
    operator fun plus(enter: EnterTransition): EnterTransition {
        return EnterTransitionImpl(
            TransitionData(
                fade = data.fade ?: enter.data.fade,
                slide = data.slide ?: enter.data.slide,
                changeSize = data.changeSize ?: enter.data.changeSize
            )
        )
    }
    // TODO: Support EnterTransition.None
}

/**
 * [ExitTransition] defines how an [AnimatedVisibility] Composable disappears on screen as it
 * becomes not visible. The 3 categories of [ExitTransition] available are:
 * 1. fade: [fadeOut]
 * 2. slide: [slideOut], [slideOutHorizontally], [slideOutVertically]
 * 3. shrink: [shrinkOut], [shrinkHorizontally], [shrinkVertically]
 *
 * They can be combined using plus operator, for example:
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * __Note__: [fadeOut] and [slideOut] do not affect the size of the [AnimatedVisibility]
 * composable. In contrast, [shrinkOut] (and [shrinkHorizontally], [shrinkVertically]) will shrink
 * the clip bounds to reveal less and less of the content.  This will automatically animate other
 * layouts to fill in the space, very much like [animateContentSize].
 *
 * @see fadeOut
 * @see slideOut
 * @see slideOutHorizontally
 * @see slideOutVertically
 * @see shrinkOut
 * @see shrinkHorizontally
 * @see shrinkVertically
 * @see AnimatedVisibility
 */
@ExperimentalAnimationApi
@Immutable
sealed class ExitTransition {
    internal abstract val data: TransitionData

    /**
     * Combines different exit transitions. The order of the [ExitTransition]s being combined
     * does not matter, as these [ExitTransition]s will start simultaneously.
     *
     * @sample androidx.compose.animation.samples.FullyLoadedTransition
     *
     * @param exit another [ExitTransition] to be combined.
     */
    @Stable
    operator fun plus(exit: ExitTransition): ExitTransition {
        return ExitTransitionImpl(
            TransitionData(
                fade = data.fade ?: exit.data.fade,
                slide = data.slide ?: exit.data.slide,
                changeSize = data.changeSize ?: exit.data.changeSize
            )
        )
    }
    // TODO: Support ExitTransition.None
}

/**
 * This fades in the content of the transition, from the specified starting alpha (i.e.
 * [initialAlpha]) to 1f, using the supplied [animSpec]. [initialAlpha] defaults to 0f,
 * and [spring] is used by default.
 *
 * @sample androidx.compose.animation.samples.FadeTransition
 *
 * @param initialAlpha the starting alpha of the enter transition, 0f by default
 * @param animSpec the [AnimationSpec] for this animation, [spring] by default
 */
@Stable
@ExperimentalAnimationApi
fun fadeIn(
    initialAlpha: Float = 0f,
    animSpec: AnimationSpec<Float> = spring()
): EnterTransition {
    return EnterTransitionImpl(TransitionData(fade = Fade(initialAlpha, animSpec)))
}

/**
 * This fades out the content of the transition, from full opacity to the specified target alpha
 * (i.e. [targetAlpha]), using the supplied [animSpec]. By default, the content will be faded out to
 * fully transparent (i.e. [targetAlpha] defaults to 0), and [animSpec] uses [spring] by default.
 *
 * @sample androidx.compose.animation.samples.FadeTransition
 *
 * @param targetAlpha the target alpha of the exit transition, 0f by default
 * @param animSpec the [AnimationSpec] for this animation, [spring] by default
 */
@Stable
@ExperimentalAnimationApi
fun fadeOut(targetAlpha: Float = 0f, animSpec: AnimationSpec<Float> = spring()): ExitTransition {
    return ExitTransitionImpl(TransitionData(fade = Fade(targetAlpha, animSpec)))
}

/**
 * This slides in the content of the transition, from a starting offset defined in [initialOffset]
 * to `IntOffset(0, 0)`. The direction of the slide can be controlled by configuring the
 * [initialOffset]. A positive x value means sliding from right to left, whereas a negative x
 * value will slide the content to the right. Similarly positive and negative y values
 * correspond to sliding up and down, respectively.
 *
 * If the sliding is only desired horizontally or vertically, instead of along both axis, consider
 * using [slideInHorizontally] or [slideInVertically].
 *
 * [initialOffset] is a lambda that takes the full size of the content and returns an offset.
 * This allows the offset to be defined proportional to the full size, or as an absolute value.
 *
 * @sample androidx.compose.animation.samples.SlideInOutSample
 *
 * @param initialOffset a lambda that takes the full size of the content and returns the initial
 *                        offset for the slide-in
 * @param animSpec the animation used for the slide-in, [spring] by default.
 */
@Stable
@ExperimentalAnimationApi
fun slideIn(
    initialOffset: (fullSize: IntSize) -> IntOffset,
    animSpec: AnimationSpec<IntOffset> = spring()
): EnterTransition {
    return EnterTransitionImpl(TransitionData(slide = Slide(initialOffset, animSpec)))
}

/**
 * This slides out the content of the transition, from an offset of `IntOffset(0, 0)` to the
 * target offset defined in [targetOffset]. The direction of the slide can be controlled by
 * configuring the [targetOffset]. A positive x value means sliding from left to right, whereas a
 * negative x value would slide the content from right to left. Similarly,  positive and negative y
 * values correspond to sliding down and up, respectively.
 *
 * If the sliding is only desired horizontally or vertically, instead of along both axis, consider
 * using [slideOutHorizontally] or [slideOutVertically].
 *
 * [targetOffset] is a lambda that takes the full size of the content and returns an offset.
 * This allows the offset to be defined proportional to the full size, or as an absolute value.
 *
 * @sample androidx.compose.animation.samples.SlideInOutSample
 *
 * @param targetOffset a lambda that takes the full size of the content and returns the target
 *                     offset for the slide-out
 * @param animSpec the animation used for the slide-out, [spring] by default.
 */
@Stable
@ExperimentalAnimationApi
fun slideOut(
    targetOffset: (fullSize: IntSize) -> IntOffset,
    animSpec: AnimationSpec<IntOffset> = spring()
): ExitTransition {
    return ExitTransitionImpl(TransitionData(slide = Slide(targetOffset, animSpec)))
}

/**
 * This expands the clip bounds of the appearing content from the size returned from [initialSize]
 * to the full size. [expandFrom] controls which part of the content gets revealed first. By
 * default, the clip bounds animates from `IntSize(0, 0)` to full size, starting from revealing the
 * bottom right corner (or bottom left corner in RTL layouts) of the content, to fully revealing
 * the entire content as the size expands.
 *
 * __Note__: [expandIn] animates the bounds of the content. This bounds change will also result
 * in the animation of other layouts that are dependent on this size.
 *
 * [initialSize] is a lambda that takes the full size of the content and returns an initial size of
 * the bounds of the content. This allows not only absolute size, but also an initial size that
 * is proportional to the content size.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * For expanding only horizontally or vertically, consider [expandHorizontally], [expandVertically].
 *
 * @sample androidx.compose.animation.samples.ExpandInShrinkOutSample
 *
 * @param expandFrom the starting point of the expanding bounds, [Alignment.BottomEnd] by default.
 * @param initialSize the start size of the expanding bounds, returning `IntSize(0, 0)` by default.
 * @param animSpec the animation used for the expanding animation, [spring] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 */
@Stable
@ExperimentalAnimationApi
fun expandIn(
    expandFrom: Alignment = Alignment.BottomEnd,
    initialSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): EnterTransition {
    return EnterTransitionImpl(
        TransitionData(
            changeSize = ChangeSize(expandFrom, initialSize, animSpec, clip)
        )
    )
}

/**
 * This shrinks the clip bounds of the disappearing content from the full size to the size returned
 * from [targetSize]. [shrinkTowards] controls the direction of the bounds shrink animation. By
 * default, the clip bounds animates from  full size to `IntSize(0, 0)`, shrinking towards the
 * the bottom right corner (or bottom left corner in RTL layouts) of the content.
 *
 * __Note__: [shrinkOut] animates the bounds of the content. This bounds change will also result
 * in the animation of other layouts that are dependent on this size.
 *
 * [targetSize] is a lambda that takes the full size of the content and returns a target size of
 * the bounds of the content. This allows not only absolute size, but also a target size that
 * is proportional to the content size.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * For shrinking only horizontally or vertically, consider [shrinkHorizontally], [shrinkVertically].
 *
 * @sample androidx.compose.animation.samples.ExpandInShrinkOutSample
 *
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.BottomEnd] by default.
 * @param targetSize returns the end size of the shrinking bounds, `IntSize(0, 0)` by default.
 * @param animSpec the animation used for the shrinking animation, [spring] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 */
@Stable
@ExperimentalAnimationApi
fun shrinkOut(
    shrinkTowards: Alignment = Alignment.BottomEnd,
    targetSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): ExitTransition {
    return ExitTransitionImpl(
        TransitionData(
            changeSize = ChangeSize(shrinkTowards, targetSize, animSpec, clip)
        )
    )
}

/**
 * This expands the clip bounds of the appearing content horizontally, from the width returned from
 * [initialWidth] to the full width. [expandFrom] controls which part of the content gets revealed
 * first. By default, the clip bounds animates from 0 to full width, starting from the end
 * of the content, and expand to fully revealing the whole content.
 *
 * __Note__: [expandHorizontally] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [initialWidth] is a lambda that takes the full width of the content and returns an initial width
 * of the bounds of the content. This allows not only an absolute width, but also an initial width
 * that is proportional to the content width.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.HorizontalTransitionSample
 *
 * @param expandFrom the starting point of the expanding bounds, [Alignment.End] by default.
 * @param initialWidth the start width of the expanding bounds, returning 0 by default.
 * @param animSpec the animation used for the expanding animation, [spring] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 */
@Stable
@ExperimentalAnimationApi
fun expandHorizontally(
    expandFrom: Alignment.Horizontal = Alignment.End,
    initialWidth: (fullWidth: Int) -> Int = { 0 },
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): EnterTransition {
    // TODO: Support different animation types
    return expandIn(
        expandFrom.toAlignment(),
        initialSize = { IntSize(initialWidth(it.width), it.height) },
        animSpec = animSpec,
        clip = clip
    )
}

/**
 * This expands the clip bounds of the appearing content vertically, from the height returned from
 * [initialHeight] to the full height. [expandFrom] controls which part of the content gets revealed
 * first. By default, the clip bounds animates from 0 to full height, revealing the bottom edge
 * first, followed by the rest of the content.
 *
 * __Note__: [expandVertically] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [initialHeight] is a lambda that takes the full height of the content and returns an initial height
 * of the bounds of the content. This allows not only an absolute height, but also an initial height
 * that is proportional to the content height.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.ExpandShrinkVerticallySample
 *
 * @param expandFrom the starting point of the expanding bounds, [Alignment.Bottom] by default.
 * @param initialHeight the start height of the expanding bounds, returning 0 by default.
 * @param animSpec the animation used for the expanding animation, [spring] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 */
@Stable
@ExperimentalAnimationApi
fun expandVertically(
    expandFrom: Alignment.Vertical = Alignment.Bottom,
    initialHeight: (fullHeight: Int) -> Int = { 0 },
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): EnterTransition {
    return expandIn(
        expandFrom.toAlignment(),
        { IntSize(it.width, initialHeight(it.height)) },
        animSpec,
        clip
    )
}

/**
 * This shrinks the clip bounds of the disappearing content horizontally, from the full width to
 * the width returned from [targetWidth]. [shrinkTowards] controls the direction of the bounds shrink
 * animation. By default, the clip bounds animates from full width to 0, shrinking towards the
 * the end of the content.
 *
 * __Note__: [shrinkHorizontally] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [targetWidth] is a lambda that takes the full width of the content and returns a target width of
 * the content. This allows not only absolute width, but also a target width that is proportional
 * to the content width.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.HorizontalTransitionSample
 *
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.End] by default.
 * @param targetWidth returns the end width of the shrinking bounds, 0 by default.
 * @param animSpec the animation used for the shrinking animation, [spring] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 */
@Stable
@ExperimentalAnimationApi
fun shrinkHorizontally(
    shrinkTowards: Alignment.Horizontal = Alignment.End,
    targetWidth: (fullWidth: Int) -> Int = { 0 },
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): ExitTransition {
    // TODO: Support different animation types
    return shrinkOut(
        shrinkTowards.toAlignment(),
        targetSize = { IntSize(targetWidth(it.width), it.height) },
        animSpec = animSpec,
        clip = clip
    )
}

/**
 * This shrinks the clip bounds of the disappearing content vertically, from the full height to
 * the height returned from [targetHeight]. [shrinkTowards] controls the direction of the bounds shrink
 * animation. By default, the clip bounds animates from full height to 0, shrinking towards the
 * the bottom of the content.
 *
 * __Note__: [shrinkVertically] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [targetHeight] is a lambda that takes the full height of the content and returns a target height of
 * the content. This allows not only absolute height, but also a target height that is proportional
 * to the content height.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.ExpandShrinkVerticallySample
 *
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.Bottom] by default.
 * @param targetHeight returns the end height of the shrinking bounds, 0 by default.
 * @param animSpec the animation used for the shrinking animation, [spring] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 */
@Stable
@ExperimentalAnimationApi
fun shrinkVertically(
    shrinkTowards: Alignment.Vertical = Alignment.Bottom,
    targetHeight: (fullHeight: Int) -> Int = { 0 },
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): ExitTransition {
    // TODO: Support different animation types
    return shrinkOut(
        shrinkTowards.toAlignment(),
        targetSize = { IntSize(it.width, targetHeight(it.height)) },
        animSpec = animSpec,
        clip = clip
    )
}

/**
 * This slides in the content horizontally, from a starting offset defined in
 * [initialOffsetX] to `0`. The direction of the slide can be controlled by configuring the
 * [initialOffsetX]. A positive value means sliding from right to left, whereas a negative
 * value would slide the content from left to right.
 *
 * [initialOffsetX] is a lambda that takes the full width of the content and returns an
 * offset. This allows the starting offset to be defined proportional to the full size, or as an
 * absolute value. It defaults to return half of negative width, which would offset the content
 * to the left by half of its width, and slide towards the right.
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * @param initialOffsetX a lambda that takes the full width of the content and returns the
 *                             initial offset for the slide-in, by default it returns `-fullWidth/2`
 * @param animSpec the animation used for the slide-in, [spring] by default.
 */
@Stable
@ExperimentalAnimationApi
fun slideInHorizontally(
    initialOffsetX: (fullWidth: Int) -> Int = { -it / 2 },
    animSpec: AnimationSpec<IntOffset> = spring()
): EnterTransition =
    slideIn(
        initialOffset = { IntOffset(initialOffsetX(it.width), 0) },
        animSpec = animSpec
    )

/**
 * This slides in the content vertically, from a starting offset defined in
 * [initialOffsetY] to `0`. The direction of the slide can be controlled by configuring the
 * [initialOffsetY]. A positive initial offset means sliding up, whereas a negative value would
 * slide the content down.
 *
 * [initialOffsetY] is a lambda that takes the full Height of the content and returns an
 * offset. This allows the starting offset to be defined proportional to the full height, or as an
 * absolute value. It defaults to return half of negative height, which would offset the content
 * up by half of its Height, and slide down.
 *
 * @sample androidx.compose.animation.samples.FullyLoadedTransition
 *
 * @param initialOffsetY a lambda that takes the full Height of the content and returns the
 *                           initial offset for the slide-in, by default it returns `-fullHeight/2`
 * @param animSpec the animation used for the slide-in, [spring] by default.
 */
@Stable
@ExperimentalAnimationApi
fun slideInVertically(
    initialOffsetY: (fullHeight: Int) -> Int = { -it / 2 },
    animSpec: AnimationSpec<IntOffset> = spring()
): EnterTransition =
    slideIn(
        initialOffset = { IntOffset(0, initialOffsetY(it.height)) },
        animSpec = animSpec
    )

/**
 * This slides out the content horizontally, from 0 to a target offset defined in
 * [targetOffsetX]. The direction of the slide can be controlled by configuring the
 * [targetOffsetX]. A positive value means sliding to the right, whereas a negative
 * value would slide the content towards the left.
 *
 * [targetOffsetX] is a lambda that takes the full width of the content and returns an
 * offset. This allows the target offset to be defined proportional to the full size, or as an
 * absolute value. It defaults to return half of negaive width, which would slide the content to
 * the left by half of its width.
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * @param targetOffsetX a lambda that takes the full width of the content and returns the
 *                             initial offset for the slide-in, by default it returns `fullWidth/2`
 * @param animSpec the animation used for the slide-out, [spring] by default.
 */
@Stable
@ExperimentalAnimationApi
fun slideOutHorizontally(
    targetOffsetX: (fullWidth: Int) -> Int = { -it / 2 },
    animSpec: AnimationSpec<IntOffset> = spring()
): ExitTransition =
    slideOut(
        targetOffset = { IntOffset(targetOffsetX(it.width), 0) },
        animSpec = animSpec
    )

/**
 * This slides out the content vertically, from 0 to a target offset defined in
 * [targetOffsetY]. The direction of the slide-out can be controlled by configuring the
 * [targetOffsetY]. A positive target offset means sliding down, whereas a negative value would
 * slide the content up.
 *
 * [targetOffsetY] is a lambda that takes the full Height of the content and returns an
 * offset. This allows the target offset to be defined proportional to the full height, or as an
 * absolute value. It defaults to return half of the negative height, which would slide the content
 * up by half of its Height.
 *
 * @param targetOffsetY a lambda that takes the full Height of the content and returns the
 *                         target offset for the slide-out, by default it returns `fullHeight/2`
 * @param animSpec the animation used for the slide-out, [spring] by default.
 */
@Stable
@ExperimentalAnimationApi
fun slideOutVertically(
    targetOffsetY: (fullHeight: Int) -> Int = { -it / 2 },
    animSpec: AnimationSpec<IntOffset> = spring()
): ExitTransition =
    slideOut(
        targetOffset = { IntOffset(0, targetOffsetY(it.height)) },
        animSpec = animSpec
    )

/*********************** Below are internal classes and methods ******************/
@Immutable
internal data class Fade(val alpha: Float, val animSpec: AnimationSpec<Float>)

@Immutable
internal data class Slide(
    val slideOffset: (fullSize: IntSize) -> IntOffset,
    val animSpec: AnimationSpec<IntOffset>
)

@Immutable
internal data class ChangeSize(
    val alignment: Alignment,
    val startSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
    val animSpec: AnimationSpec<IntSize> = spring(),
    val clip: Boolean = true
)

@OptIn(ExperimentalAnimationApi::class)
@Immutable
private class EnterTransitionImpl(override val data: TransitionData) : EnterTransition()

@ExperimentalAnimationApi
@Immutable
private class ExitTransitionImpl(override val data: TransitionData) : ExitTransition()

private fun Alignment.Horizontal.toAlignment() =
    when (this) {
        Alignment.Start -> Alignment.CenterStart
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.Center
    }

private fun Alignment.Vertical.toAlignment() =
    when (this) {
        Alignment.Top -> Alignment.TopCenter
        Alignment.Bottom -> Alignment.BottomCenter
        else -> Alignment.Center
    }

internal enum class AnimStates { Entering, Visible, Exiting, Gone }

@Immutable
internal data class TransitionData(
    val fade: Fade? = null,
    val slide: Slide? = null,
    val changeSize: ChangeSize? = null
)

/**
 * Alignment does NOT stay consistent in enter vs. exit animations. For example, the enter could be
 * expanding from top, but exit could be shrinking towards the bottom. As a result, when such an
 * animation is interrupted, it becomes very tricky to handle that. This is why there needs to be
 * two types of size animations: alignment based and rect based. When alignment stays the same,
 * size is the only value that needs to be animated. When alignment changes, however, the only
 * sensible solution is to fall back to rect based solution. Namely, this calculates the current
 * clip rect based on alignment and size, and the final rect based on the new alignment and the
 * ending size. In rect based animations, the size will still be animated using the provided size
 * animation, and the offset will be animated using physics as a part of the interruption
 * handling logic.
 */
internal interface SizeAnimation {
    val anim: AnimatedValueModel<IntSize, AnimationVector2D>
    var clip: Boolean
    val size: IntSize
        get() = anim.value

    val offset: (IntSize) -> IntOffset
    val isAnimating: Boolean
    val listener: (AnimationEndReason, Any) -> Unit

    /**
     * The instance returned may be different than the caller if the alignment has changed. Either
     * way the returned animation will be configured to animate to the new target.
     */
    fun animateTo(
        target: IntSize,
        alignment: Alignment,
        fullSize: IntSize,
        spec: AnimationSpec<IntSize>,
        clock: AnimationClockObservable
    ): SizeAnimation

    val alignment: Alignment
}

/**
 * This is the animation class used to animate content size. However, this animation may get
 * interrupted before it finishes. If the new size target is based on the same alignment, this
 * instance can be re-used to handle that interruption. A more complicated and hairy case is when
 * the alignment changes (from top aligned to bottom aligned), in which case we have to fall back
 * to Rect based animation to properly handle the alignment change.
 */
private class AlignmentBasedSizeAnimation(
    override val anim: AnimatedValueModel<IntSize, AnimationVector2D>,
    override val alignment: Alignment,
    override var clip: Boolean,
    override val listener: (AnimationEndReason, Any) -> Unit
) : SizeAnimation {

    override val offset: (IntSize) -> IntOffset
        get() = {
            alignment.align(it, anim.value, LayoutDirection.Ltr)
        }

    override fun animateTo(
        target: IntSize,
        alignment: Alignment,
        fullSize: IntSize,
        spec: AnimationSpec<IntSize>,
        clock: AnimationClockObservable
    ): SizeAnimation {
        if (anim.targetValue != target) {
            anim.animateTo(target, spec, listener)
        }

        if (alignment == this.alignment) {
            return this
        } else {
            // Alignment changed
            val offset = this.offset(fullSize)
            return RectBasedSizeAnimation(anim, offset, clip, clock, listener)
        }
    }

    override val isAnimating: Boolean
        get() = anim.isRunning
}

/**
 * This class animates the rect of the clip bounds, as a fallback for when enter and exit size
 * change animations have different alignment.
 */
private class RectBasedSizeAnimation(
    override val anim: AnimatedValueModel<IntSize, AnimationVector2D>,
    targetOffset: IntOffset,
    override var clip: Boolean,
    clock: AnimationClockObservable,
    override val listener: (AnimationEndReason, Any) -> Unit
) : SizeAnimation {
    private val offsetAnim: AnimatedValueModel<IntOffset, AnimationVector2D>

    init {
        offsetAnim = AnimatedValueModel(
            IntOffset(0, 0), IntOffset.VectorConverter, clock,
            IntOffset(1, 1)
        )
        offsetAnim.animateTo(targetOffset, onEnd = listener)
    }

    override val alignment: Alignment
        get() = Alignment.TopStart

    override val offset: (IntSize) -> IntOffset
        get() = {
            offsetAnim.value
        }

    override fun animateTo(
        target: IntSize,
        alignment: Alignment,
        fullSize: IntSize,
        spec: AnimationSpec<IntSize>,
        clock: AnimationClockObservable
    ): SizeAnimation {
        val targetOffSet = alignment.align(fullSize, target, LayoutDirection.Ltr)
        if (offsetAnim.targetValue != targetOffSet) {
            offsetAnim.animateTo(targetOffSet, onEnd = listener)
        }
        if (target != anim.targetValue) {
            anim.animateTo(target, spec, listener)
        }
        return this
    }

    override val isAnimating: Boolean
        get() = (anim.isRunning || offsetAnim.isRunning)
}

private operator fun IntSize.minus(b: IntSize) =
    IntSize(width - b.width, height - b.height)

internal interface TransitionAnimation {
    val isRunning: Boolean
    var state: AnimStates
    val modifier: Modifier
    fun getAnimatedSize(fullSize: IntSize): Pair<IntOffset, IntSize>? = null
    val listener: (AnimationEndReason, Any) -> Unit
}

/**
 * This class animates alpha through a graphics layer modifier.
 */
private class FadeTransition(
    val enter: Fade? = null,
    val exit: Fade? = null,
    clock: AnimationClockObservable,
    override val listener: (AnimationEndReason, Any) -> Unit
) : TransitionAnimation {
    override val isRunning: Boolean
        get() = alphaAnim.isRunning
    override val modifier: Modifier
        get() = if (alphaAnim.isRunning || (state == AnimStates.Exiting && exit != null)) {
            // Only add graphics layer if the animation is running, or if it's waiting for other
            // exit animations to finish.
            Modifier.graphicsLayer(alpha = alphaAnim.value)
        } else {
            Modifier
        }

    override var state: AnimStates = AnimStates.Gone
        set(value) {
            if (value == field) {
                return
            }
            // Animation state has changed if we get here.
            if (value == AnimStates.Entering) {
                // Animation is interrupted from fade out, now fade in
                if (alphaAnim.isRunning) {
                    enter?.apply {
                        // If fade in animation specified, use that. Otherwise use default.
                        alphaAnim.animateTo(1f, animSpec, listener)
                    } ?: alphaAnim.animateTo(1f, onEnd = listener)
                } else {
                    // set up initial values for alphaAnimation
                    enter?.apply {
                        // If fade in is defined start from pre-defined `alphaFrom`. If no fade in is defined,
                        // snap the alpha to 1f
                        alphaAnim.snapTo(alpha)
                        alphaAnim.animateTo(1f, animSpec, listener)
                        // If no enter is defined and animation isn't running, snap to alpha = 1
                    } ?: alphaAnim.snapTo(1f)
                }
            } else if (value == AnimStates.Exiting) {
                if (alphaAnim.isRunning) {
                    // interrupting alpha animation: directly animating to out value if defined,
                    // otherwise let the fade-in finish
                    exit?.apply {
                        alphaAnim.animateTo(alpha, animSpec, listener)
                    }
                } else {
                    // set up alpha animation to fade out, if fade out is defined
                    exit?.apply {
                        alphaAnim.animateTo(alpha, animSpec, listener)
                    }
                }
            }
            field = value
        }

    val alphaAnim: AnimatedFloatModel =
        AnimatedFloatModel(1f, clock, visibilityThreshold = 0.02f)
}

private class SlideTransition(
    val enter: Slide? = null,
    val exit: Slide? = null,
    val clock: AnimationClockObservable,
    override val listener: (AnimationEndReason, Any) -> Unit
) : TransitionAnimation {
    override val isRunning: Boolean
        get() {
            if (slideAnim?.isRunning == true) {
                return true
            }
            if (state != currentState) {
                if (state == AnimStates.Entering && enter != null) {
                    return true
                } else if (state == AnimStates.Exiting && exit != null) {
                    return true
                }
            }
            return false
        }
    override var state: AnimStates = AnimStates.Gone
    var currentState: AnimStates = AnimStates.Gone
    override val modifier: Modifier = Modifier.composed {
        SlideModifier()
    }

    inner class SlideModifier : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)

            updateAnimation(IntSize(placeable.width, placeable.height))
            return layout(placeable.width, placeable.height) {
                placeable.place(slideAnim?.value ?: IntOffset.Zero)
            }
        }
    }

    fun updateAnimation(fullSize: IntSize) {
        if (state == currentState) {
            return
        }
        // state changed
        if (state == AnimStates.Entering) {
            // Animation is interrupted from slide out, now slide in
            enter?.apply {
                // If slide in animation specified, use that. Otherwise use default.
                val anim = slideAnim
                    ?: AnimatedValueModel(
                        slideOffset(fullSize), IntOffset.VectorConverter,
                        clock, IntOffset(1, 1)
                    )
                anim.animateTo(IntOffset.Zero, animSpec, listener)
                slideAnim = anim
            } ?: slideAnim?.animateTo(IntOffset.Zero, onEnd = listener)
        } else if (state == AnimStates.Exiting) {
            // interrupting alpha animation: directly animating to out value if defined,
            // otherwise let it finish
            exit?.apply {
                val anim = slideAnim
                    ?: AnimatedValueModel(
                        IntOffset.Zero, IntOffset.VectorConverter,
                        clock, IntOffset(1, 1)
                    )
                anim.animateTo(slideOffset(fullSize), animSpec, listener)
                slideAnim = anim
            }
        }
        currentState = state
    }

    var slideAnim: AnimatedValueModel<IntOffset, AnimationVector2D>? = null
}

private class ChangeSizeTransition(
    val enter: ChangeSize? = null,
    val exit: ChangeSize? = null,
    val clock: AnimationClockObservable,
    override val listener: (AnimationEndReason, Any) -> Unit
) : TransitionAnimation {

    override val isRunning: Boolean
        get() {
            if (sizeAnim?.isAnimating == true) {
                return true
            }

            // If the state has changed, and corresponding animations are defined, then animation
            // will be running in this current frame in the layout stage.
            if (state != currentState) {
                if (state == AnimStates.Entering && enter != null) {
                    return true
                } else if (state == AnimStates.Exiting && exit != null) {
                    return true
                }
            }
            return false
        }

    // This is the pending state, which sets currentState in layout stage
    override var state: AnimStates = AnimStates.Gone

    // This tracks the current resolved state. State change happens in composition, but the
    // resolution happens during layout, since we won't know the size until then.
    var currentState: AnimStates = AnimStates.Gone

    override fun getAnimatedSize(fullSize: IntSize): Pair<IntOffset, IntSize> {
        sizeAnim?.apply {
            if (state == currentState) {
                // If no state change, return the current size animation value.
                return offset(fullSize) to size
            }
        }

        // If we get here, animate state has changed.
        if (state == AnimStates.Entering) {
            if (enter != null) {
                // if no on-going size animation, create a new one.
                val anim = sizeAnim?.run {
                    // If the animation is not running and the alignment isn't the same, prefer
                    // AlignmentBasedSizeAnimation over rect based animation.
                    if (!isRunning && alignment != enter.alignment) {
                        null
                    } else
                        this
                } ?: AlignmentBasedSizeAnimation(
                    AnimatedValueModel(
                        enter.startSize.invoke(fullSize),
                        IntSize.VectorConverter,
                        clock,
                        visibilityThreshold = IntSize(1, 1)
                    ),
                    enter.alignment, enter.clip, listener
                )
                // Animate to full size
                sizeAnim = anim.animateTo(
                    fullSize, enter.alignment, fullSize, enter.animSpec, clock
                )
            } else {
                // If enter isn't defined for size change, re-target the current animation, if any
                sizeAnim?.apply {
                    animateTo(fullSize, alignment, fullSize, spring(), clock)
                }
            }
        } else if (state == AnimStates.Exiting) {
            exit?.apply {
                // If a size change exit animation is defined, re-target on-going animation if
                // any, otherwise create a new one.
                val anim = sizeAnim?.run {
                    // If the current size animation is idling, switch to AlignmentBasedAnimation if
                    // needed.
                    if (isRunning && alignment != exit.alignment) {
                        null
                    } else {
                        this
                    }
                } ?: AlignmentBasedSizeAnimation(
                    AnimatedValueModel(
                        fullSize, IntSize.VectorConverter, clock, IntSize(1, 1)
                    ),
                    alignment, clip, listener
                )

                sizeAnim = anim.animateTo(
                    startSize(fullSize), alignment, fullSize, animSpec, clock
                )
            }
            // If exit isn't defined, but the enter animation is still on-going, let it finish
        }
        currentState = state
        return sizeAnim?.run { offset(fullSize) to size } ?: IntOffset.Zero to fullSize
    }

    override val modifier: Modifier
        get() {
            val clip: Boolean = sizeAnim?.clip
                ?: if (state == AnimStates.Entering) {
                    enter?.clip
                } else {
                    exit?.clip
                } ?: false
            return if (clip) Modifier.clipToBounds() else Modifier
        }

    var sizeAnim: SizeAnimation? = null
}

@OptIn(ExperimentalAnimationApi::class)
internal class TransitionAnimations constructor(
    enter: EnterTransition,
    exit: ExitTransition,
    clock: AnimationClockObservable,
    onFinished: () -> Unit
) {
    // This happens during composition.
    fun updateState(state: AnimStates) {
        animations.fastForEach { it.state = state }
    }

    val listener: (AnimationEndReason, Any) -> Unit = { reason, _ ->
        if (reason == AnimationEndReason.TargetReached && !isAnimating) {
            onFinished()
        }
    }

    // This is called after measure before placement.
    fun getAnimatedSize(fullSize: IntSize): Pair<IntOffset, IntSize>? {
        animations.fastForEach {
            val animSize = it.getAnimatedSize(fullSize)
            if (animSize != null) {
                return animSize
            }
        }
        return null
    }

    val isAnimating: Boolean
        get() = animations.fastFirstOrNull { it.isRunning }?.isRunning ?: false

    val animations: List<TransitionAnimation>

    init {
        animations = mutableListOf()
        // Only set up animations when either enter or exit transition is defined.
        if (enter.data.fade != null || exit.data.fade != null) {
            animations.add(
                FadeTransition(enter.data.fade, exit.data.fade, clock, listener)
            )
        }
        if (enter.data.slide != null || exit.data.slide != null) {
            animations.add(
                SlideTransition(enter.data.slide, exit.data.slide, clock, listener)
            )
        }
        if (enter.data.changeSize != null || exit.data.changeSize != null) {
            animations.add(
                ChangeSizeTransition(enter.data.changeSize, exit.data.changeSize, clock, listener)
            )
        }
    }

    val modifier: Modifier
        get() {
            var modifier: Modifier = Modifier
            animations.fastForEach { modifier = modifier.then(it.modifier) }
            return modifier
        }
}
