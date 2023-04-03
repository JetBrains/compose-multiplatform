/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
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
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class FocusableTest {
    private val focusTag = "myFocusable"

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun focusable_defaultSemantics() = runSkikoComposeUiTest {
        setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier.testTag(focusTag).focusable()
                )
            }
        }

        onNodeWithTag(focusTag)
            .assertIsEnabled()
            .assert(isFocusable())
    }

    @Test
    fun focusable_disabledSemantics() = runSkikoComposeUiTest {
        setContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier.testTag(focusTag).focusable(enabled = false)
                )
            }
        }

        onNodeWithTag(focusTag)
            .assert(isNotFocusable())
    }

    @Test
    fun focusable_focusAcquire() = runSkikoComposeUiTest {
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        setContent {
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

        onNodeWithTag(focusTag)
            .assertIsNotFocused()

        runOnIdle {
            focusRequester.requestFocus()
        }

        onNodeWithTag(focusTag)
            .assertIsFocused()

        runOnIdle {
            otherFocusRequester.requestFocus()
        }

        onNodeWithTag(focusTag)
            .assertIsNotFocused()
    }

    @Test
    fun focusable_interactionSource() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        lateinit var scope: CoroutineScope

        setContent {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is FocusInteraction.Focus }
        }

        runOnIdle {
            otherFocusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is FocusInteraction.Focus }
            assertTrue { interactions[1] is FocusInteraction.Unfocus }
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusable_interactionSource_resetWhenDisposed() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var emitFocusableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        setContent {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is FocusInteraction.Focus }
        }

        // Dispose focusable, Interaction should be gone
        runOnIdle {
            emitFocusableText = false
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is FocusInteraction.Focus }
            assertTrue { interactions[1] is FocusInteraction.Unfocus }
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun focusable_pins_whenItIsFocused() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        var onPinInvoked = false
        val pinnableContainer = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                onPinInvoked = true
                return PinnedHandle {}
            }
        }
        setContent {
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
        runOnUiThread {
            focusRequester.requestFocus()
        }

        // Assert.
        runOnIdle {
            assertThat(onPinInvoked).isTrue()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun focusable_unpins_whenItIsUnfocused() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        val focusRequester2 = FocusRequester()
        var onUnpinInvoked = false
        val pinnableContainer = object : PinnableContainer {
            override fun pin(): PinnedHandle {
                return PinnedHandle { onUnpinInvoked = true }
            }
        }
        setContent {
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
        runOnUiThread {
            focusRequester.requestFocus()
        }
        runOnIdle {
            assertThat(onUnpinInvoked).isFalse()
            focusRequester2.requestFocus()
        }

        // Assert.
        runOnIdle {
            assertThat(onUnpinInvoked).isTrue()
        }
    }

    @Test
    fun focusable_inspectorValue() = runSkikoComposeUiTest {
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
    fun focusable_requestsBringIntoView_whenFocused() = runSkikoComposeUiTest {
        val requestedRects = mutableListOf<Rect?>()
        val bringIntoViewResponder = object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                requestedRects += localRect()
            }
        }
        val focusRequester = FocusRequester()

        setContent {
            with(density) {
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

        runOnIdle {
            assertThat(requestedRects).isEmpty()
            focusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(requestedRects).containsExactly(Rect(Offset.Zero, Size(1f, 1f)))
        }
    }

    // This test also verifies that the internal API autoInvalidateRemovedNode()
    // is called when a modifier node is disposed.
    @Test
    fun removingFocusableFromLazyList_clearsFocus() = runSkikoComposeUiTest {
        // Arrange.
        var lazyRowHasFocus = false
        lateinit var state: LazyListState
        lateinit var coroutineScope: CoroutineScope
        var items by mutableStateOf((1..20).toList())
        setContent {
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
                            .focusable()
                    )
                }
            }
        }
        runOnIdle { coroutineScope.launch { state.scrollToItem(19) } }
        onNodeWithTag("19").performSemanticsAction(SemanticsActions.RequestFocus)

        // Act.
        runOnIdle { items = (1..11).toList() }

        // Assert.
        runOnIdle {
            assertThat(lazyRowHasFocus).isFalse()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun focusable_updatePinnableContainer_staysPinned() = runSkikoComposeUiTest {
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
        setContent {
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
        runOnUiThread {
            focusRequester.requestFocus()
        }
        runOnIdle {
            assertThat(container1Pinned).isTrue()
            assertThat(container2Pinned).isFalse()
            pinnableContainer = pinnableContainer2
        }

        // Assert.
        runOnIdle {
            assertThat(container1Pinned).isFalse()
            assertThat(container2Pinned).isTrue()
        }
    }

    @Test
    fun reusingFocusedItem_itemIsNotFocusedAnymore() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var state: FocusState
        var key by mutableStateOf(0)
        setContent {
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
        runOnIdle {
            focusRequester.requestFocus()
            assertThat(state.isFocused).isTrue()
        }
        onNodeWithTag(focusTag)
            .assertIsFocused()

        // Act.
        runOnIdle {
            key = 1
        }

        // Assert.
        runOnIdle {
            assertThat(state.isFocused).isFalse()
        }
        onNodeWithTag(focusTag).assertIsNotFocused()
    }
}
