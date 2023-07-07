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
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

@Sampled
@Composable
fun SimpleInteractionSourceSample() {
    // Hoist the MutableInteractionSource that we will provide to interactions
    val interactionSource = remember { MutableInteractionSource() }

    // Provide the MutableInteractionSource instances to the interactions we want to observe state
    // changes for
    val draggable = Modifier.draggable(
        interactionSource = interactionSource,
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { /* update some business state here */ }
    )

    val clickable = Modifier.clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current
    ) { /* update some business state here */ }

    // Observe changes to the binary state for these interactions
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Use the state to change our UI
    val (text, color) = when {
        isDragged && isPressed -> "Dragged and pressed" to Color.Red
        isDragged -> "Dragged" to Color.Green
        isPressed -> "Pressed" to Color.Blue
        // Default / baseline state
        else -> "Drag me horizontally, or press me!" to Color.Black
    }

    Box(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
            .size(width = 240.dp, height = 80.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(clickable)
                .then(draggable)
                .border(BorderStroke(3.dp, color))
                .padding(3.dp)
        ) {
            Text(
                text, style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxSize().wrapContentSize()
            )
        }
    }
}

@Sampled
@Composable
fun InteractionSourceFlowSample() {
    // Hoist the MutableInteractionSource that we will provide to interactions
    val interactionSource = remember { MutableInteractionSource() }

    // Provide the MutableInteractionSource instances to the interactions we want to observe state
    // changes for
    val draggable = Modifier.draggable(
        interactionSource = interactionSource,
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { /* update some business state here */ }
    )

    val clickable = Modifier.clickable(
        interactionSource = interactionSource,
        // This component is a compound component where part of it is clickable and part of it is
        // draggable. As a result we want to show indication for the _whole_ component, and not
        // just for clickable area. We set `null` indication here and provide an explicit
        // Modifier.indication instance later that will draw indication for the whole component.
        indication = null
    ) { /* update some business state here */ }

    // SnapshotStateList we will use to track incoming Interactions in the order they are emitted
    val interactions = remember { mutableStateListOf<Interaction>() }

    // Collect Interactions - if they are new, add them to `interactions`. If they represent stop /
    // cancel events for existing Interactions, remove them from `interactions` so it will only
    // contain currently active `interactions`.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    // Display some text based on the most recent Interaction stored in `interactions`
    val text = when (interactions.lastOrNull()) {
        is DragInteraction.Start -> "Dragged"
        is PressInteraction.Press -> "Pressed"
        else -> "No state"
    }

    Column(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
    ) {
        Row(
            // Draw indication for the whole component, based on the Interactions dispatched by
            // our hoisted MutableInteractionSource
            Modifier.indication(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
        ) {
            Box(
                Modifier
                    .size(width = 240.dp, height = 80.dp)
                    .then(clickable)
                    .border(BorderStroke(3.dp, Color.Blue))
                    .padding(3.dp)
            ) {
                val pressed = interactions.any { it is PressInteraction.Press }
                Text(
                    text = if (pressed) "Pressed" else "Not pressed",
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxSize().wrapContentSize()
                )
            }
            Box(
                Modifier
                    .size(width = 240.dp, height = 80.dp)
                    .then(draggable)
                    .border(BorderStroke(3.dp, Color.Red))
                    .padding(3.dp)
            ) {
                val dragged = interactions.any { it is DragInteraction.Start }
                Text(
                    text = if (dragged) "Dragged" else "Not dragged",
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxSize().wrapContentSize()
                )
            }
        }
        Text(
            text = text,
            style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxSize().wrapContentSize()
        )
    }
}
