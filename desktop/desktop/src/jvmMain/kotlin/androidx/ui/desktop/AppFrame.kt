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
package androidx.ui.desktop

import androidx.compose.runtime.Composable

abstract class AppFrame {

    var window: ComposeWindow? = null
        protected set

    var invoker: AppFrame? = null
        protected set

    var locked = false

    var title = ""
        protected set

    var width = 0
        protected set

    var height = 0
        protected set

    var x = 0
        protected set

    var y = 0
        protected set

    var isCentered: Boolean = true
        protected set

    val onDismissEvents = mutableListOf<() -> Unit>()

    abstract fun setPosition(x: Int, y: Int)

    abstract fun setWindowCentered()

    abstract fun setSize(width: Int, height: Int)

    abstract fun show(content: @Composable () -> Unit)

    abstract fun close()

    internal abstract fun connectPair(window: AppFrame)

    internal abstract fun disconnectPair()
}
