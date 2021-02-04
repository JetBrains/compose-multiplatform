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

package androidx.compose.ui.platform

import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.node.RootForTest

/**
 * The marker interface to be implemented by the desktop root backing the composition.
 * To be used in tests.
 */
interface DesktopRootForTest : RootForTest {
    /**
     * Process pointer event
     *
     * [nanoTime] time when the pointer event occurred
     * [pointers] state of all pointers
     */
    fun processPointerInput(nanoTime: Long, pointers: List<TestPointerInputEventData>)
}