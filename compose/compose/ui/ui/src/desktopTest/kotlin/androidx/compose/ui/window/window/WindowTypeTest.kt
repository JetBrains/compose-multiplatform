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

package androidx.compose.ui.window.window

import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.sendInputEvent
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendKeyTypedEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import org.junit.Test

/**
 * Tests for emulate input to the native window on various systems.
 *
 * Events were captured on each system via logging.
 * All tests can run on all OSes.
 * The OS names in test names just represent a unique order of input events on these OSes.
 */
class WindowTypeTest {
    @Test
    fun `q, w, space, backspace 4x (English)`() = runTypeTest {
        // q
        window.sendKeyEvent(81, 'q', KEY_PRESSED)
        window.sendKeyTypedEvent('q')
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(87, 'w', KEY_PRESSED)
        window.sendKeyTypedEvent('w')
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "qw", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "qw ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "qw", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, space, backspace 4x (Russian)`() = runTypeTest {
        // q
        window.sendKeyEvent(81, 'й', KEY_PRESSED)
        window.sendKeyTypedEvent('й')
        window.sendKeyEvent(81, 'й', KEY_RELEASED)
        assert(text, "й", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(87, 'ц', KEY_PRESSED)
        window.sendKeyTypedEvent('ц')
        window.sendKeyEvent(87, 'ц', KEY_RELEASED)
        assert(text, "йц", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "йц ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "йц", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "й", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `f, g, space, backspace 4x (Arabic)`() = runTypeTest {
        // q
        window.sendKeyEvent(70, 'ب', KEY_PRESSED)
        window.sendKeyTypedEvent('ب')
        window.sendKeyEvent(70, 'ب', KEY_RELEASED)
        assert(text, "ب", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(71, 'ل', KEY_PRESSED)
        window.sendKeyTypedEvent('ل')
        window.sendKeyEvent(71, 'ل', KEY_RELEASED)
        assert(text, "بل", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "بل ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "بل", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ب", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, space, backspace 4x (Korean, Windows)`() = runTypeTest {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent(null, 0)
        window.sendKeyTypedEvent('ㅈ')
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, backspace 3x (Korean, Windows)`() = runTypeTest {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `f, g, space, backspace 3x (Korean, Windows)`() = runTypeTest {
        // f
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assert(text, "ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assert(text, "ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputEvent(null, 0)
        window.sendKeyTypedEvent('ㅀ')
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "ㅀ ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅀ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `f, g, backspace 2x (Korean, Windows)`() = runTypeTest {
        // f
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assert(text, "ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assert(text, "ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, space, backspace 4x (Korean, macOS)`() = runTypeTest {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent("ㅈ ", 0)
        window.sendInputEvent("ㅈ ", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, backspace 3x (Korean, macOS)`() = runTypeTest {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputEvent("ㅈ", 0)
        window.sendInputEvent("ㅈ", 1)
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    // f, g on macOs prints 2 separate symbols (comparing to Windows), so we test t + y
    @Test
    fun `t, y, space, backspace 3x (Korean, MacOS)`() = runTypeTest {
        // t
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assert(text, "ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assert(text, "쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputEvent("쇼 ", 0)
        window.sendInputEvent("쇼 ", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "쇼 ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "쇼", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `t, y, backspace 2x (Korean, MacOS)`() = runTypeTest {
        // t
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assert(text, "ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assert(text, "쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㅅ", 0)
        window.sendInputEvent("ㅅ", 1)
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, space, backspace 4x (Korean, Linux)`() = runTypeTest {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(0, 'ㅂ', KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent(null, 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(0, 'ㅈ', KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent(null, 0)
        window.sendInputEvent("ㅈ", 1)
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, space, backspace 3x (Chinese, Windows)`() = runTypeTest {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputEvent("請問", 2)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "請問", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "請", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, backspace 3x (Chinese, Windows)`() = runTypeTest {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, space, backspace 3x (Chinese, macOS)`() = runTypeTest {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "q w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputEvent("请问", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assert(text, "请问", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "请", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    @Test
    fun `q, w, backspace 3x (Chinese, macOS)`() = runTypeTest {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assert(text, "q w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assert(text, "", selection = TextRange(0), composition = null)
    }

    private class TypeTestScope(val windowTestScope: WindowTestScope) {
        lateinit var window: ComposeWindow
        var text by mutableStateOf(TextFieldValue())
    }

    private fun runTypeTest(body: suspend TypeTestScope.() -> Unit) = runApplicationTest(
        hasAnimations = true,
        animationsDelayMillis = 100
    ) {
        val scope = TypeTestScope(this)
        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                scope.window = this.window
                val focusRequester = FocusRequester()
                TextField(
                    value = scope.text,
                    onValueChange = { scope.text = it },
                    modifier = Modifier.focusRequester(focusRequester)
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }

        awaitIdle()
        scope.body()
    }

    private suspend fun TypeTestScope.assert(
        actual: TextFieldValue, text: String, selection: TextRange, composition: TextRange?
    ) {
        windowTestScope.awaitIdle()
        assertThat(actual.text).isEqualTo(text)
        assertThat(actual.selection).isEqualTo(selection)
        assertThat(actual.composition).isEqualTo(composition)
    }
}
