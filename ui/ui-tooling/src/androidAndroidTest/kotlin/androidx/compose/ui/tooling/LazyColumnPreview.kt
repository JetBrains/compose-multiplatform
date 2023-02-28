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

package androidx.compose.ui.tooling

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text

@Preview
@Composable
fun SimpleLazyComposablePreview() {
    Surface(color = Color.Red) {
        LazyColumn {
            repeat(3) {
                item {
                    Text("Hello world")
                }
            }
        }
    }
}

@Preview
@Composable
fun ComplexLazyComposablePreview() {
    Surface(color = Color.Red) {
        LazyColumn() {
            repeat(1) {
                item {
                    Text("Hello world")
                    LazyRow {
                        repeat(2) {
                            item {
                                Button(onClick = {}) {
                                    Text("H$it")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SimpleTestComposablePreview() {
    Surface(color = Color.Red) {
            repeat(3) {
                    Text("Hello world")
            }
    }
}