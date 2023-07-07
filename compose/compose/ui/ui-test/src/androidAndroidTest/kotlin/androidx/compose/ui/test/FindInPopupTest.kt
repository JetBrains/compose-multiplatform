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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Popup
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

private const val contentTag = "content"
private const val popupTag = "popup"

@MediumTest
@RunWith(AndroidJUnit4::class)
class FindInPopupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun test() {
        rule.setContent {
            Box(Modifier.testTag(contentTag))

            Popup(alignment = Alignment.Center) {
                Box(Modifier.testTag(popupTag))
            }
        }
        rule.onNodeWithTag(contentTag).assertExists()
        rule.onNodeWithTag(popupTag).assertExists()
    }
}
