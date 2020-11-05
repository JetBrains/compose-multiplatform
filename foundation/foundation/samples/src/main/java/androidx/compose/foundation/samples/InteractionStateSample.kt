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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun PriorityInteractionStateSample() {
    val interactionState = remember { InteractionState() }

    val draggable = Modifier.draggable(
        orientation = Orientation.Horizontal,
        interactionState = interactionState
    ) { /* update some business state here */ }

    // Use InteractionState to determine how this component should appear during transient UI states
    // In this example we are using a 'priority' system, such that we ignore multiple states, and
    // don't care about the most recent state - Dragged is more important than Pressed.
    val (text, color) = when {
        Interaction.Dragged in interactionState -> "Dragged" to Color.Red
        Interaction.Pressed in interactionState -> "Pressed" to Color.Blue
        // Default / baseline state
        else -> "Drag me horizontally, or press me!" to Color.Black
    }

    Box(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
            .preferredSize(width = 240.dp, height = 80.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionState = interactionState) { /* do nothing */ }
                .then(draggable)
                .border(BorderStroke(3.dp, color))
                .padding(3.dp)
        ) {
            Text(
                text, style = AmbientTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxSize().wrapContentSize()
            )
        }
    }
}

@Sampled
@Composable
fun MultipleInteractionStateSample() {
    val interactionState = remember { InteractionState() }

    val draggable = Modifier.draggable(
        orientation = Orientation.Horizontal,
        interactionState = interactionState
    ) { /* update some business state here */ }

    val clickable = Modifier.clickable(interactionState = interactionState) {
        /* update some business state here */
    }

    // In this example we have a complex component that can be in multiple states at the same time
    // (both pressed and dragged, since different areas of the same component can be pressed and
    // dragged at the same time), and we want to use only the most recent state to show the visual
    // state of the component. This could be with a visual overlay, or similar. Note that the most
    // recent state is the _last_ state added to interactionState, so we want to start from the end,
    // hence we use `lastOrNull` and not `firstOrNull`.
    val latestState = interactionState.value.lastOrNull {
        // We only care about pressed and dragged states here, so ignore everything else
        it is Interaction.Dragged || it is Interaction.Pressed
    }

    val text = when (latestState) {
        Interaction.Dragged -> "Dragged"
        Interaction.Pressed -> "Pressed"
        else -> "No state"
    }

    Column(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
    ) {
        Row {
            Box(
                Modifier
                    .preferredSize(width = 240.dp, height = 80.dp)
                    .then(clickable)
                    .border(BorderStroke(3.dp, Color.Blue))
                    .padding(3.dp)
            ) {
                val pressed = Interaction.Pressed in interactionState
                Text(
                    text = if (pressed) "Pressed" else "Not pressed",
                    style = AmbientTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxSize().wrapContentSize()
                )
            }
            Box(
                Modifier
                    .preferredSize(width = 240.dp, height = 80.dp)
                    .then(draggable)
                    .border(BorderStroke(3.dp, Color.Red))
                    .padding(3.dp)
            ) {
                val dragged = Interaction.Dragged in interactionState
                Text(
                    text = if (dragged) "Dragged" else "Not dragged",
                    style = AmbientTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxSize().wrapContentSize()
                )
            }
        }
        Text(
            text = text,
            style = AmbientTextStyle.current.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxSize().wrapContentSize()
        )
    }
}
