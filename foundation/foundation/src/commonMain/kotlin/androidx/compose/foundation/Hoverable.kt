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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Configure component to be hoverable via pointer enter/exit events.
 *
 * @sample androidx.compose.foundation.samples.HoverableSample
 *
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [HoverInteraction.Enter] when this element is being hovered.
 * @param enabled Controls the enabled state. When `false`, hover events will be ignored.
 */
fun Modifier.hoverable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "hoverable"
        properties["interactionSource"] = interactionSource
        properties["enabled"] = enabled
    }
) {
    var hoverInteraction by remember { mutableStateOf<HoverInteraction.Enter?>(null) }

    suspend fun emitEnter() {
        if (hoverInteraction == null) {
            val interaction = HoverInteraction.Enter()
            interactionSource.emit(interaction)
            hoverInteraction = interaction
        }
    }

    suspend fun emitExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource.emit(interaction)
            hoverInteraction = null
        }
    }

    fun tryEmitExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource.tryEmit(interaction)
            hoverInteraction = null
        }
    }

    DisposableEffect(interactionSource) {
        onDispose { tryEmitExit() }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            emitExit()
        }
    }

    if (enabled) {
        Modifier
            .pointerInput(interactionSource) {
                coroutineScope {
                    val currentContext = currentCoroutineContext()
                    val outerScope = this
                    awaitPointerEventScope {
                        while (currentContext.isActive) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> outerScope.launch(
                                    start = CoroutineStart.UNDISPATCHED
                                ) {
                                    emitEnter()
                                }
                                PointerEventType.Exit -> outerScope.launch(
                                    start = CoroutineStart.UNDISPATCHED
                                ) {
                                    emitExit()
                                }
                            }
                        }
                    }
                }
            }
    } else {
        Modifier
    }
}
