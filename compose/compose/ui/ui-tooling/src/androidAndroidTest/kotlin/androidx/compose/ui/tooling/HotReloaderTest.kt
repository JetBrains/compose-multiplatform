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

package androidx.compose.ui.tooling

import org.junit.Assert.assertNotNull
import org.junit.Test

class HotReloaderTest {
    /**
     * Ensure that the `HotReloader` interface used by the preview is not modified inadvertently.
     *
     * This test replicates the sequence of calls done from the preview to obtain the methods from
     * `HotReloader`.
     */
    @Test
    fun checkViaReflection() {
        val hotReloader = HotReloaderTest::class.java.classLoader!!
            .loadClass("androidx.compose.runtime.HotReloader")
        val hotReloaderInstance = hotReloader.getDeclaredField("Companion").let {
            it.isAccessible = true
            it.get(null)
        }
        val saveStateAndDisposeMethod =
            hotReloaderInstance.javaClass.getDeclaredMethod("saveStateAndDispose", Any::class.java)
                .also {
                    it.isAccessible = true
                }
        val loadStateAndDisposeMethod =
            hotReloaderInstance.javaClass.getDeclaredMethod("loadStateAndCompose", Any::class.java)
                .also {
                    it.isAccessible = true
                }
        assertNotNull(saveStateAndDisposeMethod)
        assertNotNull(loadStateAndDisposeMethod)
    }
}