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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.launchApplication
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

internal class PreviewRunner {
    companion object {
        private var previewComposition: @Composable () -> Unit = {}

        @Suppress("DEPRECATION")
        @JvmStatic
        @OptIn(ExperimentalComposeUiApi::class)
        fun main(args: Array<String>) {
            val previewFqName = args[0]
            val className = previewFqName.substringBeforeLast(".")
            val methodName = previewFqName.substringAfterLast(".")

            previewComposition = @Composable {
                // We need to delay the reflection instantiation of the class until we are in the
                // composable to ensure all the right initialization has happened and the Composable
                // class loads correctly.
                androidx.compose.ui.tooling.ComposableInvoker.invokeComposable(
                    className,
                    methodName,
                    currentComposer
                )
            }

            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launchApplication {
                Window(onCloseRequest = ::exitApplication) {
                    previewComposition()
                }
            }
        }
    }
}