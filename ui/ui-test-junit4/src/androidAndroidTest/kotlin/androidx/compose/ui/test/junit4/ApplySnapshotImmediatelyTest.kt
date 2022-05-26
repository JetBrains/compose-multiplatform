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

package androidx.compose.ui.test.junit4

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

class ApplySnapshotImmediatelyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun test() {
        var count1 by mutableStateOf(0)
        var count2 by mutableStateOf(0)
        val sum by derivedStateOf { count1 + count2 }
        rule.mainClock.autoAdvance = false

        fun assertStateIs(expected1: Int, expected2: Int, expectedSum: Int) {
            Truth.assertThat(count1).isEqualTo(expected1)
            Truth.assertThat(count2).isEqualTo(expected2)
            Truth.assertThat(sum).isEqualTo(expectedSum)
        }
        fun assertDisplayIs(expected1: Int, expected2: Int, expectedSum: Int) {
            rule.onNodeWithText("Count1 = $expected1").assertIsDisplayed()
            rule.onNodeWithText("Count2 = $expected2").assertIsDisplayed()
            rule.onNodeWithText("Sum = $expectedSum").assertIsDisplayed()
        }

        rule.setContent {
            LaunchedEffect(Unit) {
                delay(100)
                count1++
                delay(100)
                count2++
            }
            Column {
                Text("Count1 = $count1")
                Text("Count2 = $count2")
                Text("Sum = $sum")
            }
        }

        // Initial state:
        assertStateIs(0, 0, 0)
        assertDisplayIs(0, 0, 0)

        rule.mainClock.advanceTimeBy(100 - 1, ignoreFrameDuration = true)
        // Nothing happened yet:
        assertStateIs(0, 0, 0)
        assertDisplayIs(0, 0, 0)

        rule.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        // State has changed:
        assertStateIs(1, 0, 1)
        assertDisplayIs(0, 0, 0)

        rule.mainClock.advanceTimeBy(16 - 1, ignoreFrameDuration = true)
        // Nothing changed:
        assertStateIs(1, 0, 1)
        assertDisplayIs(0, 0, 0)

        rule.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        // Composition happened:
        assertStateIs(1, 0, 1)
        assertDisplayIs(1, 0, 1)

        rule.mainClock.advanceTimeBy(100 - 16, ignoreFrameDuration = true)
        // State has changed again:
        assertStateIs(1, 1, 2)
        assertDisplayIs(1, 0, 1)

        rule.mainClock.advanceTimeByFrame()
        // Composition happened again:
        assertStateIs(1, 1, 2)
        assertDisplayIs(1, 1, 2)
    }
}