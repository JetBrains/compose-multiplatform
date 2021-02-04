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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
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
 * example: showing a ripple effect when a component is touched, or a highlight when a component
 * is focused.
 *
 * An instance of Indication is a factory that is required to produce [IndicationInstance]s on
 * demand for each component that uses an [indication] modifier using [createInstance].
 *
 * Indication is typically provided throughout the hierarchy through [LocalIndication] - you can
 * provide a custom Indication to [LocalIndication] to change the default [Indication] used for
 * components such as [clickable].
 */
@Stable
interface Indication {

    /**
     * Creates a new [IndicationInstance]. Typically this will be called by [indication],
     * so one [IndicationInstance] will be used for one component that draws [Indication],
     * such as a button.
     */
    @Composable
    fun createInstance(): IndicationInstance
}

/**
 * IndicationInstance is a specific instance of an [Indication] that draws visual effects on
 * certain interactions, such as press or focus.
 *
 * IndicationInstances can be stateful or stateless, and are created by
 * [Indication.createInstance] - they should be used in-place and not re-used between different
 * [indication] modifiers.
 */
interface IndicationInstance {

    /**
     * Draws visual effects based on [InteractionState].
     *
     * Usually, in this method indication reads [InteractionState] to observe its value and draw
     * any visuals to reflect this state. Refer to the [Interaction] to see what states are
     * possible and draw visual effects when [InteractionState] contains them.
     *
     * This method MUST call [ContentDrawScope.drawContent] at some point in order to draw the
     * component itself underneath any indication. Typically this is called at the beginning, so
     * that indication can be drawn as an overlay on top.
     *
     * @param interactionState [InteractionState] representing the combined state of interactions
     * occurring on the component the indication is drawn for.
     */
    fun ContentDrawScope.drawIndication(interactionState: InteractionState)

    /**
     * Callback which is invoked when this [IndicationInstance] is removed from composition. Use
     * this callback to free up any allocated resources, stop on-going animations, and other
     * related cleanup.
     */
    fun onDispose() {}
}

/**
 * Draws visual effects for this component when interactions occur.
 *
 * @sample androidx.compose.foundation.samples.IndicationSample
 *
 * @param interactionState [InteractionState] that will be used by [indication] to draw visual
 * effects - this [InteractionState] represents the combined state of all interactions currently
 * present on this component.
 * @param indication [Indication] used to draw visual effects. If `null`, no visual effects will
 * be shown for this component.
 */
fun Modifier.indication(
    interactionState: InteractionState,
    indication: Indication? = null
) = composed(
    factory = {
        val resolvedIndication = indication ?: NoIndication
        val instance = resolvedIndication.createInstance()
        remember(interactionState, instance) {
            IndicationModifier(interactionState, instance)
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "indication"
        properties["indication"] = indication
        properties["interactionState"] = interactionState
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
        override fun ContentDrawScope.drawIndication(interactionState: InteractionState) {
            drawContent()
        }
    }

    @Composable
    override fun createInstance(): IndicationInstance = NoIndicationInstance
}

/**
 * Simple default [Indication] that draws a rectangular overlay when pressed.
 */
private object DefaultDebugIndication : Indication {

    private object DefaultDebugIndicationInstance : IndicationInstance {
        override fun ContentDrawScope.drawIndication(interactionState: InteractionState) {
            drawContent()
            if (interactionState.contains(Interaction.Pressed)) {
                drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
            }
        }
    }

    @Composable
    override fun createInstance(): IndicationInstance {
        return DefaultDebugIndicationInstance
    }
}

private class IndicationModifier(
    val interactionState: InteractionState,
    val indicationInstance: IndicationInstance
) : RememberObserver, DrawModifier {

    override fun ContentDrawScope.draw() {
        with(indicationInstance) {
            drawIndication(interactionState)
        }
    }

    override fun onRemembered() { }

    override fun onForgotten() {
        indicationInstance.onDispose()
    }

    override fun onAbandoned() {
        indicationInstance.onDispose()
    }
}