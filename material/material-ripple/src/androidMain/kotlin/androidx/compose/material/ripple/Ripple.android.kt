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

package androidx.compose.material.ripple

import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isUnspecified
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

/**
 * Android specific Ripple implementation that uses a [RippleDrawable] under the hood, which allows
 * rendering the ripple animation on the render thread (away from the main UI thread). This
 * allows the ripple to animate smoothly even while the UI thread is under heavy load, such as
 * when navigating between complex screens.
 *
 * @see Ripple
 */
@Stable
internal actual class PlatformRipple actual constructor(
    bounded: Boolean,
    radius: Dp,
    color: State<Color>
) : Ripple(bounded, radius, color) {
    @OptIn(ExperimentalRippleApi::class)
    @Composable
    override fun rememberUpdatedRippleInstance(
        interactionSource: InteractionSource,
        bounded: Boolean,
        radius: Dp,
        color: State<Color>,
        rippleAlpha: State<RippleAlpha>
    ): RippleIndicationInstance {
        val view = findNearestViewGroup()
        // Fallback to drawing inside Compose if needed, using the common implementation
        // TODO(b/188112048): Remove isInEditMode once RenderThread support is fixed in Layoutlib.
        if (!LocalRippleNativeRendering.current || view.isInEditMode) {
            return remember(interactionSource, this) {
                CommonRippleIndicationInstance(bounded, radius, color, rippleAlpha)
            }
        }

        // Create or get the RippleContainer attached to the nearest root Compose view

        var rippleContainer: RippleContainer? = null

        for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child is RippleContainer) {
                rippleContainer = child
                break
            }
        }

        if (rippleContainer == null) {
            rippleContainer = RippleContainer(view.context).apply {
                view.addView(this)
            }
        }

        return remember(interactionSource, this, rippleContainer) {
            AndroidRippleIndicationInstance(bounded, radius, color, rippleAlpha, rippleContainer)
        }
    }

    /**
     * Returns [LocalView] if it is a [ViewGroup], otherwise the nearest parent [ViewGroup] that
     * we will add a [RippleContainer] to.
     *
     * In all normal scenarios this should just be [LocalView], but since [LocalView] is public
     * API theoretically its value can be overridden with a non-[ViewGroup], so we walk up the
     * tree to be safe.
     */
    @Composable
    private fun findNearestViewGroup(): ViewGroup {
        var view: View? = LocalView.current
        while (view !is ViewGroup) {
            // We should never get to a ViewParent that isn't a View, without finding a ViewGroup
            // first
            view = view?.parent as View
        }
        return view
    }
}

@RequiresOptIn(
    "This ripple API is experimental and may change / be removed in the future."
)
public annotation class ExperimentalRippleApi

/**
 * [CompositionLocal] that configures whether native ripples are used to draw the ripple effect
 * inside components. If set to false, the ripples will be drawn inside Compose, consistent with
 * the behavior in previous Compose releases. This is a temporary API, and will be removed when
 * native ripples are stable in Compose.
 *
 * If you use this to avoid a bug with native ripples, please [file a bug](https://issuetracker.google.com/issues/new?component=612128)
 */
@get:ExperimentalRippleApi
@ExperimentalRippleApi
public val LocalRippleNativeRendering: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { true }

/**
 * Android specific [RippleIndicationInstance]. This uses a [RippleHostView] provided by
 * [rippleContainer] to draw ripples in the drawing bounds provided within [drawIndication].
 *
 * The state layer is still handled by [drawStateLayer], and drawn inside Compose.
 */
internal class AndroidRippleIndicationInstance(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: State<Color>,
    private val rippleAlpha: State<RippleAlpha>,
    private val rippleContainer: RippleContainer
) : RippleIndicationInstance(bounded, rippleAlpha), RememberObserver {
    /**
     * Backing [RippleHostView] used to draw ripples for this [RippleIndicationInstance].
     * [mutableStateOf] as we want changes to this to invalidate drawing, and cause us to draw /
     * stop drawing a ripple.
     */
    private var rippleHostView: RippleHostView? by mutableStateOf(null)

    /**
     * State we use to cause invalidations in Compose when the drawable requests an invalidation -
     * since we read this in the draw scope this is equivalent to manually invalidating the internal
     * layer. This is needed as layers internal to the underlying LayoutNode, which we also
     * cannot access from here.
     */
    private var invalidateTick by mutableStateOf(true)

    /**
     * Cache the size of the canvas we will draw the ripple into - this is updated each time
     * [drawIndication] is called. This is needed as before we start animating the ripple, we
     * need to know its size (changing the bounds mid-animation will cause us to continue the
     * animation on the UI thread, not the render thread), but the size is only known inside the
     * draw scope.
     */
    private var rippleSize: Size = Size.Zero

    private var rippleRadius: Int = -1

    /**
     * Flip [invalidateTick] to cause a re-draw when the ripple requests invalidation.
     */
    private val onInvalidateRipple = {
        invalidateTick = !invalidateTick
    }

    override fun ContentDrawScope.drawIndication() {
        // Update size and radius properties needed by addRipple()

        rippleSize = size

        rippleRadius = if (radius.isUnspecified) {
            // Explicitly calculate the radius instead of using RippleDrawable.RADIUS_AUTO
            // since the latest spec does not match with the existing radius calculation in the
            // framework.
            getRippleEndRadius(bounded, size).roundToInt()
        } else {
            radius.roundToPx()
        }

        val color = color.value
        val alpha = rippleAlpha.value.pressedAlpha

        drawContent()
        drawStateLayer(radius, color)
        drawIntoCanvas { canvas ->
            // Reading this ensures that we invalidate when the drawable requests invalidation
            invalidateTick

            rippleHostView?.run {
                // We set these inside addRipple() already, but they may change during the ripple
                // animation, so update them here too.
                // Note that changes to color / alpha will not be reflected in any
                // currently drawn ripples if the ripples are being drawn on the RenderThread,
                // since only the software paint is updated, not the hardware paint used in
                // RippleForeground.
                updateRippleProperties(
                    size = size,
                    radius = rippleRadius,
                    color = color,
                    alpha = alpha
                )

                draw(canvas.nativeCanvas)
            }
        }
    }

    override fun addRipple(interaction: PressInteraction.Press, scope: CoroutineScope) {
        rippleHostView = with(rippleContainer) {
            getRippleHostView().apply {
                addRipple(
                    interaction = interaction,
                    bounded = bounded,
                    size = rippleSize,
                    radius = rippleRadius,
                    color = color.value,
                    alpha = rippleAlpha.value.pressedAlpha,
                    onInvalidateRipple = onInvalidateRipple
                )
            }
        }
    }

    override fun removeRipple(interaction: PressInteraction.Press) {
        rippleHostView?.removeRipple()
    }

    override fun onRemembered() {}

    override fun onForgotten() {
        dispose()
    }

    override fun onAbandoned() {
        dispose()
    }

    private fun dispose() {
        with(rippleContainer) {
            disposeRippleIfNeeded()
        }
    }

    /**
     * Remove the reference to the existing host view, so we don't incorrectly draw if it is
     * recycled and used by another [RippleIndicationInstance].
     */
    fun resetHostView() {
        rippleHostView = null
    }
}
