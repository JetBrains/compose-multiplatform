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

package androidx.compose.material3.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
fun ShapeDemo() {
    val shapes = MaterialTheme.shapes
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Button(shape = RectangleShape, onClick = {}) { Text("None") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(shape = shapes.extraSmall, onClick = {}) { Text("Extra  Small") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(shape = shapes.small, onClick = {}) { Text("Small") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(shape = shapes.medium, onClick = {}) { Text("Medium") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(shape = shapes.large, onClick = {}) { Text("Large") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(shape = shapes.extraLarge, onClick = {}) { Text("Extra Large") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(shape = CircleShape, onClick = {}) { Text("Full") }
    }
}
