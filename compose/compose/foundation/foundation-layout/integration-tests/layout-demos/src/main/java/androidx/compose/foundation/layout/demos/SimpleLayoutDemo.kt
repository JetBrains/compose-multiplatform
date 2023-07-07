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

package androidx.compose.foundation.layout.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimpleLayoutDemo() {
    val lightGrey = Color(0xFFCFD8DC)
    Column {
        Text(text = "Row", fontSize = 48.sp)

        Box(Modifier.width(ExampleSize).background(color = lightGrey)) {
            Row(Modifier.fillMaxWidth()) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(ExampleSize).background(color = lightGrey)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(ExampleSize).background(color = lightGrey)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(ExampleSize).background(color = lightGrey)) {
            Row(Modifier.fillMaxWidth()) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(ExampleSize).background(color = lightGrey)) {
            Row(Modifier.fillMaxWidth()) {
                PurpleSquare(Modifier.align(Alignment.Bottom))
                CyanSquare(Modifier.align(Alignment.Bottom))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(text = "Column", fontSize = 48.sp)
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.height(ExampleSize).background(color = lightGrey)) {
                Column(Modifier.fillMaxHeight()) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.width(24.dp))
            Box(Modifier.height(ExampleSize).background(color = lightGrey)) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.width(24.dp))
            Box(Modifier.height(ExampleSize).background(color = lightGrey)) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.width(24.dp))
            Box(Modifier.height(ExampleSize).background(color = lightGrey)) {
                Column(Modifier.fillMaxHeight()) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.width(24.dp))
            Box(Modifier.height(ExampleSize).background(color = lightGrey)) {
                Column(Modifier.fillMaxHeight()) {
                    PurpleSquare(Modifier.align(Alignment.End))
                    CyanSquare(Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@Composable
private fun PurpleSquare(modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).background(Color(0xFF6200EE)))
}

@Composable
private fun CyanSquare(modifier: Modifier = Modifier) {
    Box(modifier.size(24.dp).background(Color(0xFF03DAC6)))
}

private val ExampleSize = 140.dp