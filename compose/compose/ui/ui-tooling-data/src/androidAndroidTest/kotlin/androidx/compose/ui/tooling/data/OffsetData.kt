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

package androidx.compose.ui.tooling.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WARNING: Any modification of this *requires* a change to OffsetInformationTest
 */

@Composable
fun MyComposeTheme(content: @Composable () -> Unit) {
    content()
}

@Composable
fun OffsetData() {
    MyComposeTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Hello",
                style = TextStyle(
                    color = Color.Magenta,
                    background = Color.White,
                    fontSize = 30.sp
                )
            )
            Greeting("Android")
            Surface(
                color = Color.Blue,
                shape = MaterialTheme.shapes.small
            ) {
                Button(onClick = {}) { Text("OK") }
            }
            Surface(
                color = Color.Red,
                shape = MaterialTheme.shapes.small
            ) {
                TextButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}