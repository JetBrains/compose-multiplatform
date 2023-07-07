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

import androidx.compose.ui.ExperimentalComposeUiApi
import android.view.KeyEvent.KEYCODE_A as KeyCodeA
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class KeyTest {
    @Test
    fun androidKeyCode_to_composeKey() {
        // Arrange.
        val androidKeyCode = KeyCodeA

        // Act.
        val composeKey = Key(androidKeyCode)

        // Assert.
        assertThat(composeKey).isEqualTo(Key.A)
    }

    @Test
    fun composeKey_to_androidKeyCode() {
        // Arrange.
        val composeKey = Key.A

        // Act.
        val androidKeyCode = composeKey.nativeKeyCode

        // Assert.
        assertThat(androidKeyCode).isEqualTo(KeyCodeA)
    }
}
