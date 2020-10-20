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
package example.imageviewer

import androidx.compose.desktop.Window
import androidx.compose.runtime.remember
import example.imageviewer.utils.getPreferredWindowSize
import example.imageviewer.view.BuildAppUI
import example.imageviewer.model.ContentState
import example.imageviewer.model.ImageRepository
import example.imageviewer.style.icAppRounded

fun main() {

    Window(
        title = "ImageViewer",
        size = getPreferredWindowSize(800, 1000),
        icon = icAppRounded()
    ) {
        val content = ContentState.applyContent(
            "https://spvessel.com/iv/images/fetching.list"
        )
        BuildAppUI(content)
    }
}
