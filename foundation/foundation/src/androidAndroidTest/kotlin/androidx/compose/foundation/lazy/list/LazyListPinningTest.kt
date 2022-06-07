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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
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
    private var itemCount by mutableStateOf(100)

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
                items(itemCount - 1) { index ->
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
        rule.onNodeWithTag("0").assertPlaced()
        rule.assertNotPlaced(1..itemCount)
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
                items(itemCount - 1) { index ->
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
        rule.assertPlaced(0..10)
        rule.assertNotPlaced(11..itemCount)

        // Act - Unpin the items.
        rule.runOnIdle { pinnedItemsHandle.unpin() }

        // Assert - The beyond bounds items are disposed.
        rule.waitForIdle()
        rule.onNodeWithTag("0").assertPlaced()
        rule.assertNotPlaced(1..itemCount)
    }

    @Test
    fun pinnedBeyondBoundsItems_reduceItemCount_greaterThanBeyondBoundsItems() {
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
                items(itemCount - 1) { index ->
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

        // Act - Reduce the number of items.
        rule.runOnIdle { itemCount = 50 }

        // Assert - The beyond bounds items are not disposed.
        rule.waitForIdle()
        rule.assertPlaced(0..10)
        rule.assertNotPlaced(11..itemCount)

        // Cleanup - Unpin the items.
        rule.runOnIdle { pinnedItemsHandle.unpin() }
    }

    @Test
    fun pinnedBeyondBoundsItems_reduceItemCount_equalToBeyondBoundsItems() {
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
                items(itemCount - 1) { index ->
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

        // Act - Reduce the number of items so that it includes visible + beyond bounds items.
        rule.runOnIdle { itemCount = 11 }

        // Assert - The beyond bounds items are not disposed.
        rule.waitForIdle()
        rule.assertPlaced(0..10)
        rule.assertNotPlaced(11..itemCount)

        // Cleanup - Unpin the items.
        rule.runOnIdle { pinnedItemsHandle.unpin() }
    }

    @Test
    fun pinnedBeyondBoundsItems_reduceItemCount_lessThanBeyondBoundsItems() {
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
                items(itemCount - 1) { index ->
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

        // Act - Reduce the number of items to a number less than the beyond bounds items.
        rule.runOnIdle { itemCount = 5 }

        // Assert - Only the beyond bounds items after item count are disposed.
        rule.waitForIdle()
        rule.assertPlaced(0..4)
        rule.assertNotPlaced(5..itemCount)

        // Cleanup - Unpin the items.
        rule.runOnIdle { pinnedItemsHandle.unpin() }
    }

    @Test
    fun pinnedBeyondBoundsItems_reduceItemCount_noBeyondBoundItems() {
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
                items(itemCount - 1) { index ->
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

        // Act - Reduce the number of items to only the visible items.
        rule.runOnIdle { itemCount = 1 }

        // Assert - The beyond bounds items are disposed.
        rule.waitForIdle()
        rule.onNodeWithTag("0").assertPlaced()
        rule.assertNotPlaced(1..itemCount)

        // Cleanup - Unpin the items.
        rule.runOnIdle { pinnedItemsHandle.unpin() }
    }

    private fun ComposeContentTestRule.assertPlaced(tags: IntRange) {
        tags.forEach {
            onNodeWithTag("$it").assertPlaced()
        }
    }

    private fun ComposeContentTestRule.assertNotPlaced(tags: IntRange) {
        tags.forEachIndexed { index, tag ->
            // TODO(b/228100623): Remove this after we fix this bug
            //  which places extra items that are replaced by padding.
            if (index == 0) return@forEachIndexed

            onNodeWithTag("$tag").assertNotPlaced() }
    }

    /**
     * Asserts that the current semantics node is placed.
     *
     * Throws [AssertionError] if the node is not placed.
     */
    private fun SemanticsNodeInteraction.assertPlaced(): SemanticsNodeInteraction {
        val errorMessageOnFail = "Assert failed: The component is not placed!"
        if (!fetchSemanticsNode(errorMessageOnFail).layoutInfo.isPlaced) {
            throw AssertionError(errorMessageOnFail)
        }
        return this
    }

    /**
     * Asserts that the current semantics node is not placed.
     *
     * Throws [AssertionError] if the node is placed.
     */
    private fun SemanticsNodeInteraction.assertNotPlaced() {
        // TODO(b/187188981): We don't have a non-throwing API to check whether an item exists.
        //  So until this bug is fixed, we are going to catch the assertion error and then check
        //  whether the node is placed or not.
        try {
            // If the node does not exist, it implies that it is also not placed.
            assertDoesNotExist()
        } catch (e: AssertionError) {
            // If the node exists, we need to assert that it is not placed.
            val errorMessageOnFail = "Assert failed: The component is placed!"
            if (fetchSemanticsNode().layoutInfo.isPlaced) {
                throw AssertionError(errorMessageOnFail)
            }
        }
    }
}
