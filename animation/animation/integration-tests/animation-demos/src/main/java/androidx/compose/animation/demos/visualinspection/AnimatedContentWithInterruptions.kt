/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.animation.demos.visualinspection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedContentWithInterruptions() {
    var count by remember { mutableStateOf(0) }
    AnimatedContent(targetState = count, transitionSpec = {
        if (targetState == 0) {
            (slideInVertically { it } with fadeOut(targetAlpha = 0.88f))
                .apply { targetContentZIndex = 1f }
        } else {
            (fadeIn(initialAlpha = 0.88f) with slideOutVertically { it } +
                fadeOut(targetAlpha = 0.88f))
                .apply { targetContentZIndex = -1f }
        }
    }) { state ->
        if (state == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Green)
            )
        } else if (state == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
            )
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(100, 500))
            count = 1 - count
        }
    }

    Row {
        Text("Red",
            Modifier
                .clickable { if (count < 1) count++ }
                .padding(50.dp))
        Spacer(Modifier.width(60.dp))
        Text("Green",
            Modifier
                .clickable { if (count > 0) count-- }
                .padding(50.dp))
    }
}