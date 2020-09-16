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

package androidx.compose.foundation.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.samples.SimpleCircleBox
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxDemo() {
    Column(Modifier.padding(10.dp)) {
        SimpleCircleBox()
        Spacer(Modifier.preferredHeight(30.dp))
        Box(
            modifier = Modifier.preferredSize(200.dp, 100.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(5.dp, Color.Gray),
            paddingStart = 20.dp,
            backgroundColor = Color.White
        ) {
            Box(
                modifier = Modifier.padding(10.dp).fillMaxSize(),
                backgroundColor = Color.DarkGray,
                shape = CutCornerShape(10.dp),
                border = BorderStroke(10.dp, Color.LightGray),
                gravity = ContentGravity.Center
            ) {
                Text("Nested boxes")
            }
        }
    }
}
