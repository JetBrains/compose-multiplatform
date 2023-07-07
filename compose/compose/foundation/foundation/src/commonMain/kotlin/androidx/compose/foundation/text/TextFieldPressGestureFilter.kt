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

package androidx.compose.foundation.text

import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Required for the press and tap [MutableInteractionSource] consistency for TextField.
 */
@Suppress("ModifierInspectorInfo")
internal fun Modifier.tapPressTextFieldModifier(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean = true,
    onTap: (Offset) -> Unit
): Modifier = if (enabled) composed {
    val scope = rememberCoroutineScope()
    val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
    val onTapState = rememberUpdatedState(onTap)
    DisposableEffect(interactionSource) {
        onDispose {
            pressedInteraction.value?.let { oldValue ->
                val interaction = PressInteraction.Cancel(oldValue)
                interactionSource?.tryEmit(interaction)
                pressedInteraction.value = null
            }
        }
    }
    Modifier.pointerInput(interactionSource) {
        detectTapAndPress(
            onPress = {
                scope.launch {
                    // Remove any old interactions if we didn't fire stop / cancel properly
                    pressedInteraction.value?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource?.emit(interaction)
                        pressedInteraction.value = null
                    }
                    val interaction = PressInteraction.Press(it)
                    interactionSource?.emit(interaction)
                    pressedInteraction.value = interaction
                }
                val success = tryAwaitRelease()
                scope.launch {
                    pressedInteraction.value?.let { oldValue ->
                        val interaction =
                            if (success) {
                                PressInteraction.Release(oldValue)
                            } else {
                                PressInteraction.Cancel(oldValue)
                            }
                        interactionSource?.emit(interaction)
                        pressedInteraction.value = null
                    }
                }
            },
            onTap = { onTapState.value.invoke(it) }
        )
    }
} else this
