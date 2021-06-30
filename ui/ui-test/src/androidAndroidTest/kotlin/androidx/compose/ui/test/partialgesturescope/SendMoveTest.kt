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

package androidx.compose.ui.test.partialgesturescope

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.move
import androidx.compose.ui.test.partialgesturescope.Common.partialGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.test.filters.MediumTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests the error states of [move] that are not tested in [SendMoveToTest] and [SendMoveByTest]
 */
@MediumTest
class SendMoveTest() {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
    }

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent {
            ClickableTestBox()
        }
    }

    @Test
    fun moveWithoutDown() {
        expectError<IllegalStateException> {
            rule.partialGesture { move() }
        }
    }

    @Test
    fun moveAfterUp() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { up() }
        expectError<IllegalStateException> {
            rule.partialGesture { move() }
        }
    }

    @Test
    fun moveAfterCancel() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { cancel() }
        expectError<IllegalStateException> {
            rule.partialGesture { move() }
        }
    }
}
