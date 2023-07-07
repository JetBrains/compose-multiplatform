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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
fun SimpleFlowColumn() {
    FlowColumn(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
            .requiredHeight(200.dp)
            .border(BorderStroke(2.dp, Color.Gray)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        maxItemsInEachColumn = 3
    ) {
        repeat(9) { index ->
            Box(
                Modifier
                    .padding(10.dp)
                    .width(50.dp)
                    .height(50.dp)
                    .background(color = Color.Green)
            ) {
                Text(text = index.toString(), fontSize = 18.sp, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowColumnWithWeights() {
    FlowColumn(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
            .requiredHeight(200.dp)
            .border(BorderStroke(2.dp, Color.Gray)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        maxItemsInEachColumn = 3
    ) {
        repeat(10) { index ->
            Box(
                Modifier
                    .padding(10.dp)
                    .width(50.dp)
                    .height(50.dp)
                    .weight(if (index % 3 == 0) 1f else 2f, fill = true)
                    .background(color = Color.Green)
            )
        }
    }
}