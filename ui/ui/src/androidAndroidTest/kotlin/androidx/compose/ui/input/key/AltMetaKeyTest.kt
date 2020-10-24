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

import android.view.KeyEvent.META_ALT_LEFT_ON
import android.view.KeyEvent.META_ALT_RIGHT_ON
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalKeyInput::class)
class AltMetaKeyTest {

    @Test
    fun noMetaKeyIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent()

        // Assert.
        assertThat(keyEvent.alt.isPressed).isFalse()
        assertThat(keyEvent.alt.isLeftAltPressed).isFalse()
        assertThat(keyEvent.alt.isRightAltPressed).isFalse()
    }

    @Test
    fun altLeftIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(androidMetaKeys = META_ALT_LEFT_ON)

        // Assert.
        assertThat(keyEvent.alt.isPressed).isTrue()
        assertThat(keyEvent.alt.isLeftAltPressed).isTrue()
        assertThat(keyEvent.alt.isRightAltPressed).isFalse()
    }

    @Test
    fun altRightIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(androidMetaKeys = META_ALT_RIGHT_ON)

        // Assert.
        assertThat(keyEvent.alt.isPressed).isTrue()
        assertThat(keyEvent.alt.isLeftAltPressed).isFalse()
        assertThat(keyEvent.alt.isRightAltPressed).isTrue()
    }

    @Test
    fun altLeftAndAltRightArePressed() {
        // Arrange.
        val keyEvent = testKeyEvent(androidMetaKeys = META_ALT_LEFT_ON or META_ALT_RIGHT_ON)

        // Assert.
        assertThat(keyEvent.alt.isPressed).isTrue()
        assertThat(keyEvent.alt.isLeftAltPressed).isTrue()
        assertThat(keyEvent.alt.isRightAltPressed).isTrue()
    }

    private fun testKeyEvent(androidMetaKeys: Int = 0): KeyEvent {
        return keyEvent(Key.A, KeyEventType.KeyDown, androidMetaKeys)
    }
}
