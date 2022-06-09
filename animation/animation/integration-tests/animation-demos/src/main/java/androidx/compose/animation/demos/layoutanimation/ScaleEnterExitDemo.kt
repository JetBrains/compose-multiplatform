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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceEvenly
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScaleEnterExitDemo() {
    Column {
        var showRed by remember { mutableStateOf(true) }
        var showYellow by remember { mutableStateOf(true) }
        var showGreen by remember { mutableStateOf(true) }
        var showBlue by remember { mutableStateOf(true) }

        AnimatedVisibility(
            visible = showRed,
            // Scale up from the TopLeft by setting TransformOrigin to (0f, 0f), while expanding the
            // layout size from Top start and fading. This will create a coherent look as if the
            // scale is impacting the size.
            enter = scaleIn(transformOrigin = TransformOrigin(0f, 0f)) + fadeIn() +
                expandIn(expandFrom = Alignment.TopStart),
            // Scale down from the TopLeft by setting TransformOrigin to (0f, 0f), while shrinking
            // the layout towards Top start and fading. This will create a coherent look as if the
            // scale is impacting the layout size.
            exit = scaleOut(transformOrigin = TransformOrigin(0f, 0f)) + fadeOut() +
                shrinkOut(shrinkTowards = Alignment.TopStart)
        ) {
            Box(Modifier.size(100.dp).background(Color.Red, shape = RoundedCornerShape(20.dp)))
        }
        AnimatedVisibility(
            visible = showYellow,
            enter = scaleIn(transformOrigin = TransformOrigin(0f, 0f)),
            exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f))
        ) {
            Box(Modifier.size(100.dp).background(Color.Yellow, shape = RoundedCornerShape(20.dp)))
        }
        AnimatedVisibility(
            visible = showGreen,
            // By Default, `scaleIn` uses the center as the pivot point. When used with a vertical
            // expansion from the vertical center, the content will be growing from the center of
            // the vertically expanding layout.
            enter = scaleIn() + expandVertically(expandFrom = Alignment.CenterVertically),
            // By Default, `scaleOut` uses the center as the pivot point. When used with an
            // ExitTransition that shrinks towards the center, the content will be shrinking both in
            // terms of scale and layout size towards the center.
            exit = scaleOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
        ) {
            Box(Modifier.size(100.dp).background(Color.Green, shape = RoundedCornerShape(20.dp)))
        }
        AnimatedVisibility(
            visible = showBlue,
            enter = scaleIn(initialScale = 1.2f) +
                slideInHorizontally(initialOffsetX = { (-it * 1.2f).toInt() }),
            exit = scaleOut(targetScale = 2f) +
                slideOutHorizontally(targetOffsetX = { -2 * it })
        ) {
            Box(Modifier.size(100.dp).background(Color.Blue, shape = RoundedCornerShape(20.dp)))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = SpaceEvenly) {
            Column(Modifier.clickable { showRed = !showRed }.padding(5.dp)) {
                Checkbox(showRed, { showRed = !showRed })
                Text("Red\n (Scale \n + Expand/Shrink \n TopStart)")
            }
            Column(Modifier.clickable { showYellow = !showYellow }.padding(5.dp)) {
                Checkbox(showYellow, { showYellow = !showYellow })
                Text("Yellow\n (Scale)")
            }
            Column(Modifier.clickable { showGreen = !showGreen }.padding(5.dp)) {
                Checkbox(showGreen, { showGreen = !showGreen })
                Text("Green\n (Scale \n + Expand/Shrink \n CenterVertical)")
            }
            Column(Modifier.clickable { showBlue = !showBlue }.padding(5.dp)) {
                Checkbox(showBlue, { showBlue = !showBlue })
                Text("Blue\n (Scale \n + Slide)")
            }
        }
    }
}