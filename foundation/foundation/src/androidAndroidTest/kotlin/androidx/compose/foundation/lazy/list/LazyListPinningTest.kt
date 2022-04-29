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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.layout.ModifierLocalPinnableParent
import androidx.compose.foundation.lazy.layout.PinnableParent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@MediumTest
class LazyListPinningTest {

    @get:Rule
    val rule = createComposeRule()

    private var beyondBoundsLayout: BeyondBoundsLayout? = null
    private var pinnableParent: PinnableParent? = null

    @Test
    fun UnpinnedBeyondBoundsItems() {
        // Arrange.
        rule.setContent {
            LazyRow(Modifier.size(10.dp)) {
                item {
                    Box(Modifier
                        .size(10.dp)
                        .testTag("0")
                        .modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                            pinnableParent = ModifierLocalPinnableParent.current
                        }
                    )
                }
                items(100) { index ->
                    Box(Modifier.size(10.dp).testTag("${index + 1}"))
                }
            }
        }

        // Act - Add 10 items beyond bounds.
        var extraItemCount = 10
        rule.runOnIdle {
            beyondBoundsLayout!!.layout(Right) {
                // Return null to continue the search, and true to stop.
                if (--extraItemCount > 0) null else true
            }
        }

        // Assert.
        rule.waitForIdle()
        rule.onNodeWithTag("9").assertDoesNotExist()
    }

    @Test
    fun pinnedBeyondBoundsItems() {
        // Arrange.
        rule.setContent {
            LazyRow(Modifier.size(10.dp)) {
                item {
                    Box(Modifier
                        .size(10.dp)
                        .testTag("0")
                        .modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                            pinnableParent = ModifierLocalPinnableParent.current
                        }
                    )
                }
                items(100) { index ->
                    Box(Modifier.size(10.dp).testTag("${index + 1}"))
                }
            }
        }

        // Act - Add 10 items beyond bounds, and pin them.
        var extraItemCount = 10
        lateinit var pinnedItemsHandle: PinnableParent.PinnedItemsHandle
        rule.runOnIdle {
            beyondBoundsLayout!!.layout(Right) {
                if (--extraItemCount > 0) {
                    // Return null to continue the search.
                    null
                } else {
                    pinnedItemsHandle = pinnableParent!!.pinItems()
                    // Return true to stop the search.
                    true
                }
            }
        }

        // Assert - The beyond bounds items are not disposed.
        rule.waitForIdle()
        rule.onNodeWithTag("9").assertExists()

        // Act - Unpin the items.
        rule.runOnIdle { pinnedItemsHandle.unpin() }

        // Assert - The beyond bounds items are disposed.
        rule.waitForIdle()
        rule.onNodeWithTag("9").assertDoesNotExist()
    }
}
