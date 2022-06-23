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

package androidx.compose.mpp.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Application

fun getViewControllerWithCompose() = Application("Compose/Native sample") {
    var textState1 by remember { mutableStateOf("text field 1") }
    var textState2 by remember { mutableStateOf("text field 2") }
    Column {
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text("Hello, UIKit")
        TextField(value = textState1, onValueChange = {
            textState1 = it
        })
        TextField(value = textState2, onValueChange = {
            textState2 = it
        })
        Image(
            painter = object : Painter() {
                override val intrinsicSize: Size = Size(16f, 16f)
                override fun DrawScope.onDraw() {
                    drawRect(color = Color.Blue)
                }
            },
            contentDescription = "image sample"
        )
    }
}
