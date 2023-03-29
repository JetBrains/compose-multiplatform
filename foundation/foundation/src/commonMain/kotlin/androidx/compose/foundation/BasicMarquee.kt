/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.FixedMotionDurationScale.scaleFactor
import androidx.compose.foundation.MarqueeAnimationMode.Companion.Immediately
import androidx.compose.foundation.MarqueeAnimationMode.Companion.WhileFocused
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

// From https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=736;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalFoundationApi
@get:ExperimentalFoundationApi
val DefaultMarqueeIterations: Int = 3

// From https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=13979;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalFoundationApi
@get:ExperimentalFoundationApi
val DefaultMarqueeDelayMillis: Int = 1_200

// From https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=14088;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalFoundationApi
@get:ExperimentalFoundationApi
val DefaultMarqueeSpacing: MarqueeSpacing = MarqueeSpacing.fractionOfContainer(1f / 3f)

// From https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=13980;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalFoundationApi
@get:ExperimentalFoundationApi
val DefaultMarqueeVelocity: Dp = 30.dp

/**
 * Applies an animated marquee effect to the modified content if it's too wide to fit in the
 * available space. This modifier has no effect if the content fits in the max constraints. The
 * content will be measured with unbounded width.
 *
 * When the animation is running, it will restart from the initial state any time:
 *  - any of the parameters to this modifier change, or
 *  - the content or container size change.
 *
 * The animation only affects the drawing of the content, not its position. The offset returned by
 * the [LayoutCoordinates] of anything inside the marquee is undefined relative to anything outside
 * the marquee, and may not match its drawn position on screen. This modifier also does not
 * currently support content that accepts position-based input such as pointer events.
 *
 * @sample androidx.compose.foundation.samples.BasicMarqueeSample
 *
 * To only animate when the composable is focused, specify [animationMode] and make the composable
 * focusable.
 * @sample androidx.compose.foundation.samples.BasicFocusableMarqueeSample
 *
 * This modifier does not add any visual effects aside from scrolling, but you can add your own by
 * placing modifiers before this one.
 * @sample androidx.compose.foundation.samples.BasicMarqueeWithFadedEdgesSample
 *
 * @param iterations The number of times to repeat the animation. `Int.MAX_VALUE` will repeat
 * forever, and 0 will disable animation.
 * @param animationMode Whether the marquee should start animating [Immediately] or only
 * [WhileFocused]. In [WhileFocused] mode, the modified node or the content must be made
 * [focusable]. Note that the [initialDelayMillis] is part of the animation, so this parameter
 * determines when that initial delay starts counting down, not when the content starts to actually
 * scroll.
 * @param delayMillis The duration to wait before starting each subsequent iteration, in millis.
 * @param initialDelayMillis The duration to wait before starting the first iteration of the
 * animation, in millis. By default, there will be no initial delay if [animationMode] is
 * [WhileFocused], otherwise the initial delay will be [delayMillis].
 * @param spacing A [MarqueeSpacing] that specifies how much space to leave at the end of the
 * content before showing the beginning again.
 * @param velocity The speed of the animation in dps / second.
 */
@ExperimentalFoundationApi
fun Modifier.basicMarquee(
    iterations: Int = DefaultMarqueeIterations,
    animationMode: MarqueeAnimationMode = Immediately,
    // TODO(aosp/2339066) Consider taking an AnimationSpec instead of specific configuration params.
    delayMillis: Int = DefaultMarqueeDelayMillis,
    initialDelayMillis: Int = if (animationMode == Immediately) delayMillis else 0,
    spacing: MarqueeSpacing = DefaultMarqueeSpacing,
    velocity: Dp = DefaultMarqueeVelocity
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "basicMarquee"
        properties["iterations"] = iterations
        properties["animationMode"] = animationMode
        properties["delayMillis"] = delayMillis
        properties["initialDelayMillis"] = initialDelayMillis
        properties["spacing"] = spacing
        properties["velocity"] = velocity
    }
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val modifier = remember(
        iterations,
        delayMillis,
        initialDelayMillis,
        velocity,
        density,
        layoutDirection,
    ) {
        MarqueeModifier(
            iterations = iterations,
            delayMillis = delayMillis,
            initialDelayMillis = initialDelayMillis,
            velocity = velocity * if (layoutDirection == Ltr) 1f else -1f,
            density = density
        )
    }
    modifier.spacing = spacing
    modifier.animationMode = animationMode

    LaunchedEffect(modifier) {
        modifier.runAnimation()
    }

    return@composed modifier
}

private class MarqueeModifier(
    private val iterations: Int,
    private val delayMillis: Int,
    private val initialDelayMillis: Int,
    private val velocity: Dp,
    private val density: Density,
) : Modifier.Element,
    LayoutModifier,
    DrawModifier,
    @Suppress("DEPRECATION") androidx.compose.ui.focus.FocusEventModifier {

    private var contentWidth by mutableStateOf(0)
    private var containerWidth by mutableStateOf(0)
    private var hasFocus by mutableStateOf(false)
    var spacing: MarqueeSpacing by mutableStateOf(DefaultMarqueeSpacing)
    var animationMode: MarqueeAnimationMode by mutableStateOf(Immediately)

    private val offset = Animatable(0f)
    private val direction = sign(velocity.value)
    private val spacingPx by derivedStateOf {
        with(spacing) {
            density.calculateSpacing(contentWidth, containerWidth)
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val childConstraints = constraints.copy(maxWidth = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        containerWidth = constraints.constrainWidth(placeable.width)
        contentWidth = placeable.width
        return layout(containerWidth, placeable.height) {
            // Placing the marquee content in a layer means we don't invalidate the parent draw
            // scope on every animation frame.
            placeable.placeWithLayer(x = (-offset.value * direction).roundToInt(), y = 0)
        }
    }

    override fun ContentDrawScope.draw() {
        val clipOffset = offset.value * direction
        val firstCopyVisible = when (direction) {
            1f -> offset.value < contentWidth
            else -> offset.value < containerWidth
        }
        val secondCopyVisible = when (direction) {
            1f -> offset.value > (contentWidth + spacingPx) - containerWidth
            else -> offset.value > spacingPx
        }
        val secondCopyOffset = when (direction) {
            1f -> contentWidth + spacingPx
            else -> -contentWidth - spacingPx
        }.toFloat()

        clipRect(left = clipOffset, right = clipOffset + containerWidth) {
            // TODO(b/262284225) When both copies are visible, we call drawContent twice. This is
            //  generally a bad practice, however currently the only alternative is to compose the
            //  content twice, which can't be done with a modifier. In the future we might get the
            //  ability to create intrinsic layers in draw scopes, which we should use here to avoid
            //  invalidating the contents' draw scopes.
            if (firstCopyVisible) {
                this@draw.drawContent()
            }
            if (secondCopyVisible) {
                translate(left = secondCopyOffset) {
                    this@draw.drawContent()
                }
            }
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        hasFocus = focusState.hasFocus
    }

    suspend fun runAnimation() {
        if (iterations <= 0) {
            // No animation.
            return
        }

        // Marquee animations should not be affected by motion accessibility settings.
        // Wrap the entire flow instead of just the animation calls so kotlin doesn't have to create
        // an extra CoroutineContext every time the flow emits.
        withContext(FixedMotionDurationScale) {
            snapshotFlow {
                // Don't animate if content fits. (Because coroutines, the int will get boxed
                // anyway.)
                if (contentWidth <= containerWidth) return@snapshotFlow null
                if (animationMode == WhileFocused && !hasFocus) return@snapshotFlow null
                (contentWidth + spacingPx).toFloat()
            }.collectLatest { contentWithSpacingWidth ->
                // Don't animate when the content fits.
                if (contentWithSpacingWidth == null) return@collectLatest

                val spec = createMarqueeAnimationSpec(
                    iterations,
                    contentWithSpacingWidth,
                    initialDelayMillis,
                    delayMillis,
                    velocity,
                    density
                )

                offset.snapTo(0f)
                try {
                    offset.animateTo(contentWithSpacingWidth, spec)
                } finally {
                    offset.snapTo(0f)
                }
            }
        }
    }
}

private fun createMarqueeAnimationSpec(
    iterations: Int,
    targetValue: Float,
    initialDelayMillis: Int,
    delayMillis: Int,
    velocity: Dp,
    density: Density
): AnimationSpec<Float> {
    val pxPerSec = with(density) { velocity.toPx() }
    val singleSpec = velocityBasedTween(
        velocity = pxPerSec.absoluteValue,
        targetValue = targetValue,
        delayMillis = delayMillis
    )
    // Need to cancel out the non-initial delay.
    val startOffset = StartOffset(-delayMillis + initialDelayMillis)
    return if (iterations == Int.MAX_VALUE) {
        infiniteRepeatable(singleSpec, initialStartOffset = startOffset)
    } else {
        repeatable(iterations, singleSpec, initialStartOffset = startOffset)
    }
}

/**
 * Calculates a float [TweenSpec] that moves at a constant [velocity] for an animation from 0 to
 * [targetValue].
 *
 * @param velocity Speed of animation in px / sec.
 */
private fun velocityBasedTween(
    velocity: Float,
    targetValue: Float,
    delayMillis: Int
): TweenSpec<Float> {
    val pxPerMilli = velocity / 1000f
    return tween(
        durationMillis = ceil(targetValue / pxPerMilli).toInt(),
        easing = LinearEasing,
        delayMillis = delayMillis
    )
}

/** Specifies when the [basicMarquee] animation runs. */
@ExperimentalFoundationApi
@JvmInline
value class MarqueeAnimationMode private constructor(private val value: Int) {

    override fun toString(): String = when (this) {
        Immediately -> "Immediately"
        WhileFocused -> "WhileFocused"
        else -> error("invalid value: $value")
    }

    companion object {
        /**
         * Starts animating immediately (accounting for any initial delay), irrespective of focus
         * state.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalFoundationApi
        @get:ExperimentalFoundationApi
        val Immediately = MarqueeAnimationMode(0)

        /**
         * Only animates while the marquee has focus or a node in the marquee's content has focus.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalFoundationApi
        @get:ExperimentalFoundationApi
        val WhileFocused = MarqueeAnimationMode(1)
    }
}

/**
 * A [MarqueeSpacing] with a fixed size.
 */
@ExperimentalFoundationApi
fun MarqueeSpacing(spacing: Dp): MarqueeSpacing = MarqueeSpacing { _, _ -> spacing.roundToPx() }

/**
 * Defines a [calculateSpacing] method that determines the space after the end of [basicMarquee]
 * content before drawing the content again.
 */
@ExperimentalFoundationApi
@Stable
fun interface MarqueeSpacing {
    /**
     * Calculates the space after the end of [basicMarquee] content before drawing the content
     * again.
     *
     * This is a restartable method: any state used to calculate the result will cause the spacing
     * to be re-calculated when it changes.
     *
     * @param contentWidth The width of the content inside the marquee, in pixels. Will always be
     * larger than [containerWidth].
     * @param containerWidth The width of the marquee itself, in pixels. Will always be smaller than
     * [contentWidth].
     * @return The space in pixels between the end of the content and the beginning of the content
     * when wrapping.
     */
    @ExperimentalFoundationApi
    fun Density.calculateSpacing(
        contentWidth: Int,
        containerWidth: Int
    ): Int

    companion object {
        /**
         * A [MarqueeSpacing] that is a fraction of the container's width.
         */
        @ExperimentalFoundationApi
        fun fractionOfContainer(fraction: Float): MarqueeSpacing = MarqueeSpacing { _, width ->
            (fraction * width).roundToInt()
        }
    }
}

/** A [MotionDurationScale] that always reports a [scaleFactor] of 1. */
private object FixedMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = 1f
}