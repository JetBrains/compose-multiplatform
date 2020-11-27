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
import androidx.compose.runtime.CompositionLifecycleObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Generic interface to define visual effects when certain interaction happens. Examples might
 * be showing some press indication, such as material ripples or define custom decoration when
 * item is dragged.
 *
 * This interface is factory-like and required to produce [IndicationInstance] on demand for
 * [indication] modifier.
 *
 * If you want to override default behaviour for [indication] for the whole subtree, consider
 * creating object of this factory and providing it in [AmbientIndication].
 */
@Stable
interface Indication {

    /**
     * Function to create new [IndicationInstance] on demand. Typically this will be called by
     * [indication] modified to spawn new instances when added to modified element.
     */
    fun createInstance(): IndicationInstance
}

/**
 * Generic interface to define the instance if the [Indication] to draw visual effects when certain
 * interaction happens.
 *
 * Indication can be stateful or stateless, and they expected to be created  by [Indication] and
 * used in-place and not reused between different [indication] modifiers.
 */
interface IndicationInstance {

    /**
     * Method to draw visual effects based on [InteractionState].
     *
     * Usually, in this method indication reads [InteractionState] to observe its value and draw
     * any visuals to reflect this state. Refer to the [Interaction] to see what states are
     * possible and draw visual effects when [InteractionState] contains them.
     *
     * This method MUST call [ContentDrawScope.drawContent] at some point in order to draw the
     * rest of the UI tree below indication.
     *
     * @param interactionState state of the parent of this indication
     */
    fun ContentDrawScope.drawIndication(interactionState: InteractionState)

    /**
     * Callback which is called when this [IndicationInstance] disappears
     * from composition and should free any allocated resources / stop on-going animations / etc
     */
    fun onDispose() {}
}

/**
 * Show visual indicator for an [InteractionState].
 *
 * @sample androidx.compose.foundation.samples.IndicationSample
 *
 * @param interactionState state for indication to indicate against. This state is updates by
 * modifier such as [clickable].
 * @param indication indication to be drawn. If `null`, there will be no indication shown
 */
fun Modifier.indication(
    interactionState: InteractionState,
    indication: Indication? = null
) = composed(
    factory = {
        val resolvedIndication = indication ?: NoIndication
        remember(interactionState, resolvedIndication) {
            IndicationModifier(interactionState, resolvedIndication.createInstance())
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "indication"
        properties["indication"] = indication
        properties["interactionState"] = interactionState
    }
)

/**
 * Ambient to provide [IndicationInstance] to draw visual indication for press and other events.
 *
 * By default there will be [DefaultDebugIndication] created.
 */
// TODO : temporary made it to be lambda, fix when b/157150564 is fixed
val AmbientIndication = staticAmbientOf<@Composable () -> Indication> { { DefaultDebugIndication } }

private object NoIndication : Indication {
    private object NoIndicationInstance : IndicationInstance {
        override fun ContentDrawScope.drawIndication(interactionState: InteractionState) {
            drawContent()
        }
    }

    override fun createInstance(): IndicationInstance = NoIndicationInstance
}

/**
 * Simple default [Indication] that show visual effect when tap occurs.
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

    override fun createInstance(): IndicationInstance {
        return DefaultDebugIndicationInstance
    }
}

private class IndicationModifier(
    val interactionState: InteractionState,
    val indicationInstance: IndicationInstance
) : CompositionLifecycleObserver, DrawModifier {

    override fun ContentDrawScope.draw() {
        with(indicationInstance) {
            drawIndication(interactionState)
        }
    }

    override fun onEnter() {}

    override fun onLeave() {
        indicationInstance.onDispose()
    }
}