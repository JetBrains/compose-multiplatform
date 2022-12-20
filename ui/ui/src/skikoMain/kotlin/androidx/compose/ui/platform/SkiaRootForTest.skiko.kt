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

import androidx.compose.ui.ComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.node.RootForTest

/**
 * The marker interface to be implemented by the desktop root backing the composition.
 * To be used in tests.
 */
@InternalComposeUiApi
interface SkiaRootForTest : RootForTest {
    /**
     * The [ComposeScene] which contains this root
     */
    val scene: ComposeScene get() = throw UnsupportedOperationException("SkiaRootForTest.scene is not implemented")

    /**
     * Whether the Owner has pending layout work.
     */
    val hasPendingMeasureOrLayout: Boolean

    /**
     * Process pointer event
     *
     * [timeMillis] time when the pointer event occurred
     * [pointers] state of all pointers
     */
    @Suppress("DEPRECATION")
    @Deprecated("Don't send events directly to root. Send events to [scene]. Will be removed in Compose 1.3")
    fun processPointerInput(timeMillis: Long, pointers: List<TestPointerInputEventData>)
}