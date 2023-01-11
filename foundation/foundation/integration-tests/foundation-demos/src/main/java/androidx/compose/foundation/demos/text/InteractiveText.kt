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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveTextDemo() {
    val clickedOffset = remember { mutableStateOf<Int?>(null) }
    val hoveredOffset = remember { mutableStateOf<Int?>(null) }
    val numOnHoverInvocations = remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        Text(text = "ClickableText onHover", style = MaterialTheme.typography.h6)

        Text(text = "Click/Hover the lorem ipsum text below.")
        Text(text = "Clicked offset: ${clickedOffset.value ?: "No click yet"}")
        Text(text = "Hovered offset: ${hoveredOffset.value ?: "Not hovering"}")
        Text(text = "Number of onHover invocations: ${numOnHoverInvocations.value}")

        ClickableText(
            text = AnnotatedString(loremIpsum(wordCount = 30)),
            modifier = Modifier.border(Dp.Hairline, Color.Black),
            style = MaterialTheme.typography.body1,
            onHover = {
                numOnHoverInvocations.value = numOnHoverInvocations.value + 1
                hoveredOffset.value = it
            }
        ) { offset ->
            clickedOffset.value = offset
        }

        Button(
            onClick = {
                clickedOffset.value = null
                hoveredOffset.value = null
                numOnHoverInvocations.value = 0
            }
        ) {
            Text(text = "Reset Offsets/Counter")
        }
    }
}
