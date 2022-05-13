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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER")

package androidx.compose.integration.docs.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.integration.docs.input.DynamicButton.PressIconButton
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/handling-interaction
 *
 * No action required if it's modified.
 */

@Composable
private fun UseInteractionSoure() {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = { /* do something */ },
        interactionSource = interactionSource) {
        Text(if (isPressed) "Pressed!" else "Not pressed")
    }
}

@Composable
private fun InteractionSourceBuildList() {
    val interactionSource = remember { MutableInteractionSource() }
    val interactions = remember { mutableStateListOf<Interaction>() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    interactions.add(interaction)
                }
                is DragInteraction.Start -> {
                    interactions.add(interaction)
                }
            }
        }
    }
}

@Composable
private fun InteractionSourcePruneList() {
    val interactionSource = remember { MutableInteractionSource() }

    // snippet 1:

    val interactions = remember { mutableStateListOf<Interaction>() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    interactions.add(interaction)
                }
                is PressInteraction.Release -> {
                    interactions.remove(interaction.press)
                }
                is PressInteraction.Cancel -> {
                    interactions.remove(interaction.press)
                }
                is DragInteraction.Start -> {
                    interactions.add(interaction)
                }
                is DragInteraction.Stop -> {
                    interactions.remove(interaction.start)
                }
                is DragInteraction.Cancel -> {
                    interactions.add(interaction.start)
                }
            }
        }
    }

    // snippet 2:
    val isPressedOrDragged = interactions.isNotEmpty()

    // snippet 3:
    val lastInteraction = when (interactions.lastOrNull()) {
        is DragInteraction.Start -> "Dragged"
        is PressInteraction.Press -> "Pressed"
        else -> "No state"
    }
}

private object DynamicButton {
    @Composable
    fun PressIconButton(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        text: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource =
            remember { MutableInteractionSource() },
    ) {
        val isPressed by interactionSource.collectIsPressedAsState()
        Button(onClick = onClick, modifier = modifier,
            interactionSource = interactionSource) {
            AnimatedVisibility(visible = isPressed) {
                if (isPressed) {
                    Row {
                        icon()
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    }
                }
            }
            text()
        }
    }
}

@Composable
private fun UseDynamicButton() {
    PressIconButton(
        onClick = {},
        icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
        text = { Text("Add to cart") }
    )
}

/*
Fakes needed for snippets to build:
 */

// none yet
