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

package androidx.compose.testutils

import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DpAssertionsTest {

    @Test
    fun dp_assertEquals() {
        5.dp.assertIsEqualTo(5.dp)
        5.dp.assertIsEqualTo(4.6.dp)
        5.dp.assertIsEqualTo(5.4.dp)
    }

    @Test
    fun dp_assertNotEquals() {
        5.dp.assertIsNotEqualTo(6.dp)
    }

    @Test
    fun dp_assertEquals_fail() {
        expectError<AssertionError> {
            5.dp.assertIsEqualTo(6.dp)
        }
    }

    @Test
    fun dp_assertNotEquals_fail() {
        expectError<AssertionError> {
            5.dp.assertIsNotEqualTo(5.dp)
            5.dp.assertIsNotEqualTo(5.4.dp)
        }
    }
}