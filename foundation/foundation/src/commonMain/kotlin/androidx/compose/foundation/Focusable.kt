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

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

/**
 * Configure component to be focusable via focus system or accessibility "focus" event.
 *
 * Add this modifier to the element to make it focusable within its bounds.
 *
 * @sample androidx.compose.foundation.samples.FocusableSample
 *
 * @param enabled Controls the enabled state. When `false`, element won't participate in the focus
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [FocusInteraction.Focus] when this element is being focused.
 */
fun Modifier.focusable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "focusable"
        properties["enabled"] = enabled
        properties["interactionSource"] = interactionSource
    }
) {
    val scope = rememberCoroutineScope()
    val focusedInteraction = remember { mutableStateOf<FocusInteraction.Focus?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    @OptIn(ExperimentalFoundationApi::class)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    DisposableEffect(interactionSource) {
        onDispose {
            focusedInteraction.value?.let { oldValue ->
                val interaction = FocusInteraction.Unfocus(oldValue)
                interactionSource?.tryEmit(interaction)
                focusedInteraction.value = null
            }
        }
    }
    DisposableEffect(enabled) {
        if (!enabled) {
            scope.launch {
                focusedInteraction.value?.let { oldValue ->
                    val interaction = FocusInteraction.Unfocus(oldValue)
                    interactionSource?.emit(interaction)
                    focusedInteraction.value = null
                }
            }
        }
        onDispose { }
    }

    @OptIn(ExperimentalFoundationApi::class)
    if (enabled) {
        Modifier
            .semantics {
                this.focused = isFocused
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (isFocused) {
                    scope.launch {
                        focusedInteraction.value?.let { oldValue ->
                            val interaction = FocusInteraction.Unfocus(oldValue)
                            interactionSource?.emit(interaction)
                            focusedInteraction.value = null
                        }
                        val interaction = FocusInteraction.Focus()
                        interactionSource?.emit(interaction)
                        focusedInteraction.value = interaction
                        bringIntoViewRequester.bringIntoView()
                    }
                } else {
                    scope.launch {
                        focusedInteraction.value?.let { oldValue ->
                            val interaction = FocusInteraction.Unfocus(oldValue)
                            interactionSource?.emit(interaction)
                            focusedInteraction.value = null
                        }
                    }
                }
            }
            .focusTarget()
    } else {
        Modifier
    }
}

// TODO: b/202856230 - consider either making this / a similar API public, or add a parameter to
//  focusable to configure this behavior.
/**
 * [focusable] but only when not in touch mode - when [LocalInputModeManager] is
 * not [InputMode.Touch]
 */
internal fun Modifier.focusableInNonTouchMode(
    enabled: Boolean,
    interactionSource: MutableInteractionSource?
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "focusableInNonTouchMode"
        properties["enabled"] = enabled
        properties["interactionSource"] = interactionSource
    }
) {
    val inputModeManager = LocalInputModeManager.current
    Modifier
        .focusProperties { canFocus = inputModeManager.inputMode != InputMode.Touch }
        .focusable(enabled, interactionSource)
}
