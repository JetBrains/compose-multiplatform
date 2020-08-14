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
package androidx.ui.desktop.examples.example1

import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.fontFamily
import androidx.compose.ui.text.platform.font
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.desktop.AppWindow

private const val title = "Desktop Compose Elements"

val italicFont = fontFamily(font("Noto Italic", "NotoSans-Italic.ttf"))

fun main() {
    AppWindow(title, IntSize(1024, 768)).show {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("BUTTON") },
                    onClick = {
                        println("Floating button clicked")
                    }
                )
            },
            bodyContent = {
                val amount = remember { mutableStateOf(0) }
                val animation = remember { mutableStateOf(true) }
                val text = remember {
                    mutableStateOf("Hello \uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0\nПривет")
                }
                Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly) {
                    Text(
                        text = "Привет! 你好! Desktop Compose ${amount.value}",
                        color = Color.Black,
                        modifier = Modifier
                            .background(Color.Blue)
                            .preferredHeight(56.dp)
                            .wrapContentSize(Alignment.Center)
                    )

                    Text(
                        text = with(AnnotatedString.Builder("The quick ")) {
                            pushStyle(SpanStyle(color = Color(0xff964B00)))
                            append("brown fox")
                            pop()
                            append(" 🦊 ate a ")
                            pushStyle(SpanStyle(fontSize = 30.sp))
                            append("zesty hamburgerfons")
                            pop()
                            append(" 🍔.\nThe 👩‍👩‍👧‍👧 laughed.")
                            addStyle(SpanStyle(color = Color.Green), 25, 35)
                            toAnnotatedString()
                        },
                        color = Color.Black
                    )

                    Text(
                        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
                        " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad" +
                        " minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
                        " aliquipex ea commodo consequat. Duis aute irure dolor in reprehenderit" +
                        " in voluptate velit esse cillum dolore eu fugiat nulla pariatur." +
                        " Excepteur" +
                        " sint occaecat cupidatat non proident, sunt in culpa qui officia" +
                        " deserunt mollit anim id est laborum."
                    )

                    Text(
                        text = "fun <T : Comparable<T>> List<T>.quickSort(): List<T> = when {\n" +
                        "  size < 2 -> this\n" +
                        "  else -> {\n" +
                        "    val pivot = first()\n" +
                        "    val (smaller, greater) = drop(1).partition { it <= pivot }\n" +
                        "    smaller.quickSort() + pivot + greater.quickSort()\n" +
                        "   }\n" +
                        "}",
                        modifier = Modifier.padding(10.dp),
                        fontFamily = italicFont
                    )

                    Button(onClick = {
                        amount.value++
                    }) {
                        Text("Base")
                    }

                    Row(modifier = Modifier.padding(vertical = 10.dp),
                        verticalGravity = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                            animation.value = !animation.value
                        }) {
                            Text("Toggle")
                        }

                        if (animation.value) {
                            CircularProgressIndicator()
                        }
                    }

                    Slider(value = amount.value.toFloat() / 100f,
                        onValueChange = { amount.value = (it * 100).toInt() })
                    TextField(
                        value = amount.value.toString(),
                        onValueChange = { amount.value = it.toIntOrNull() ?: 42 },
                        label = { Text(text = "Input1") }
                    )
                    TextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        label = { Text(text = "Input2") }
                    )

                    Image(imageResource("androidx/ui/desktop/example/circus.jpg"))
                }
            }
        )
    }
}
