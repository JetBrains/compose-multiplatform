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

import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import android.view.accessibility.AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
import android.view.accessibility.AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_SELECTION
import android.view.accessibility.AccessibilityNodeProvider
import android.view.accessibility.AccessibilityRecord
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.internal.matchers.apachecommons.ReflectionEquals

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class AndroidAccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: OpenComposeView
    private lateinit var delegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var provider: AccessibilityNodeProvider

    private val argument = ArgumentCaptor.forClass(AccessibilityEvent::class.java)

    @Before
    fun setup() {
        // Use uiAutomation to enable accessibility manager.
        InstrumentationRegistry.getInstrumentation().uiAutomation

        rule.activityRule.scenario.onActivity { activity ->
            container = spy(OpenComposeView(activity)) {
                on { onRequestSendAccessibilityEvent(any(), any()) } doReturn false
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            activity.setContentView(container)

            androidComposeView = container.getChildAt(0) as AndroidComposeView
            delegate = ViewCompat.getAccessibilityDelegate(androidComposeView) as
                AndroidComposeViewAccessibilityDelegateCompat
            delegate.accessibilityForceEnabledForTesting = true
            provider = delegate.getAccessibilityNodeProvider(androidComposeView).provider
                as AccessibilityNodeProvider
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forToggleable() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)
        assertEquals("android.view.View", accessibilityNodeInfo.className)
        val stateDescription = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                accessibilityNodeInfo.stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                accessibilityNodeInfo.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertEquals("Checked", stateDescription)
        assertTrue(accessibilityNodeInfo.isClickable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forTextField() {
        val tag = "TextField"
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue("hello")) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(textFieldNode.id)

        assertEquals("android.widget.EditText", accessibilityNodeInfo.className)
        assertEquals("hello", accessibilityNodeInfo.text.toString())
        assertTrue(accessibilityNodeInfo.isFocusable)
        assertFalse(accessibilityNodeInfo.isFocused)
        assertTrue(accessibilityNodeInfo.isEditable)
        assertTrue(accessibilityNodeInfo.isVisibleToUser)
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_SET_SELECTION, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(ACTION_NEXT_AT_MOVEMENT_GRANULARITY, null)
            )
        )
        assertTrue(
            accessibilityNodeInfo.actionList.contains(
                AccessibilityNodeInfo.AccessibilityAction(
                    ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, null
                )
            )
        )
        assertEquals(
            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER or
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD or
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH or
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE or
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE,
            accessibilityNodeInfo.movementGranularities
        )
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(
                listOf(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY),
                accessibilityNodeInfo.availableExtraData
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun reportedTexts_inTextFieldWithLabel_whenEditableTextNotEmpty() {
        val tag = "TextField"

        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "hello",
                onValueChange = {},
                decorationBox = {
                    BasicText("Label")
                    it()
                }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(textFieldNode.id)

        assertEquals("hello", accessibilityNodeInfo.text.toString())
        assertEquals("Label", accessibilityNodeInfo.hintText.toString())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun reportedText_inTextFieldWithLabel_whenEditableTextEmpty() {
        val tag = "TextField"
        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "",
                onValueChange = {},
                decorationBox = {
                    BasicText("Label")
                    it()
                }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val accessibilityNodeInfo = provider.createAccessibilityNodeInfo(textFieldNode.id)

        assertEquals("Label", accessibilityNodeInfo.text.toString())
        assertEquals(true, accessibilityNodeInfo.isShowingHintText)
    }

    @Test
    fun testPerformAction_succeedOnEnabledNodes() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsOn()

        waitForSubtreeEventToSend()
        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(toggleableNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(tag)
            .assertIsOff()
    }

    @Test
    fun testPerformAction_failOnDisabledNodes() {
        val tag = "DisabledToggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(
                        value = checked,
                        enabled = false,
                        onValueChange = { checked = it }
                    )
                    .testTag(tag),
                content = {
                    BasicText("ToggleableText")
                }
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsOn()

        waitForSubtreeEventToSend()
        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        rule.runOnUiThread {
            assertFalse(provider.performAction(toggleableNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(tag)
            .assertIsOn()
    }

    @Test
    fun testTextField_performClickAction_succeedOnEnabledNode() {
        val tag = "TextField"
        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "value",
                onValueChange = {}
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")

        rule.runOnUiThread {
            assertTrue(provider.performAction(textFieldNode.id, ACTION_CLICK, null))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
    }

    @Test
    fun testTextField_performSetSelectionAction_succeedOnEnabledNode() {
        val tag = "TextField"
        var textFieldSelectionOne = false
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue("hello")) }
            BasicTextField(
                modifier = Modifier
                    .semantics {
                        // Make sure this block will be executed when selection changes.
                        this.textSelectionRange = value.selection
                        if (value.selection == TextRange(1)) {
                            textFieldSelectionOne = true
                        }
                    }
                    .testTag(tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 1)
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 1)

        rule.runOnUiThread {
            textFieldSelectionOne = false
            assertTrue(provider.performAction(textFieldNode.id, ACTION_SET_SELECTION, argument))
        }
        rule.waitUntil(5_000) { textFieldSelectionOne }

        rule.onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(1)
                )
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testAddExtraDataToAccessibilityNodeInfo_notMerged() {
        val tag = "TextField"
        lateinit var textLayoutResult: TextLayoutResult

        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "texy",
                onValueChange = {},
                onTextLayout = { textLayoutResult = it }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val info = AccessibilityNodeInfo.obtain()
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
        argument.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, 1)
        provider.addExtraDataToAccessibilityNodeInfo(
            textFieldNode.id,
            info,
            AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
            argument
        )
        val data = info.extras
            .getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
        assertEquals(1, data!!.size)
        val rectF = data[0] as RectF
        val expectedRect = textLayoutResult.getBoundingBox(0).translate(
            textFieldNode.positionInWindow
        )
        assertEquals(expectedRect.left, rectF.left)
        assertEquals(expectedRect.top, rectF.top)
        assertEquals(expectedRect.right, rectF.right)
        assertEquals(expectedRect.bottom, rectF.bottom)
    }

    // This test needs to be improved after text merging(b/157474582) is fixed.
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testAddExtraDataToAccessibilityNodeInfo_merged() {
        val tag = "MergedText"
        val textOne = "hello"
        val textTwo = "world"
        lateinit var textLayoutResult: TextLayoutResult

        container.setContent {
            Column(modifier = Modifier.testTag(tag).semantics(true) {}) {
                BasicText(text = textOne, onTextLayout = { textLayoutResult = it })
                BasicText(text = textTwo)
            }
        }

        val textNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val info = AccessibilityNodeInfo.obtain()
        val argument = Bundle()
        val length = textOne.length + textTwo.length
        argument.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
        argument.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, length)
        provider.addExtraDataToAccessibilityNodeInfo(
            textNode.id,
            info,
            AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
            argument
        )
        val data = info.extras
            .getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
        assertEquals(length, data!!.size)
        val rectF = data[0] as RectF
        val expectedRect = textLayoutResult.getBoundingBox(0).translate(
            textNode.positionInWindow
        )
        assertEquals(expectedRect.left, rectF.left)
        assertEquals(expectedRect.top, rectF.top)
        assertEquals(expectedRect.right, rectF.right)
        assertEquals(expectedRect.bottom, rectF.bottom)
    }

    @Test
    fun sendStateChangeEvent_whenClickToggleable() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsOn()

        waitForSubtreeEventToSend()
        rule.onNodeWithTag(tag)
            .performClick()
            .assertIsOff()

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")

        val stateEvent = delegate.createEvent(
            toggleableNode.id,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )
        stateEvent.contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION

        rule.runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            assertTrue(containsEvent(values, stateEvent))
        }
    }

    @Test
    fun sendTextEvents_whenSetText() {
        val tag = "TextField"
        val initialText = "h"
        val text = "hello"
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue(initialText)) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString(initialText)
                )
            )

        waitForSubtreeEventToSend()
        rule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(text)) }
        rule.onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString(text)
                )
            )

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")

        val textEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        textEvent.fromIndex = initialText.length
        textEvent.removedCount = 0
        textEvent.addedCount = text.length - initialText.length
        textEvent.beforeText = initialText
        textEvent.text.add(text)

        val selectionEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        )
        selectionEvent.fromIndex = text.length
        selectionEvent.toIndex = text.length
        selectionEvent.itemCount = text.length
        selectionEvent.text.add(text)

        rule.runOnIdle {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView), argument.capture()
            )
            val values = argument.allValues
            assertTrue(containsEvent(values, textEvent))
            assertTrue(containsEvent(values, selectionEvent))
        }
    }

    @Test
    @Ignore("b/177656801")
    fun sendSubtreeChangeEvents_whenNodeRemoved() {
        val columnTag = "topColumn"
        val textFieldTag = "TextFieldTag"
        var isTextFieldVisible by mutableStateOf(true)

        container.setContent {
            Column(Modifier.testTag(columnTag)) {
                if (isTextFieldVisible) {
                    BasicTextField(
                        modifier = Modifier.testTag(textFieldTag),
                        value = "text",
                        onValueChange = {}
                    )
                }
            }
        }

        val parentNode = rule.onNodeWithTag(columnTag)
            .fetchSemanticsNode("couldn't find node with tag $columnTag")
        rule.onNodeWithTag(textFieldTag)
            .assertExists()
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == parentNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        // TextField is removed compared to setup.
        isTextFieldVisible = false

        rule.onNodeWithTag(textFieldTag)
            .assertDoesNotExist()
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == parentNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    @Ignore("b/178524529")
    fun traverseEventBeforeSelectionEvent_whenTraverseTextField() {
        val tag = "TextFieldTag"
        val text = "h"
        container.setContent {
            var value by remember { mutableStateOf(TextFieldValue(text)) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it },
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = {
                    BasicText("Label")
                    it()
                }
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .fetchSemanticsNode("couldn't find node with tag $tag")

        waitForSubtreeEventToSend()
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
        selectionEvent.fromIndex = text.length
        selectionEvent.toIndex = text.length
        selectionEvent.itemCount = text.length
        selectionEvent.text.add(text)

        val traverseEvent = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
        )
        traverseEvent.fromIndex = 0
        traverseEvent.toIndex = 1
        traverseEvent.action = AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
        traverseEvent.movementGranularity =
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
        traverseEvent.text.add(text)

        rule.runOnIdle {
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

    @Test
    @Ignore("b/177656801")
    fun semanticsNodeBeingMergedLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
                Box {
                    BasicText("TextNode")
                }
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val textNode = rule.onNodeWithText("TextNode", useUnmergedTree = true)
            .fetchSemanticsNode("couldn't find node with text TextNode")
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        rule.runOnUiThread {
            // Directly call onLayoutChange because this guarantees short time.
            for (i in 1..10) {
                delegate.onLayoutChange(textNode.layoutNode)
            }
        }

        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    @Ignore("b/177656801")
    fun layoutNodeWithoutSemanticsLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
                Box {
                    BasicText("TextNode")
                }
            }
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val textNode = rule.onNodeWithText("TextNode", useUnmergedTree = true)
            .fetchSemanticsNode("couldn't find node with text TextNode")
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        rule.runOnUiThread {
            // Directly call onLayoutChange because this guarantees short time.
            for (i in 1..10) {
                // layout change for the parent box node
                delegate.onLayoutChange(textNode.layoutNode.parent!!)
            }
        }

        waitForSubtreeEventToSendAndVerify {
            // One from initialization and one from layout changes.
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == toggleableNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    fun testGetVirtualViewAt() {
        val tag = "Toggleable"
        container.setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier
                    .toggleable(value = checked, onValueChange = { checked = it })
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }

        var rootNodeBoundsLeft = 0f
        var rootNodeBoundsTop = 0f
        rule.runOnIdle {
            val rootNode = androidComposeView.semanticsOwner.rootSemanticsNode
            rootNodeBoundsLeft = rootNode.boundsInWindow.left
            rootNodeBoundsTop = rootNode.boundsInWindow.top
        }

        val toggleableNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val toggleableNodeBounds = toggleableNode.boundsInWindow

        val toggleableNodeId = delegate.getVirtualViewAt(
            (toggleableNodeBounds.left + toggleableNodeBounds.right) / 2 - rootNodeBoundsLeft,
            (toggleableNodeBounds.top + toggleableNodeBounds.bottom) / 2 - rootNodeBoundsTop
        )

        assertEquals(toggleableNode.id, toggleableNodeId)
    }

    @Test
    fun testGetVirtualViewAt_overlappedChildren() {
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        container.setContent {
            Box {
                BasicText(
                    "Child One",
                    Modifier
                        .zIndex(1f)
                        .testTag(childOneTag)
                        .requiredSize(50.dp)
                )
                BasicText(
                    "Child Two",
                    Modifier
                        .testTag(childTwoTag)
                        .requiredSize(50.dp)
                )
            }
        }

        var rootNodeBoundsLeft = 0f
        var rootNodeBoundsTop = 0f
        rule.runOnIdle {
            val rootNode = androidComposeView.semanticsOwner.rootSemanticsNode
            rootNodeBoundsLeft = rootNode.boundsInWindow.left
            rootNodeBoundsTop = rootNode.boundsInWindow.top
        }

        val overlappedChildOneNode = rule.onNodeWithTag(childOneTag)
            .fetchSemanticsNode("couldn't find node with tag $childOneTag")
        val overlappedChildTwoNode = rule.onNodeWithTag(childTwoTag)
            .fetchSemanticsNode("couldn't find node with tag $childTwoTag")
        val overlappedChildNodeBounds = overlappedChildTwoNode.boundsInWindow
        val overlappedChildNodeId = delegate.getVirtualViewAt(
            (overlappedChildNodeBounds.left + overlappedChildNodeBounds.right) / 2 -
                rootNodeBoundsLeft,
            (overlappedChildNodeBounds.top + overlappedChildNodeBounds.bottom) / 2 -
                rootNodeBoundsTop
        )

        assertEquals(overlappedChildOneNode.id, overlappedChildNodeId)
        assertNotEquals(overlappedChildTwoNode.id, overlappedChildNodeId)
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned() {
        val parentTag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        container.setContent {
            Box(Modifier.testTag(parentTag)) {
                BasicText(
                    "Child One",
                    Modifier
                        .zIndex(1f)
                        .testTag(childOneTag)
                        .requiredSize(50.dp)
                )
                BasicText(
                    "Child Two",
                    Modifier
                        .testTag(childTwoTag)
                        .requiredSize(50.dp)
                )
            }
        }

        val parentNode = rule.onNodeWithTag(parentTag)
            .fetchSemanticsNode("couldn't find node with tag $parentTag")
        val overlappedChildOneNode = rule.onNodeWithTag(childOneTag)
            .fetchSemanticsNode("couldn't find node with tag $childOneTag")
        val overlappedChildTwoNode = rule.onNodeWithTag(childTwoTag)
            .fetchSemanticsNode("couldn't find node with tag $childTwoTag")

        assertEquals(1, provider.createAccessibilityNodeInfo(parentNode.id).childCount)
        assertEquals(
            "Child One",
            provider.createAccessibilityNodeInfo(overlappedChildOneNode.id).text.toString()
        )
        assertNull(provider.createAccessibilityNodeInfo(overlappedChildTwoNode.id))
    }

    @Test
    fun testPaneAppear() {
        val paneTag = "Pane"
        var isPaneVisible by mutableStateOf(false)
        val paneTestTitle by mutableStateOf("pane title")

        container.setContent {
            if (isPaneVisible) {
                Box(
                    Modifier
                        .testTag(paneTag)
                        .semantics { paneTitle = paneTestTitle }
                ) {}
            }
        }

        rule.onNodeWithTag(paneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()
        val paneNode = rule.onNodeWithTag(paneTag).fetchSemanticsNode()
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == paneNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED
                    }
                )
            )
        }
    }

    @Test
    fun testPaneTitleChange() {
        val paneTag = "Pane"
        var isPaneVisible by mutableStateOf(false)
        var paneTestTitle by mutableStateOf("pane title")

        container.setContent {
            if (isPaneVisible) {
                Box(
                    Modifier
                        .testTag(paneTag)
                        .semantics { paneTitle = paneTestTitle }
                ) {}
            }
        }

        rule.onNodeWithTag(paneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()

        paneTestTitle = "new pane title"
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "new pane title"
                )
            )
        val paneNode = rule.onNodeWithTag(paneTag).fetchSemanticsNode()
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == paneNode.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE
                    }
                )
            )
        }
    }

    @Test
    fun testPaneDisappear() {
        val paneTag = "Pane"
        var isPaneVisible by mutableStateOf(false)
        val paneTestTitle by mutableStateOf("pane title")

        container.setContent {
            if (isPaneVisible) {
                Box(Modifier.testTag(paneTag).semantics { paneTitle = paneTestTitle }) {}
            }
        }

        rule.onNodeWithTag(paneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.onNodeWithTag(paneTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "pane title"
                )
            )
            .assertIsDisplayed()
        waitForSubtreeEventToSend()

        isPaneVisible = false
        rule.onNodeWithTag(paneTag).assertDoesNotExist()
        rule.runOnIdle {
            verify(container, times(1)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                            it.contentChangeTypes ==
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
                    }
                )
            )
        }
    }

    @Test
    fun testEventForPasswordTextField() {
        val tag = "TextField"
        container.setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "value",
                onValueChange = {},
                visualTransformation = PasswordVisualTransformation()
            )
        }

        val textFieldNode = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("Couldn't fetch node with tag $tag")
        val event = delegate.createEvent(
            textFieldNode.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )

        assertTrue(event.isPassword)
    }

    @Test
    fun testDialog_setCorrectBounds() {
        var dialogComposeView: AndroidComposeView? = null
        container.setContent {
            Dialog(onDismissRequest = {}) {
                dialogComposeView = LocalView.current as AndroidComposeView
                delegate = ViewCompat.getAccessibilityDelegate(dialogComposeView!!) as
                    AndroidComposeViewAccessibilityDelegateCompat

                Box(Modifier.size(300.dp)) {
                    BasicText(
                        text = "text",
                        modifier = Modifier.offset(10.dp, 10.dp).fillMaxSize()
                    )
                }
            }
        }

        val textNode = rule.onNodeWithText("text").fetchSemanticsNode()
        val info = AccessibilityNodeInfoCompat.obtain()
        delegate.populateAccessibilityNodeInfoProperties(
            textNode.id,
            info,
            textNode
        )

        val viewPosition = intArrayOf(0, 0)
        dialogComposeView!!.getLocationOnScreen(viewPosition)
        with(rule.density) {
            val offset = 10.dp.roundToPx()
            val size = 300.dp.roundToPx()
            val textPositionOnScreenX = viewPosition[0] + offset
            val textPositionOnScreenY = viewPosition[1] + offset

            val textRect = android.graphics.Rect()
            info.getBoundsInScreen(textRect)
            assertEquals(
                android.graphics.Rect(
                    textPositionOnScreenX,
                    textPositionOnScreenY,
                    textPositionOnScreenX + size,
                    textPositionOnScreenY + size
                ),
                textRect
            )
        }
    }

    @Test
    fun testContentDescription_notMergingDescendants_withOwnContentDescription() {
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics { contentDescription = "Column" }.testTag(tag)) {
                BasicText("Text")
                Box(Modifier.size(100.dp).semantics { contentDescription = "Box" })
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)

        assertEquals("Column", info.contentDescription)
    }

    @Test
    fun testContentDescription_mergingDescendants_withOwnContentDescription() {
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics(true) { contentDescription = "Column" }.testTag(tag)) {
                BasicText("Text")
                Box(Modifier.size(100.dp).semantics { contentDescription = "Box" })
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)

        assertEquals("Column", info.contentDescription)
    }

    @Test
    fun testContentDescription_notMergingDescendants_withoutOwnContentDescription() {
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics {}.testTag(tag)) {
                BasicText("Text")
                Box(Modifier.size(100.dp).semantics { contentDescription = "Box" })
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)

        assertEquals(null, info.contentDescription)
    }

    @Test
    fun testContentDescription_mergingDescendants_withoutOwnContentDescription() {
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics(true) {}.testTag(tag)) {
                BasicText("Text")
                Box(Modifier.size(100.dp).semantics { contentDescription = "Box" })
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)

        assertEquals("Text, Box", info.contentDescription)
    }

    @Test
    fun testContentDescription_mergingDescendants() {
        // This is a bit more complex example
        val tag = "Column"
        container.setContent {
            Column(Modifier.semantics(true) {}.testTag(tag)) {
                Column(Modifier.semantics(true) { contentDescription = "Column1" }) {
                    BasicText("Text1")
                    Row(Modifier.semantics {}) {
                        Box(Modifier.size(100.dp).semantics { contentDescription = "Box1" })
                        Box(Modifier.size(100.dp).semantics { contentDescription = "Box2" })
                    }
                }
                Column(Modifier.semantics {}) {
                    BasicText("Text2")
                    Row(Modifier.semantics(true) {}) {
                        Box(Modifier.size(100.dp).semantics { contentDescription = "Box3" })
                        Box(Modifier.size(100.dp).semantics { contentDescription = "Box4" })
                    }
                }
                Column(Modifier.semantics { }) {
                    BasicText("Text3")
                    Row(Modifier.semantics {}) {
                        Box(Modifier.size(100.dp).semantics { contentDescription = "Box5" })
                        Box(Modifier.size(100.dp).semantics { contentDescription = "Box6" })
                    }
                }
            }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)

        assertEquals("Text2, Text3, Box5, Box6", info.contentDescription)
    }

    @Test
    fun testRole_doesNotMerge() {
        container.setContent {
            Row(Modifier.semantics(true) {}.testTag("Row")) {
                Box(Modifier.size(100.dp).semantics { role = Role.Button })
                Box(Modifier.size(100.dp).semantics { role = Role.Image })
            }
        }

        val node = rule.onNodeWithTag("Row").fetchSemanticsNode()
        val info = provider.createAccessibilityNodeInfo(node.id)

        assertEquals(AndroidComposeViewAccessibilityDelegateCompat.ClassName, info.className)
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

    private fun getAccessibilityEventSourceSemanticsNodeId(event: AccessibilityEvent): Int {
        val getSourceNodeIdMethod = AccessibilityRecord::class.java
            .getDeclaredMethod("getSourceNodeId")
        getSourceNodeIdMethod.isAccessible = true
        return (getSourceNodeIdMethod.invoke(event) as Long shr 32).toInt()
    }

    private fun waitForSubtreeEventToSendAndVerify(verify: () -> Unit) {
        // TODO(aelias): Make this wait after the 100ms delay to check the second batch is also correct
        rule.waitForIdle()
        verify()
    }

    private fun waitForSubtreeEventToSend() {
        // When the subtree events are sent, we will also update our previousSemanticsNodes,
        // which will affect our next accessibility events from semantics tree comparison.
        rule.mainClock.advanceTimeBy(5000)
        rule.waitForIdle()
    }
}
