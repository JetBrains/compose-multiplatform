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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class KeyEventHelpersTest {
    @Test
    fun whenBackAndUp_cancels() {
        val event = KeyEvent(
            NativeKeyEvent(
                NativeKeyEvent.ACTION_UP,
                NativeKeyEvent.KEYCODE_BACK
            )
        )
        assertThat(event.cancelsTextSelection()).isTrue()
    }

    @Test
    fun whenBackAndDown_ignores() {
        val event = KeyEvent(
            NativeKeyEvent(
                NativeKeyEvent.ACTION_DOWN,
                NativeKeyEvent.KEYCODE_BACK
            )
        )
        assertThat(event.cancelsTextSelection()).isFalse()
    }

    @Test
    fun whenNotBack_ignores() {
        val event = KeyEvent(
            NativeKeyEvent(
                NativeKeyEvent.ACTION_DOWN,
                NativeKeyEvent.KEYCODE_HOME
            )
        )
        assertThat(event.cancelsTextSelection()).isFalse()
    }
}