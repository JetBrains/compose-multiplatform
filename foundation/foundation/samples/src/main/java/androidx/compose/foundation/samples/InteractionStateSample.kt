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
import androidx.compose.foundation.Box
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.currentTextStyle
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun InteractionStateSample() {
    val interactionState = remember { InteractionState() }

    val draggable = Modifier.draggable(
        orientation = Orientation.Horizontal,
        interactionState = interactionState
    ) { /* update some business state here */ }

    // Use InteractionState to determine how this component should appear during transient UI states
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
                .then(draggable),
            border = BorderStroke(3.dp, color)
        ) {
            Text(
                text, style = currentTextStyle().copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxSize().wrapContentSize()
            )
        }
    }
}