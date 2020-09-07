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
package example.imageviewer.view

import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onActive
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.GlobalScope
import example.imageviewer.style.ToastBackground
import example.imageviewer.style.Foreground

enum class ToastDuration(val value: Int) {
    Short(1000), Long(3000)
}

private var isShown: Boolean = false

@Composable
fun Toast(
    text: String,
    visibility: MutableState<Boolean> = mutableStateOf(false),
    duration: ToastDuration = ToastDuration.Long
) {
    if (isShown) {
        return
    }

    if (visibility.value) {
        isShown = true
        Popup(
            alignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier.preferredSize(300.dp, 70.dp),
                backgroundColor = ToastBackground,
                shape = RoundedCornerShape(4.dp),
                gravity = ContentGravity.Center
            ) {
                Text(
                    text = text,
                    color = Foreground
                )
            }
            onActive {
                GlobalScope.launch {
                    delay(duration.value.toLong())
                    isShown = false
                    visibility.value = false
                }
            }
        }
    }
}