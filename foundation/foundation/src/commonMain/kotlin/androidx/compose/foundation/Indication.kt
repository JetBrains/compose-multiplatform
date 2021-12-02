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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Indication represents visual effects that occur when certain interactions happens. For
 * example: showing a ripple effect when a component is pressed, or a highlight when a component
 * is focused.
 *
 * An instance of Indication is a factory that is required to produce [IndicationInstance]s on
 * demand for each component that uses an [indication] modifier using [rememberUpdatedInstance].
 *
 * Indication is typically provided throughout the hierarchy through [LocalIndication] - you can
 * provide a custom Indication to [LocalIndication] to change the default [Indication] used for
 * components such as [clickable].
 */
@Stable
interface Indication {

    /**
     * [remember]s a new [IndicationInstance], and updates its state based on [Interaction]s
     * emitted via [interactionSource] . Typically this will be called by [indication],
     * so one [IndicationInstance] will be used for one component that draws [Indication], such
     * as a button.
     *
     * Implementations of this function should observe [Interaction]s using [interactionSource],
     * using them to launch animations / state changes inside [IndicationInstance] that will
     * then be reflected inside [IndicationInstance.drawIndication].
     *
     * @param interactionSource the [InteractionSource] representing the stream of
     * [Interaction]s the returned [IndicationInstance] should represent
     * @return an [IndicationInstance] that represents the stream of [Interaction]s emitted by
     * [interactionSource]
     */
    @Composable
    fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance
}

/**
 * IndicationInstance is a specific instance of an [Indication] that draws visual effects on
 * certain interactions, such as press or focus.
 *
 * IndicationInstances can be stateful or stateless, and are created by
 * [Indication.rememberUpdatedInstance] - they should be used in-place and not re-used between
 * different [indication] modifiers.
 */
interface IndicationInstance {

    /**
     * Draws visual effects for the current interactions present on this component.
     *
     * Typically this function will read state within this instance that is mutated by
     * [Indication.rememberUpdatedInstance]. This allows [IndicationInstance] to just read state
     * and draw visual effects, and not actually change any state itself.
     *
     * This method MUST call [ContentDrawScope.drawContent] at some point in order to draw the
     * component itself underneath any indication. Typically this is called at the beginning, so
     * that indication can be drawn as an overlay on top.
     */
    fun ContentDrawScope.drawIndication()
}

/**
 * Draws visual effects for this component when interactions occur.
 *
 * @sample androidx.compose.foundation.samples.IndicationSample
 *
 * @param interactionSource [InteractionSource] that will be used by [indication] to draw
 * visual effects - this [InteractionSource] represents the stream of [Interaction]s for this
 * component.
 * @param indication [Indication] used to draw visual effects. If `null`, no visual effects will
 * be shown for this component.
 */
fun Modifier.indication(
    interactionSource: InteractionSource,
    indication: Indication?
) = composed(
    factory = {
        val resolvedIndication = indication ?: NoIndication
        val instance = resolvedIndication.rememberUpdatedInstance(interactionSource)
        remember(instance) {
            IndicationModifier(instance)
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "indication"
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    }
)

/**
 * CompositionLocal that provides an [Indication] through the hierarchy. This [Indication] will
 * be used by default to draw visual effects for interactions such as press and drag in components
 * such as [clickable].
 *
 * By default this will provide [DefaultDebugIndication].
 */
val LocalIndication = staticCompositionLocalOf<Indication> {
    DefaultDebugIndication
}

private object NoIndication : Indication {
    private object NoIndicationInstance : IndicationInstance {
        override fun ContentDrawScope.drawIndication() {
            drawContent()
        }
    }

    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        return NoIndicationInstance
    }
}

/**
 * Simple default [Indication] that draws a rectangular overlay when pressed.
 */
private object DefaultDebugIndication : Indication {

    private class DefaultDebugIndicationInstance(
        private val isPressed: State<Boolean>,
        private val isHovered: State<Boolean>,
        private val isFocused: State<Boolean>,
    ) : IndicationInstance {
        override fun ContentDrawScope.drawIndication() {
            drawContent()
            if (isPressed.value) {
                drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
            } else if (isHovered.value || isFocused.value) {
                drawRect(color = Color.Black.copy(alpha = 0.1f), size = size)
            }
        }
    }

    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val isPressed = interactionSource.collectIsPressedAsState()
        val isHovered = interactionSource.collectIsHoveredAsState()
        val isFocused = interactionSource.collectIsFocusedAsState()
        return remember(interactionSource) {
            DefaultDebugIndicationInstance(isPressed, isHovered, isFocused)
        }
    }
}

private class IndicationModifier(
    val indicationInstance: IndicationInstance
) : DrawModifier {

    override fun ContentDrawScope.draw() {
        with(indicationInstance) {
            drawIndication()
        }
    }
}