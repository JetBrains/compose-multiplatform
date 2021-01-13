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

package androidx.compose.ui.input.key

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import java.awt.event.KeyEvent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun desktopKey_to_composeKey() {
        // Arrange.
        val desktopKeyCode = KeyEvent.VK_ALT
        val desktopKeyLocation = KeyEvent.KEY_LOCATION_LEFT

        // Act.
        val desktopKey = Key(desktopKeyCode, desktopKeyLocation)

        // Assert.
        assertThat(desktopKey).isEqualTo(Key.AltLeft)
    }

    @Test
    fun composeKey_to_desktopKeyCode() {
        // Arrange.
        val key = Key.A

        // Act.
        val nativeKeyCode = key.nativeKeyCode

        // Arrange.
        assertThat(nativeKeyCode).isEqualTo(KeyEvent.VK_A)
    }

    @Test
    fun composeKey_to_standardDesktopKeyLocation() {
        // Arrange.
        val key = Key.A

        // Act.
        val desktopKeyLocation = key.nativeKeyLocation

        // Arrange.
        assertThat(desktopKeyLocation).isEqualTo(KeyEvent.KEY_LOCATION_STANDARD)
    }

    @Test
    fun composeKey_to_knownDesktopKeyLocation() {
        // Arrange.
        val key = Key.AltLeft

        // Act.
        val desktopKeyLocation = key.nativeKeyLocation

        // Arrange.
        assertThat(desktopKeyLocation).isEqualTo(KeyEvent.KEY_LOCATION_LEFT)
    }
}
