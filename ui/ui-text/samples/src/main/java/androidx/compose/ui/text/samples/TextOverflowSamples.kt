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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun TextOverflowClipSample() {
    Text(
        text = "Hello ".repeat(2),
        modifier = Modifier.size(100.dp, 70.dp).background(Color.Cyan),
        fontSize = 35.sp,
        overflow = TextOverflow.Clip
    )
}

@Sampled
@Composable
fun TextOverflowEllipsisSample() {
    Text(
        text = "Hello ".repeat(2),
        modifier = Modifier.width(100.dp).background(Color.Cyan),
        fontSize = 35.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@Sampled
@Composable
fun TextOverflowVisibleFixedSizeSample() {
    val background = remember { mutableStateOf(Color.Cyan) }
    Box(modifier = Modifier.size(100.dp, 100.dp)) {
        Text(
            text = "Hello ".repeat(2),
            modifier = Modifier.size(100.dp, 70.dp)
                .background(background.value)
                .clickable {
                    background.value = if (background.value == Color.Cyan) {
                        Color.Gray
                    } else {
                        Color.Cyan
                    }
                },
            fontSize = 35.sp,
            overflow = TextOverflow.Visible
        )
    }
}

@Sampled
@Composable
fun TextOverflowVisibleMinHeightSample() {
    val background = remember { mutableStateOf(Color.Cyan) }
    val count = remember { mutableStateOf(1) }
    Box(modifier = Modifier.size(100.dp, 100.dp)) {
        Text(
            text = "Hello".repeat(count.value),
            modifier = Modifier.width(100.dp).heightIn(min = 70.dp)
                .background(background.value)
                .clickable {
                    background.value =
                        if (background.value == Color.Cyan) Color.Gray else Color.Cyan
                    count.value = if (count.value == 1) 2 else 1
                },
            fontSize = 35.sp,
            overflow = TextOverflow.Visible
        )
    }
}
