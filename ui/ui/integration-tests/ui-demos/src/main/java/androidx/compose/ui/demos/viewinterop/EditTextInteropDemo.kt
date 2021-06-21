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

package androidx.compose.ui.demos.viewinterop

import android.annotation.SuppressLint
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement.SpaceEvenly
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun EditTextInteropDemo() {
    Column {
        Row(horizontalArrangement = SpaceEvenly, verticalAlignment = CenterVertically) {
            Text("TextField in Compose:")
            val text = remember { mutableStateOf("") }
            TextField(text.value, onValueChange = { text.value = it })
        }
        Spacer(Modifier.requiredHeight(20.dp))
        Row(horizontalArrangement = SpaceEvenly, verticalAlignment = CenterVertically) {
            AndroidView({
                LinearLayout(it).apply {
                    this.orientation = LinearLayout.VERTICAL
                    addView(
                        LinearLayout(it).apply {
                            addView(
                                TextView(it).apply {
                                    @SuppressLint("SetTextI18n")
                                    text = "EditText within AndroidView:"
                                }
                            )
                            addView(EditText(it).apply { width = 500 })
                        }
                    )
                    addView(
                        LinearLayout(it).apply {
                            addView(
                                TextView(it).apply {
                                    @SuppressLint("SetTextI18n")
                                    text = "TextField within AndroidView:"
                                }
                            )
                            addView(
                                ComposeView(it).apply {
                                    setContent {
                                        val text = remember { mutableStateOf("") }
                                        TextField(text.value, onValueChange = { text.value = it })
                                    }
                                }
                            )
                        }
                    )
                }
            })
        }
    }
}
