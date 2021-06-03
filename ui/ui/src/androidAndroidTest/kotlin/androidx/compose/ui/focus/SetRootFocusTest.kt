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

package androidx.compose.ui.focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

@LargeTest
@RunWith(AndroidJUnit4::class)
class SetRootFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private val focusable = "Focusable"
    private val nonFocusable = "NotFocusable"

    @Test
    fun clearFocus_byClickingOutsideFocusableComponent() {
        // Arrange.
        var isFocused = false
        var focusChanged = CountDownLatch(1)
        rule.setContent {
            Column {
                // TODO(b/163725615): Remove this after clickable is made focusable.
                val focusRequester = remember { FocusRequester() }
                BasicText(
                    text = "ClickableText",
                    modifier = Modifier
                        .testTag(focusable)
                        .clickable { focusRequester.requestFocus() }
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                            focusChanged.countDown()
                        }
                        .focusTarget()
                )
                BasicText(
                    text = "Non Clickable Text",
                    modifier = Modifier.testTag(nonFocusable)
                )
            }
        }
        rule.onNodeWithTag(focusable).performClick()
        rule.waitForIdle()
        focusChanged.await(1, SECONDS)
        rule.runOnIdle { assertThat(isFocused).isTrue() }
        focusChanged = CountDownLatch(1)

        // Act.
        rule.onNodeWithTag(nonFocusable).performClick()

        // Assert.
        rule.waitForIdle()
        focusChanged.await(1, SECONDS)
        rule.runOnIdle { assertThat(isFocused).isFalse() }
    }
}
