/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.desktop.ui.tooling.preview.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density

/**
 * This class is used by Compose Desktop Intellij plugin
 * for the non-interactive preview functionality.
 *
 * The class is used via reflection.
 *
 * The non-interactive preview for desktop works in the following way:
 * 1. A user annotates a composable function with the @Preview annotation;
 * 2. A user is able to request an IDE preview by clicking a gutter icon in the editor;
 * 3. When an updated preview is requested, the Intellij plugin requests a fresh build from Gradle
 *    and pulls an updated preview configuration from the build;
 * 4. The IDE plugin manages a separate preview host process;
 * 5. The IDE plugin requests a new frame from the preview host process, when needed (either when
 *    a user requests it explicitly or by resizing the preview tool window);
 * 6. When the preview process receives a preview request, it renders a new frame using this
 *    facade in an isolated classloader via reflection. The classloader is persisted between
 *    requests to minimize render time. Currently it is reused only if there are no changes in a
 *    preview classpath (a requested preview function or a requested frame size may differ).
 * 7. A rendered frame is sent back to the IDE plugin and is shown in the IDE as an image.
 */
@Suppress("DEPRECATION", "unused")
internal class NonInteractivePreviewFacade {
    companion object {
        @JvmStatic
        @OptIn(ExperimentalComposeUiApi::class)
        fun render(fqName: String, width: Int, height: Int, scale: Double?): ByteArray {
            val className = fqName.substringBeforeLast(".")
            val methodName = fqName.substringAfterLast(".")
            val density = scale?.let { Density(it.toFloat()) } ?: Density(1f)
            val window = TestComposeWindow(width = width, height = height, density = density)
            window.setContent @Composable {
                // We need to delay the reflection instantiation of the class until we are in the
                // composable to ensure all the right initialization has happened and the Composable
                // class loads correctly.
                androidx.compose.ui.tooling.ComposableInvoker.invokeComposable(
                    className,
                    methodName,
                    currentComposer
                )
            }
            return window.surface.makeImageSnapshot().encodeToData()!!.bytes
        }
    }
}