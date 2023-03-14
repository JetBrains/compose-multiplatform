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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.PinnableContainer.PinnedHandle
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusableTest {

    @get:Rule
    val rule = createComposeRule()

    private val focusTag = "myFocusable"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun focusable_defaultSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusable()
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assertIsEnabled()
            .assert(isFocusable())
    }

    @Test
    fun focusable_disabledSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusable(enabled = false)
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assert(isNotFocusable())
    }

    @Test
    fun focusable_focusAcquire() {
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        rule.setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(focusRequester)
                        .focusable()
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier
                        .focusRequester(otherFocusRequester)
                        .focusable()
                )
            }
        }

        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag(focusTag)
            .assertIsFocused()

        rule.runOnIdle {
            otherFocusRequester.requestFocus()
        }

        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()
    }

    @Test
    fun focusable_interactionSource() {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier
                        .focusRequester(otherFocusRequester)
                        .focusable()
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle {
            otherFocusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1])
                .isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var emitFocusableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitFocusableText) {
                    BasicText(
                        "focusableText",
                        modifier = Modifier
                            .testTag(focusTag)
                            .focusRequester(focusRequester)
                            .focusable(interactionSource = interactionSource)
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Dispose focusable, Interaction should be gone
        rule.runOnIdle {
            emitFocusableText = false
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun focusable_pins_whenItIsFocused() {
        // Arrange.
        val focusRequester = FocusRequester()
        var onPinInvoked = false
        val pinnableContainer = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                onPinInvoked = true
                return PinnedHandle {}
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalPinnableContainer provides pinnableContainer) {
                Box(
                    Modifier
                        .size(100.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(onPinInvoked).isTrue()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun focusable_unpins_whenItIsUnfocused() {
        // Arrange.
        val focusRequester = FocusRequester()
        val focusRequester2 = FocusRequester()
        var onUnpinInvoked = false
        val pinnableContainer = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                return PinnedHandle { onUnpinInvoked = true }
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalPinnableContainer provides pinnableContainer) {
                Box(
                    Modifier
                        .size(100.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
            Box(
                Modifier
                    .size(100.dp)
                    .focusRequester(focusRequester2)
                    .focusable()
            )
        }

        // Act.
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }
        rule.runOnIdle {
            assertThat(onUnpinInvoked).isFalse()
            focusRequester2.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(onUnpinInvoked).isTrue()
        }
    }

    @Test
    fun focusable_inspectorValue() {
        val modifier = Modifier.focusable() as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("focusable")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.map { it.name }.asIterable())
            .containsExactly(
                "enabled",
                "interactionSource"
            )
    }

    @Test
    fun focusable_requestsBringIntoView_whenFocused() {
        // Arrange.
        val requestedRects = mutableListOf<Rect?>()
        val bringIntoViewResponder = object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                requestedRects += localRect()
            }
        }
        val focusRequester = FocusRequester()

        rule.setContent {
            with(rule.density) {
                Box(
                    Modifier
                        .bringIntoViewResponder(bringIntoViewResponder)
                        .focusRequester(focusRequester)
                        .focusable()
                        // Needs a non-zero size.
                        .size(1f.toDp())
                )
            }
        }

        rule.runOnIdle {
            assertThat(requestedRects).isEmpty()
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRects).containsExactly(Rect(Offset.Zero, Size(1f, 1f)))
        }
    }
    // This test also verifies that the internal API autoInvalidateRemovedNode()
    // is called when a modifier node is disposed.
    @Test
    fun removingFocusableFromLazyList_clearsFocus() {
        // Arrange.
        var lazyRowHasFocus = false
        lateinit var state: LazyListState
        lateinit var coroutineScope: CoroutineScope
        var items by mutableStateOf((1..20).toList())
        rule.setContent {
            state = rememberLazyListState()
            coroutineScope = rememberCoroutineScope()
            LazyRow(
                modifier = Modifier
                    .requiredSize(100.dp)
                    .onFocusChanged { lazyRowHasFocus = it.hasFocus },
                state = state
            ) {
                items(items.size) {
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .testTag("$it")
                            .focusable())
                }
            }
        }
        rule.runOnIdle { coroutineScope.launch { state.scrollToItem(19) } }
        rule.onNodeWithTag("19").performSemanticsAction(SemanticsActions.RequestFocus)

        // Act.
        rule.runOnIdle { items = (1..11).toList() }

        // Assert.
        rule.runOnIdle {
            assertThat(lazyRowHasFocus).isFalse()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun focusable_updatePinnableContainer_staysPinned() {
        // Arrange.
        val focusRequester = FocusRequester()
        var container1Pinned = false
        val pinnableContainer1 = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                container1Pinned = true
                return PinnedHandle { container1Pinned = false }
            }
        }
        var container2Pinned = false
        val pinnableContainer2 = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                container2Pinned = true
                return PinnedHandle { container2Pinned = false }
            }
        }
        var pinnableContainer by mutableStateOf<PinnableContainer>(pinnableContainer1)
        rule.setContent {
            CompositionLocalProvider(LocalPinnableContainer provides pinnableContainer) {
                Box(
                    Modifier
                        .size(100.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }
        rule.runOnIdle {
            assertThat(container1Pinned).isTrue()
            assertThat(container2Pinned).isFalse()
            pinnableContainer = pinnableContainer2
        }

        // Assert.
        rule.runOnIdle {
            assertThat(container1Pinned).isFalse()
            assertThat(container2Pinned).isTrue()
        }
    }

    @Test
    fun reusingFocusedItem_itemIsNotFocusedAnymore() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var state: FocusState
        var key by mutableStateOf(0)
        rule.setContent {
            ReusableContent(key) {
                BasicText(
                    "focusableText",
                    modifier = Modifier
                        .testTag(focusTag)
                        .focusRequester(focusRequester)
                        .onFocusEvent { state = it }
                        .focusable()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(state.isFocused).isTrue()
        }
        rule.onNodeWithTag(focusTag)
            .assertIsFocused()

        // Act.
        rule.runOnIdle {
            key = 1
        }

        // Assert.
        rule.runOnIdle {
            assertThat(state.isFocused).isFalse()
        }
        rule.onNodeWithTag(focusTag)
            .assertIsNotFocused()
    }
}
