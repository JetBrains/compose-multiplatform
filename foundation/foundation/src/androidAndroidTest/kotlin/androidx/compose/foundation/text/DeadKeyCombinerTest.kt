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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class DeadKeyCombinerTest {

    private val keyEventUmlaut = KeyEvent(
        NativeKeyEvent(
            0,
            0,
            NativeKeyEvent.ACTION_DOWN,
            NativeKeyEvent.KEYCODE_U,
            0,
            NativeKeyEvent.META_ALT_ON
        )
    )

    private val keyEventSpace =
        KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_SPACE))

    private val keyEventO =
        KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_O))

    private val keyEventJ =
        KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_J))

    @Test
    fun testHappyPath() {
        test(
            keyEventUmlaut to null,
            keyEventO to 'ö',
        )
    }

    @Test
    fun testMultipleDeadKeysFollowedByMultipleComposingKeys() {
        test(
            keyEventUmlaut to null,
            keyEventUmlaut to null,
            keyEventUmlaut to null,
            keyEventO to 'ö',
            keyEventO to 'o',
            keyEventO to 'o',
        )
    }

    @Test
    fun testMultiplePressesInterleaved() {
        test(
            keyEventO to 'o',
            keyEventUmlaut to null,
            keyEventO to 'ö',
            keyEventUmlaut to null,
            keyEventUmlaut to null,
            keyEventO to 'ö',
            keyEventUmlaut to null,
            keyEventO to 'ö',
            keyEventO to 'o',
        )
    }

    @Test
    fun testNonExistingCombinationFallsBackToCurrentKey() {
        test(
            keyEventUmlaut to null,
            keyEventJ to 'j',
        )
    }

    @Test
    fun testSameDeadKey() {
        test(
            keyEventUmlaut to null,
            keyEventUmlaut to null,
        )
    }

    @Test
    fun testDeadKeyThenSpaceOutputsTheAccent() {
        test(
            keyEventUmlaut to null,
            keyEventSpace to '¨',
        )
    }

    private fun test(vararg pairs: Pair<KeyEvent, Char?>) {
        val combiner = DeadKeyCombiner()
        pairs.forEach { (event, result) ->
            assertThat(combiner.consume(event)?.toChar()).run {
                when (result) {
                    null -> isNull()
                    else -> isEqualTo(result)
                }
            }
        }
    }
}