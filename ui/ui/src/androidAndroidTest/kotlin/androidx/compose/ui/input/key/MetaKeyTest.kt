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

import android.view.KeyEvent.KEYCODE_A as KeyCodeA
import android.view.KeyEvent.ACTION_DOWN as KeyDown
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.META_ALT_ON
import android.view.KeyEvent.META_CTRL_ON
import android.view.KeyEvent.META_META_ON
import android.view.KeyEvent.META_SHIFT_ON
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MetaKeyTest {

    @Test
    fun noMetaKeyIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent()

        // Assert.
        assertThat(keyEvent.isAltPressed).isFalse()
        assertThat(keyEvent.isCtrlPressed).isFalse()
        assertThat(keyEvent.isMetaPressed).isFalse()
        assertThat(keyEvent.isShiftPressed).isFalse()
    }

    @Test
    fun altIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_ALT_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isTrue()
        assertThat(keyEvent.isCtrlPressed).isFalse()
        assertThat(keyEvent.isMetaPressed).isFalse()
        assertThat(keyEvent.isShiftPressed).isFalse()
    }

    @Test
    fun ctrlIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_CTRL_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isFalse()
        assertThat(keyEvent.isCtrlPressed).isTrue()
        assertThat(keyEvent.isMetaPressed).isFalse()
        assertThat(keyEvent.isShiftPressed).isFalse()
    }

    @Test
    fun metaIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_META_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isFalse()
        assertThat(keyEvent.isCtrlPressed).isFalse()
        assertThat(keyEvent.isMetaPressed).isTrue()
        assertThat(keyEvent.isShiftPressed).isFalse()
    }

    @Test
    fun shiftIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_SHIFT_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isFalse()
        assertThat(keyEvent.isCtrlPressed).isFalse()
        assertThat(keyEvent.isMetaPressed).isFalse()
        assertThat(keyEvent.isShiftPressed).isTrue()
    }

    @Test
    fun CtrlShiftIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_CTRL_ON or META_SHIFT_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isFalse()
        assertThat(keyEvent.isCtrlPressed).isTrue()
        assertThat(keyEvent.isMetaPressed).isFalse()
        assertThat(keyEvent.isShiftPressed).isTrue()
    }

    @Test
    fun AltCtrlShiftIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_ALT_ON or META_CTRL_ON or META_SHIFT_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isTrue()
        assertThat(keyEvent.isCtrlPressed).isTrue()
        assertThat(keyEvent.isMetaPressed).isFalse()
        assertThat(keyEvent.isShiftPressed).isTrue()
    }

    @Test
    fun AltCtrlMetaShiftIsPressed() {
        // Arrange.
        val keyEvent = testKeyEvent(META_ALT_ON or META_CTRL_ON or META_META_ON or META_SHIFT_ON)

        // Assert.
        assertThat(keyEvent.isAltPressed).isTrue()
        assertThat(keyEvent.isCtrlPressed).isTrue()
        assertThat(keyEvent.isMetaPressed).isTrue()
        assertThat(keyEvent.isShiftPressed).isTrue()
    }

    private fun testKeyEvent(androidMetaKeys: Int = 0): KeyEvent {
        return KeyEvent(AndroidKeyEvent(0L, 0L, KeyDown, KeyCodeA, 0, androidMetaKeys))
    }
}
