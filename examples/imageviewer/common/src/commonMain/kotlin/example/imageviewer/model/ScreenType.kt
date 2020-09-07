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
package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

enum class ScreenType {
    Main, FullscreenImage
}

object AppState {
    private var screen: MutableState<ScreenType>
    init {
        screen = mutableStateOf(ScreenType.Main)
    }

    fun screenState() : ScreenType {
        return screen.value
    }

    fun screenState(state: ScreenType) {
        screen.value = state
    }
}