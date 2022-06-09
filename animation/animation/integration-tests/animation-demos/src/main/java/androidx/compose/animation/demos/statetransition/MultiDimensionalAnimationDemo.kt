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

package androidx.compose.animation.demos.statetransition

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateRect
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun MultiDimensionalAnimationDemo() {
    var currentState by remember { mutableStateOf(AnimState.Collapsed) }
    val onClick = {
        // Cycle through states when clicked.
        currentState = when (currentState) {
            AnimState.Collapsed -> AnimState.Expanded
            AnimState.Expanded -> AnimState.PutAway
            AnimState.PutAway -> AnimState.Collapsed
        }
    }

    var width by remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }
    val transition = updateTransition(currentState)
    val rect by transition.animateRect(transitionSpec = { spring(stiffness = 100f) }) {
        when (it) {
            AnimState.Collapsed -> Rect(600f, 600f, 900f, 900f)
            AnimState.Expanded -> Rect(0f, 400f, width, height - 400f)
            AnimState.PutAway -> Rect(width - 300f, height - 300f, width, height)
        }
    }

    val color by transition.animateColor(transitionSpec = { tween(durationMillis = 500) }) {
        when (it) {
            AnimState.Collapsed -> Color.LightGray
            AnimState.Expanded -> Color(0xFFd0fff8)
            AnimState.PutAway -> Color(0xFFe3ffd9)
        }
    }
    Canvas(
        modifier = Modifier.fillMaxSize().clickable(
            onClick = onClick,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        width = size.width
        height = size.height

        drawRect(
            color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height)
        )
    }
}

private enum class AnimState {
    Collapsed,
    Expanded,
    PutAway
}