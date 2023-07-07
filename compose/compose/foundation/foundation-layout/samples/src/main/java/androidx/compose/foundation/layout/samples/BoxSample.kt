/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun SimpleBox() {
    Box {
        Box(Modifier.fillMaxSize().background(Color.Cyan))
        Box(
            Modifier.matchParentSize()
                .padding(top = 20.dp, bottom = 20.dp)
                .background(Color.Yellow)
        )
        Box(
            Modifier.matchParentSize()
                .padding(40.dp)
                .background(Color.Magenta)
        )
        Box(
            Modifier.align(Alignment.Center)
                .size(300.dp, 300.dp)
                .background(Color.Green)
        )
        Box(
            Modifier.align(Alignment.TopStart)
                .size(150.dp, 150.dp)
                .background(Color.Red)
        )
        Box(
            Modifier.align(Alignment.BottomEnd)
                .size(150.dp, 150.dp)
                .background(Color.Blue)
        )
    }
}