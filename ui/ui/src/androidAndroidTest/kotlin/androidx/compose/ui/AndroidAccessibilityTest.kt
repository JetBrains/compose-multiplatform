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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeApi::class
)
class AndroidAccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: OpenComposeView
    private lateinit var delegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var provider: AccessibilityNodeProvider
    private lateinit var textLayoutResult: TextLayoutResult

    private val argument = ArgumentCaptor.forClass(AccessibilityEvent::class.java)
    private var isTextFieldVisible by mutableStateOf(true)
    private var textFieldSelectionOneLatch = CountDownLatch(1)

    companion object {
        private const val TopColTag = "topColumn"
        private const val ToggleableTag = "toggleable"
        private const val DisabledToggleableTag = "disabledToggleable"
        private const val TextFieldTag = "textField"
        private const val TextNodeTag = "textNode"
        private const val OverlappedChildOneTag = "OverlappedChildOne"
        private const val OverlappedChildTwoTag = "OverlappedChildTwo"
        private const val InputText = "hello"
        private const val InitialText = "h"
    }

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
            container.setContent {
                var checked by remember { mutableStateOf(true) }
                var value by remember { mutableStateOf(TextFieldValue(InitialText)) }
                Column(Modifier.testTag(TopColTag)) {
                    Box(
                        Modifier
                            .toggleable(value = checked, onValueChange = { checked = it })
                            .testTag(ToggleableTag)
                    ) {
                        BasicText("ToggleableText")
                        Box {
                            BasicText("TextNode", Modifier.testTag(TextNodeTag))
                        }
                    }
                    Box(
                        Modifier
                            .toggleable(
                                value = checked,
                                enabled = false,
                                onValueChange = { checked = it }
                            )
                            .testTag(DisabledToggleableTag),
                        content = {
                            BasicText("ToggleableText")
                        }
                    )
                    Box {
                        BasicText("Child One", Modifier.zIndex(1f).testTag(OverlappedChildOneTag))
                        BasicText("Child Two", Modifier.testTag(OverlappedChildTwoTag))
                    }
                    if (isTextFieldVisible) {
                        BasicTextField(
                            modifier = Modifier
                                .semantics {
                                    // Make sure this block will be executed when selection changes.
                                    this.textSelectionRange = value.selection
                                    if (value.selection == TextRange(1)) {
                                        textFieldSelectionOneLatch.countDown()
                                    }
                                }
                                .testTag(TextFieldTag),
                            value = value,
                            onValueChange = { value = it },
                            onTextLayout = { textLayoutResult = it }
                        )
                    }
                }
            }
            androidComposeView = container.getChildAt(0) as AndroidComposeView
            delegate = ViewCompat.getAccessibilityDelegate(androidComposeView) as
                AndroidComposeViewAccessibilityDelegateCompat
            delegate.accessibilityForceEnabledForTesting = true
            provider = delegate.getAccessibilityNodeProvider(androidComposeView).provider
                as AccessibilityNodeProvider
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo() {
        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")
        var accessibilityNodeInfo = provider.createAccessibilityNodeInfo(toggleableNode.id)
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

        val textFieldNode = rule.onNodeWithTag(TextFieldTag)
            .fetchSemanticsNode("couldn't find node with tag $TextFieldTag")
        accessibilityNodeInfo = provider.createAccessibilityNodeInfo(textFieldNode.id)
        assertEquals("android.widget.EditText", accessibilityNodeInfo.className)
        assertEquals(InitialText, accessibilityNodeInfo.text.toString())
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
    fun testPerformAction_succeedOnEnabledNodes() {
        rule.onNodeWithTag(ToggleableTag)
            .assertIsOn()
        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(toggleableNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(ToggleableTag)
            .assertIsOff()

        val textFieldNode = rule.onNodeWithTag(TextFieldTag)
            .fetchSemanticsNode("couldn't find node with tag $TextFieldTag")
        rule.runOnUiThread {
            assertTrue(provider.performAction(textFieldNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(TextFieldTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 1)
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 1)
        rule.runOnUiThread {
            textFieldSelectionOneLatch = CountDownLatch(1)
            assertTrue(provider.performAction(textFieldNode.id, ACTION_SET_SELECTION, argument))
        }
        if (!textFieldSelectionOneLatch.await(5, TimeUnit.SECONDS)) {
            throw AssertionError("Failed to wait for text selection change.")
        }
        rule.onNodeWithTag(TextFieldTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(1)
                )
            )
    }

    @Test
    fun testPerformAction_failOnDisabledNodes() {
        rule.onNodeWithTag(DisabledToggleableTag)
            .assertIsOn()
        val toggleableNode = rule.onNodeWithTag(DisabledToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $DisabledToggleableTag")
        rule.runOnUiThread {
            assertFalse(provider.performAction(toggleableNode.id, ACTION_CLICK, null))
        }
        rule.onNodeWithTag(DisabledToggleableTag)
            .assertIsOn()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testAddExtraDataToAccessibilityNodeInfo() {
        val textFieldNode = rule.onNodeWithTag(TextFieldTag)
            .fetchSemanticsNode("couldn't find node with tag $TextFieldTag")
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
            textFieldNode
                .globalPosition
        )
        assertEquals(expectedRect.left, rectF.left)
        assertEquals(expectedRect.top, rectF.top)
        assertEquals(expectedRect.right, rectF.right)
        assertEquals(expectedRect.bottom, rectF.bottom)
    }

    @Test
    fun sendStateChangeEvent_whenClickToggleable() {
        rule.onNodeWithTag(ToggleableTag)
            .assertIsOn()
            .performClick()
            .assertIsOff()

        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")

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
        rule.onNodeWithTag(TextFieldTag)
            .assertTextEquals(InitialText)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(InputText)) }
        rule.onNodeWithTag(TextFieldTag)
            .assertTextEquals(InputText)

        val textFieldNode = rule.onNodeWithTag(TextFieldTag)
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
    fun sendSubtreeChangeEvents_whenNodeRemoved() {
        val topColumn = rule.onNodeWithTag(TopColTag)
            .fetchSemanticsNode("couldn't find node with tag $TopColTag")
        rule.onNodeWithTag(TextFieldTag)
            .assertExists()
        // wait for the subtree change events from initialization to send
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == topColumn.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }

        // TextField is removed compared to setup.
        isTextFieldVisible = false

        rule.onNodeWithTag(TextFieldTag)
            .assertDoesNotExist()
        waitForSubtreeEventToSendAndVerify {
            verify(container, atLeastOnce()).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
                        getAccessibilityEventSourceSemanticsNodeId(it) == topColumn.id &&
                            it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                            it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                    }
                )
            )
        }
    }

    @Test
    fun traverseEventBeforeSelectionEvent_whenTraverseTextField() {
        val textFieldNode = rule.onNodeWithTag(TextFieldTag)
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
    fun semanticsNodeBeingMergedLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")
        val textNode = rule.onNodeWithTag(TextNodeTag, useUnmergedTree = true)
            .fetchSemanticsNode("couldn't find node with tag $TextNodeTag")
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
    fun layoutNodeWithoutSemanticsLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")
        val textNode = rule.onNodeWithTag(TextNodeTag, useUnmergedTree = true)
            .fetchSemanticsNode("couldn't find node with tag $TextNodeTag")
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
        var rootNodeBoundsLeft = 0f
        var rootNodeBoundsTop = 0f
        rule.runOnIdle {
            val rootNode = androidComposeView.semanticsOwner.rootSemanticsNode
            rootNodeBoundsLeft = rootNode.globalBounds.left
            rootNodeBoundsTop = rootNode.globalBounds.top
        }
        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")
        val toggleableNodeBounds = toggleableNode.globalBounds

        val toggleableNodeId = delegate.getVirtualViewAt(
            (toggleableNodeBounds.left + toggleableNodeBounds.right) / 2 - rootNodeBoundsLeft,
            (toggleableNodeBounds.top + toggleableNodeBounds.bottom) / 2 - rootNodeBoundsTop
        )
        assertEquals(toggleableNode.id, toggleableNodeId)

        val overlappedChildOneNode = rule.onNodeWithTag(OverlappedChildOneTag)
            .fetchSemanticsNode("couldn't find node with tag $OverlappedChildOneTag")
        val overlappedChildTwoNode = rule.onNodeWithTag(OverlappedChildTwoTag)
            .fetchSemanticsNode("couldn't find node with tag $OverlappedChildTwoTag")
        val overlappedChildNodeBounds = overlappedChildTwoNode.globalBounds
        val overlappedChildNodeId = delegate.getVirtualViewAt(
            (overlappedChildNodeBounds.left + overlappedChildNodeBounds.right) / 2 -
                rootNodeBoundsLeft,
            (overlappedChildNodeBounds.top + overlappedChildNodeBounds.bottom) / 2 -
                rootNodeBoundsTop
        )
        assertEquals(overlappedChildOneNode.id, overlappedChildNodeId)
        assertNotEquals(overlappedChildTwoNode.id, overlappedChildNodeId)
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
}
