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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hasSize
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isEmpty
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isNull
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.lazy.layout.ModifierLocalPinnableParent
import androidx.compose.foundation.lazy.layout.PinnableParent
import androidx.compose.foundation.lazy.layout.PinnableParent.PinnedItemsHandle
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
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
    fun focusable_pinsAndUnpins_whenItIsFocused() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        var onPinInvoked = false
        var onUnpinInvoked = false
        setContent {
            Box(
                 Modifier
                     .size(100.dp)
                     .pinnableParent(
                         onPin = {
                             onPinInvoked = true
                             TestPinnedItemsHandle { onUnpinInvoked = true }
                         }
                     )
                     .focusRequester(focusRequester)
                     .focusable()
            )
        }

        // Act.
        runOnUiThread {
            focusRequester.requestFocus()
        }

        // Assert.
        runOnIdle {
            assertThat(onPinInvoked).isTrue()
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

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalFoundationApi
    private fun Modifier.pinnableParent(onPin: () -> PinnedItemsHandle): Modifier {
        return modifierLocalProvider(ModifierLocalPinnableParent) {
            object : PinnableParent {
                override fun pinItems(): PinnedItemsHandle {
                    return onPin()
                }
            }
        }
    }

    @ExperimentalFoundationApi
    private class TestPinnedItemsHandle(val onUnpin: () -> Unit) : PinnedItemsHandle {
        override fun unpin() {
            onUnpin.invoke()
        }
    }
}
