/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.event.MouseEvent
import org.junit.Test

class ComposeWindowTest {
    // bug https://github.com/JetBrains/compose-jb/issues/1448
    @Test
    fun `dispose window inside event handler`() = runApplicationTest {
        var isClickHappened = false

        val window = ComposeWindow()
        window.isUndecorated = true
        window.size = Dimension(200, 200)
        window.setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.Blue).clickable {
                isClickHappened = true
                window.dispose()
            })
        }

        window.isVisible = true
        awaitIdle()

        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x = 100, y = 50)
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x = 100, y = 50)
        awaitIdle()

        assertThat(isClickHappened).isTrue()
    }
}