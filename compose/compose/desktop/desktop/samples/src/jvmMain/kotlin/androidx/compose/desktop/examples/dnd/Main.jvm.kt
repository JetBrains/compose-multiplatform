/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.desktop.examples.dnd

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.onExternalDrag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication(
    title = "External dnd demo"
) {
    MaterialTheme {
        val size = 200.dp

        var isDragging by remember { mutableStateOf(false) }
        var text by remember { mutableStateOf<String?>(null) }
        var painter by remember { mutableStateOf<Painter?>(null) }

        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier.size(size, size)
                    .background(
                        when {
                            isDragging -> Color.Green
                            text != null -> Color.White
                            else -> Color.Red
                        }
                    )
                    .onExternalDrag(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragExit = {
                            isDragging = false
                        },
                        onDrag = {

                        },
                        onDrop = { state ->
                            val dragData = state.dragData
                            text = dragData.toString()
                            if (dragData is DragData.Image) {
                                painter = dragData.readImage()
                            }
                            isDragging = false
                        })
            ) {
                Text(text ?: "Try to drag some files or image here", modifier = Modifier.align(Alignment.Center))
                val currentPainter = painter
                if (currentPainter != null) {
                    Image(currentPainter, contentDescription = "Pasted Image")
                }
            }
        }
    }
}