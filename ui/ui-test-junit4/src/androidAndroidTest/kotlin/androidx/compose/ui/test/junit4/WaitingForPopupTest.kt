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

package androidx.compose.ui.test.junit4

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WaitingForPopupTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun ShowPopup() {
        Popup {
            Box(Modifier.size(10.dp, 10.dp))
        }
    }

    @Test
    fun popupInFirstComposition() {
        rule.setContent {
            ShowPopup()
        }
        rule.onNode(isPopup()).assertExists()
    }

    @Test
    fun popupInLaterComposition() {
        val showPopup = mutableStateOf(false)
        rule.setContent {
            if (showPopup.value) {
                ShowPopup()
            }
        }
        rule.onNode(isPopup()).assertDoesNotExist()
        showPopup.value = true
        rule.onNode(isPopup()).assertExists()
    }

    @Test
    fun popupTogglingRepeatedly() {
        val showPopup = mutableStateOf(false)
        rule.setContent {
            if (showPopup.value) {
                ShowPopup()
            }
        }
        rule.onNode(isPopup()).assertDoesNotExist()

        // (no particular reason for 4x, could've been 10x just as well)
        repeat(4) {
            showPopup.value = !showPopup.value
            if (showPopup.value) {
                rule.onNode(isPopup()).assertExists()
            } else {
                rule.onNode(isPopup()).assertDoesNotExist()
            }
        }
    }
}
