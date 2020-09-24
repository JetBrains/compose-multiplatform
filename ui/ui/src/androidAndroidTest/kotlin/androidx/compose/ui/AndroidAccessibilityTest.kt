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
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BaseTextField
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.os.BuildCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assert
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.assertTextEquals
import androidx.ui.test.createAndroidComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import androidx.ui.test.performSemanticsAction
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.atLeast
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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    ExperimentalLayoutNodeApi::class
)
class AndroidAccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: ViewGroup
    private lateinit var delegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var provider: AccessibilityNodeProvider
    private lateinit var textLayoutResult: TextLayoutResult

    private val argument = ArgumentCaptor.forClass(AccessibilityEvent::class.java)
    private var isTextFieldVisible by mutableStateOf(true)
    private var textFieldSelectionOneLatch = CountDownLatch(1)

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
            BuildCompat.isAtLeastR() -> {
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
    fun testPerformAction() {
        val toggleableNode = rule.onNodeWithTag(ToggleableTag)
            .fetchSemanticsNode("couldn't find node with tag $ToggleableTag")
        rule.runOnUiThread {
            provider.performAction(toggleableNode.id, ACTION_CLICK, null)
        }
        rule.onNodeWithTag(ToggleableTag)
            .assertIsOff()

        val textFieldNode = rule.onNodeWithTag(TextFieldTag)
            .fetchSemanticsNode("couldn't find node with tag $TextFieldTag")
        rule.runOnUiThread {
            provider.performAction(textFieldNode.id, ACTION_CLICK, null)
        }
        rule.onNodeWithTag(TextFieldTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 1)
        argument.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 1)
        rule.runOnUiThread {
            textFieldSelectionOneLatch = CountDownLatch(1)
            provider.performAction(textFieldNode.id, ACTION_SET_SELECTION, argument)
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
        rule.onNodeWithTag(TextFieldTag)
            .assertExists()
        // TextField is removed compared to setup.
        isTextFieldVisible = false
        rule.onNodeWithTag(TextFieldTag)
            .assertDoesNotExist()

        rule.runOnIdle {
            // One from initialization and one from text field removal.
            verify(container, atLeast(2)).requestSendAccessibilityEvent(
                eq(androidComposeView),
                argThat(
                    ArgumentMatcher {
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
