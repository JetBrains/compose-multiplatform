/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowRow() {
    Column() {
        FlowRow(
            Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight(align = Alignment.Top)
                .border(BorderStroke(2.dp, Color.Gray)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            maxItemsInEachRow = 3
        ) {
            repeat(10) {
                Box(
                    Modifier
                        .padding(10.dp)
                        .width(50.dp)
                        .height(50.dp)
                        .background(Color.Green)
                ) {
                    Text(text = it.toString(), fontSize = 18.sp, modifier = Modifier.padding(3.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowRowWithWeights() {
    Column() {
        FlowRow(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            maxItemsInEachRow = 3
        ) {
            repeat(6) { index ->
                Box(
                    Modifier
                        .padding(10.dp)
                        .width(50.dp)
                        .height(50.dp)
                        .background(Color.Green)
                        .weight(if (index % 3 == 0) 1f else 2f, fill = true)
                )
            }
        }
    }
}