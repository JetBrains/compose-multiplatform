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

package androidx.compose.ui

import android.os.Bundle
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeProvider
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BaseTextField
import androidx.compose.foundation.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.test.android.createAndroidComposeRule
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.assertTextEquals
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import androidx.ui.test.performSemanticsAction
import androidx.ui.test.runOnIdle
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.internal.matchers.apachecommons.ReflectionEquals

@MediumTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class AndroidAccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>(false, true)

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: ViewGroup
    private lateinit var delegate: AndroidComposeViewAccessibilityDelegateCompat

    private val argument = ArgumentCaptor.forClass(AccessibilityEvent::class.java)
    private var isTextFieldVisible by mutableStateOf(true)

    companion object {
        private const val ToggleableTag = "toggleable"
        private const val TextFieldTag = "textField"
        private const val InputText = "hello"
        private const val InitialText = "h"
    }

    @Before
    fun setup() {
        // Use uiAutomation to enable accessibility manager.
        InstrumentationRegistry.getInstrumentation().uiAutomation

        rule.activityRule.scenario.onActivity { activity ->
            container = spy(FrameLayout(activity)) {
                on { onRequestSendAccessibilityEvent(any(), any()) } doReturn false
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            activity.setContentView(container)
            container.setContent(Recomposer.current()) {
                var checked by remember { mutableStateOf(true) }
                var value by remember { mutableStateOf(TextFieldValue(InitialText)) }
                Column {
                    Box(
                        Modifier
                            .toggleable(value = checked, onValueChange = { checked = it })
                            .testTag(ToggleableTag),
                        children = {
                            Text("ToggleableText")
                        }
                    )
                    if (isTextFieldVisible) {
                        BaseTextField(
                            modifier = Modifier.testTag(TextFieldTag),
                            value = value,
                            onValueChange = { value = it }
                        )
                    }
                }
            }
            androidComposeView = container.getChildAt(0) as AndroidComposeView
            delegate = ViewCompat.getAccessibilityDelegate(androidComposeView) as
                    AndroidComposeViewAccessibilityDelegateCompat
        }
    }

    @Test
    fun sendStateChangeEvent_whenClickToggleable() {
        onNodeWithTag(ToggleableTag)
            .assertIsOn()
            .performClick()
            .assertIsOff()

        val toggleableNode = onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")

        val stateEvent = delegate.createEvent(
            toggleableNode.id,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )
        stateEvent.contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION

        runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            assertTrue(containsEvent(values, stateEvent))
        }
    }

    @Test
    fun sendTextEvents_whenSetText() {
        onNodeWithTag(TextFieldTag)
            .assertTextEquals(InitialText)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(InputText)) }
        onNodeWithTag(TextFieldTag)
            .assertTextEquals(InputText)

        val textFieldNode = onNodeWithTag(TextFieldTag)
            .fetchSemanticsNode("couldn't find node with tag $TextFieldTag")

        val textEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        textEvent.fromIndex = InitialText.length
        textEvent.removedCount = 0
        textEvent.addedCount = InputText.length - InitialText.length
        textEvent.beforeText = InitialText
        textEvent.text.add(InputText)

        val selectionEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        )
        selectionEvent.fromIndex = InputText.length
        selectionEvent.toIndex = InputText.length
        selectionEvent.itemCount = InputText.length
        selectionEvent.text.add(InputText)

        runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            assertTrue(containsEvent(values, textEvent))
            assertTrue(containsEvent(values, selectionEvent))
        }
    }

    @Test
    fun sendSubtreeChangeEvents_whenNodeRemoved() {
        onNodeWithTag(TextFieldTag)
            .assertExists()
        // TextField is removed compared to setup.
        isTextFieldVisible = false
        onNodeWithTag(TextFieldTag)
            .assertDoesNotExist()

        runOnIdle {
            // One from initialization and one from text field removal.
            verify(container, atLeast(2)).requestSendAccessibilityEvent(eq(androidComposeView),
                argThat(ArgumentMatcher {
                    it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                })
            )
        }
    }

    @Test
    fun traverseEventBeforeSelectionEvent_whenTraverseTextField() {
        val textFieldNode = onNodeWithTag(TextFieldTag)
            .fetchSemanticsNode("couldn't find node with tag $TextFieldTag")

        val args = Bundle()
        args.putInt(
            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
        )
        args.putBoolean(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, false)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView).provider as
                AccessibilityNodeProvider
        provider.performAction(
            textFieldNode.id,
            AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
            args
        )

        val selectionEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        )
        selectionEvent.fromIndex = InitialText.length
        selectionEvent.toIndex = InitialText.length
        selectionEvent.itemCount = InitialText.length
        selectionEvent.text.add(InitialText)

        val traverseEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
        )
        traverseEvent.fromIndex = 0
        traverseEvent.toIndex = 1
        traverseEvent.action = AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
        traverseEvent.movementGranularity =
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
        traverseEvent.text.add(InitialText)

        runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            // Note right now the event ordering is incorrect. The ordering in test needs to be
            // changed when the event ordering if fixed.
            val traverseEventIndex = eventIndex(values, traverseEvent)
            val selectionEventIndex = eventIndex(values, selectionEvent)
            assertNotEquals(-1, traverseEventIndex)
            assertNotEquals(-1, selectionEventIndex)
            assertTrue(traverseEventIndex < selectionEventIndex)
        }
    }

    private fun eventIndex(list: List<AccessibilityEvent>, event: AccessibilityEvent): Int {
        for (i in list.indices) {
            if (ReflectionEquals(list[i], null).matches(event)) {
                return i
            }
        }
        return -1
    }

    private fun containsEvent(list: List<AccessibilityEvent>, event: AccessibilityEvent): Boolean {
        return eventIndex(list, event) != -1
    }
}
