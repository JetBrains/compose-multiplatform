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
package androidx.compose.desktop.examples.popupexample

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.lang.Exception

object AppState {
    private val imageRes: String = "androidx/compose/desktop/example/tray.png"
    private var icon: BufferedImage? = null
    fun image(): BufferedImage {
        if (icon != null) {
            return icon!!
        }
        try {
            val img = Thread.currentThread().contextClassLoader.getResource(imageRes)
            val bitmap: BufferedImage? = ImageIO.read(img)
            if (bitmap != null) {
                icon = bitmap
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    }

    val wndTitle = mutableStateOf("Desktop Compose Popup")
    val wndSize = mutableStateOf(IntSize.Zero)
    val wndPos = mutableStateOf(IntOffset.Zero)
    val popupState = mutableStateOf(false)
    val amount = mutableStateOf(0)

    val undecorated = mutableStateOf(false)
    val alertDialog = mutableStateOf(false)

    val notify = mutableStateOf(true)
    val warn = mutableStateOf(false)
    val error = mutableStateOf(false)

    fun diselectOthers(state: MutableState<Boolean>) {
        if (notify != state) {
            notify.value = false
        }
        if (warn != state) {
            warn.value = false
        }
        if (error != state) {
            error.value = false
        }
    }
}