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

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Common Ripple implementation that directly animates and draws to the underlying canvas
 * provided by [ContentDrawScope].
 *
 * @see Ripple
 */
@Stable
internal class CommonRipple(
    bounded: Boolean,
    radius: Dp,
    color: State<Color>
) : Ripple(bounded, radius, color) {
    @Composable
    override fun rememberUpdatedRippleInstance(
        interactionSource: InteractionSource,
        bounded: Boolean,
        radius: Dp,
        color: State<Color>,
        rippleAlpha: State<RippleAlpha>
    ): RippleIndicationInstance {
        return remember(interactionSource, this) {
            CommonRippleIndicationInstance(bounded, radius, color, rippleAlpha)
        }
    }
}

internal class CommonRippleIndicationInstance constructor(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: State<Color>,
    private val rippleAlpha: State<RippleAlpha>
) : RippleIndicationInstance(bounded, rippleAlpha), RememberObserver {
    private val ripples = mutableStateMapOf<PressInteraction.Press, RippleAnimation>()

    override fun ContentDrawScope.drawIndication() {
        val color = color.value
        drawContent()
        drawStateLayer(radius, color)
        drawRipples(color)
    }

    override fun addRipple(interaction: PressInteraction.Press, scope: CoroutineScope) {
        // Finish existing ripples
        ripples.forEach { (_, ripple) -> ripple.finish() }
        val origin = if (bounded) interaction.pressPosition else null
        val rippleAnimation = RippleAnimation(
            origin = origin,
            radius = radius,
            bounded = bounded
        )
        ripples[interaction] = rippleAnimation
        scope.launch {
            try {
                rippleAnimation.animate()
            } finally {
                ripples.remove(interaction)
            }
        }
    }

    override fun removeRipple(interaction: PressInteraction.Press) {
        ripples[interaction]?.finish()
    }

    private fun DrawScope.drawRipples(color: Color) {
        ripples.forEach { (_, ripple) ->
            with(ripple) {
                val alpha = rippleAlpha.value.pressedAlpha
                if (alpha != 0f) {
                    draw(color.copy(alpha = alpha))
                }
            }
        }
    }

    override fun onRemembered() {}

    override fun onForgotten() {
        ripples.clear()
    }

    override fun onAbandoned() {
        ripples.clear()
    }
}
