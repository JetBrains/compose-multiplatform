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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Test

class TestComposeWindowTest {
    @Test
    fun `run multiple TestComposeWindow`() {
        for (i in 1..15) {
            TestComposeWindow(800, 800).setContent {
                Box(Modifier.fillMaxWidth()) {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Filled.AccountBox, "") },
                        text = {},
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    @Test(timeout = 5000)
    fun `disposing TestComposeWindow should not cancel coroutineContext's Job`() {
        runBlocking(Dispatchers.Swing) {
            val window = TestComposeWindow(100, 100, coroutineContext = coroutineContext)
            window.dispose()
        }
    }
}