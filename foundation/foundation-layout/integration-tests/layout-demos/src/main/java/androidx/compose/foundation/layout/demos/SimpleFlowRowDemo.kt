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

package androidx.compose.foundation.layout.demos

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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleFlowRowDemo() {
    Column() {
        FlowRow(
            Modifier
                .wrapContentWidth(align = Alignment.Start)
                .wrapContentHeight(align = Alignment.Top)
                .requiredHeight(150.dp)
                .border(BorderStroke(2.dp, Color.Gray)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = Int.MAX_VALUE
        ) {
            repeat(50) {
                Text("Heldo")
            }
        }
        FlowRow(
            Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight(align = Alignment.Top)
                .border(BorderStroke(2.dp, Color.Gray)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            maxItemsInEachRow = 3
        ) {
            repeat(10) {
                Box(
                    Modifier
                        .padding(10.dp)
                        .width(50.dp)
                        .height(
                            if (it % 2 == 0) {
                                30.dp
                            } else {
                                50.dp
                            }
                        )
                        .background(Color(0xFF6200ED))
                        .weight(1f, false)
                ) {
                    Text(text = it.toString(), fontSize = 18.sp, modifier = Modifier.padding(3.dp))
                }
            }
        }

        FlowRow(
            Modifier
                .wrapContentHeight()
                .width(200.dp),
            horizontalArrangement = Arrangement.Start,
            maxItemsInEachRow = 5
        ) {
            repeat(6) { _ ->
                Box(
                    Modifier
                        .size(20.dp)
                )
            }
        }
    }
}