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

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidKeyTest {
    @Test
    fun androidKeyCode_to_composeKey() {
        // Arrange.
        val key = Key(KeyEvent.KEYCODE_A)

        // Assert.
        assertThat(key).isEqualTo(Key.A)
    }

    @Test
    fun composeKey_to_androidKeyCode() {
        // Arrange.
        val key = Key.A

        // Act.
        val keyCode = key.keyCode

        // Assert.
        assertThat(keyCode).isEqualTo(KeyEvent.KEYCODE_A)
    }

    @Test
    fun UnknownAndroidKeyCodeIsPreserved() {
        // Arrange.
        val key = Key(1000)

        // Act.
        val keyCode = key.keyCode

        // Assert.
        assertThat(keyCode).isEqualTo(1000)
    }
}