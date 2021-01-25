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

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.pressIndicatorGestureFilter

/**
 * Required for the press [InteractionState] consistency for TextField.
 */
@Suppress("ModifierInspectorInfo", "DEPRECATION")
internal fun Modifier.pressGestureFilter(
    interactionState: InteractionState?,
    enabled: Boolean = true
): Modifier = if (enabled) composed {
    DisposableEffect(interactionState) {
        onDispose {
            interactionState?.removeInteraction(Interaction.Pressed)
        }
    }
    pressIndicatorGestureFilter(
        onStart = {
            interactionState?.addInteraction(Interaction.Pressed, it)
        },
        onStop = {
            interactionState?.removeInteraction(Interaction.Pressed)
        },
        onCancel = {
            interactionState?.removeInteraction(Interaction.Pressed)
        }
    )
} else this
