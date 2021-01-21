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

import android.os.Build
import android.text.SpannableString
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.node.InnerPlaceable
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AmbientClipboardManager
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsModifierCore
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsWrapper
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidComposeViewAccessibilityDelegateCompatTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var accessibilityDelegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var container: ViewGroup
    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var info: AccessibilityNodeInfoCompat

    @Before
    fun setup() {
        // Use uiAutomation to enable accessibility manager.
        InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.activityRule.scenario.onActivity {
            androidComposeView = AndroidComposeView(it)
            container = spy(FrameLayout(it)) {
                on {
                    onRequestSendAccessibilityEvent(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                } doReturn false
            }
            container.addView(androidComposeView)
            accessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(
                androidComposeView
            )
            accessibilityDelegate.accessibilityForceEnabledForTesting = true
        }
        rule.setContent {
            AmbientClipboardManager.current.setText(AnnotatedString("test"))
        }
        info = AccessibilityNodeInfoCompat.obtain()
    }

    @After
    fun cleanup() {
        info.recycle()
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_general() {
        val clickActionLabel = "click"
        val dismissActionLabel = "dismiss"
        val stateDescription = "checked"
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            this.stateDescription = stateDescription
            heading()
            onClick(clickActionLabel) { true }
            dismiss(dismissActionLabel) { true }
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.view.View", info.className)
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                    clickActionLabel
                )
            )
        )
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_DISMISS,
                    dismissActionLabel
                )
            )
        )
        val stateDescriptionResult = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                info.unwrap().stateDescription
            }
            Build.VERSION.SDK_INT >= 19 -> {
                info.extras.getCharSequence(
                    "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY"
                )
            }
            else -> {
                null
            }
        }
        assertEquals(stateDescription, stateDescriptionResult)
        assertTrue(info.isHeading)
        assertTrue(info.isClickable)
        assertTrue(info.isVisibleToUser)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_disabled() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            disabled()
            text = AnnotatedString("text")
            horizontalScrollAxisRange = ScrollAxisRange(0f, 5f)
            onClick { true }
            onLongClick { true }
            copyText { true }
            pasteText { true }
            cutText { true }
            setText { true }
            setSelection { _, _, _ -> true }
            dismiss { true }
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertTrue(info.isClickable)
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK
            )
        )
        assertTrue(info.isLongClickable)
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK
            )
        )
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COPY
            )
        )
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_PASTE
            )
        )
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CUT
            )
        )
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SET_TEXT
            )
        )
        // This is the default ACTION_SET_SELECTION.
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SET_SELECTION
            )
        )
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_DISMISS
            )
        )
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD
            )
        )
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT
            )
        )
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_buttonRole() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            role = Role.Button
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.Button", info.className)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_switchRole() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            role = Role.Switch
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.Switch", info.className)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_checkBoxRole() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            role = Role.Checkbox
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.CheckBox", info.className)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_radioButtonRole() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            role = Role.RadioButton
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.RadioButton", info.className)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_tabRole() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            role = Role.Tab
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("Tab", info.roleDescription)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_imageRole() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            role = Role.Image
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.ImageView", info.className)
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_SeekBar() {
        val setProgressActionLabel = "setProgress"
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            progressBarRangeInfo = ProgressBarRangeInfo(0.5f, 0f..1f, 6)
            setProgress(setProgressActionLabel) { true }
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.SeekBar", info.className)
        assertEquals(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
            info.rangeInfo.type
        )
        assertEquals(0.5f, info.rangeInfo.current)
        assertEquals(0f, info.rangeInfo.min)
        assertEquals(1f, info.rangeInfo.max)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertTrue(
                containsAction(
                    info,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        android.R.id.accessibilityActionSetProgress,
                        setProgressActionLabel
                    )
                )
            )
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_EditText() {
        val setSelectionActionLabel = "setSelection"
        val setTextActionLabel = "setText"
        val text = "hello"
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            this.text = AnnotatedString(text)
            this.textSelectionRange = TextRange(1)
            this.focused = true
            getTextLayoutResult { true }
            setText(setTextActionLabel) { true }
            setSelection(setSelectionActionLabel) { _, _, _ -> true }
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)
        assertEquals("android.widget.EditText", info.className)
        assertEquals(SpannableString(text), info.text)
        assertTrue(info.isFocusable)
        assertTrue(info.isFocused)
        assertTrue(info.isEditable)
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                    setSelectionActionLabel
                )
            )
        )
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_SET_TEXT,
                    setTextActionLabel
                )
            )
        )
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat
                    .ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            )
        )
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat
                    .ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
            )
        )
        assertEquals(
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE,
            info.movementGranularities
        )
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(
                listOf(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY),
                info.unwrap().availableExtraData
            )
        }
    }

    @Test
    fun test_PasteAction_ifFocused() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            focused = true
            pasteText {
                true
            }
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)

        assertTrue(info.isFocused)
        assertTrue(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_PASTE,
                    null
                )
            )
        )
    }

    @Test
    fun test_noPasteAction_ifUnfocused() {
        val semanticsNode = createSemanticsNodeWithProperties(1, true) {
            pasteText {
                true
            }
        }
        accessibilityDelegate.populateAccessibilityNodeInfoProperties(1, info, semanticsNode)

        assertFalse(info.isFocused)
        assertFalse(
            containsAction(
                info,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_PASTE,
                    null
                )
            )
        )
    }

    @Test
    fun notSendScrollEvent_whenOnlyScrollAxisRangeMaxValueChanges() {
        val oldSemanticsNode = createSemanticsNodeWithProperties(1, true) {
            this.verticalScrollAxisRange = ScrollAxisRange(0f, 0f, false)
        }
        accessibilityDelegate.previousSemanticsNodes[1] =
            AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy(
                oldSemanticsNode,
                mapOf()
            )
        val newSemanticsNode = createSemanticsNodeWithProperties(1, true) {
            this.verticalScrollAxisRange = ScrollAxisRange(0f, 5f, false)
        }
        val newNodes = mutableMapOf<Int, SemanticsNode>()
        newNodes[1] = newSemanticsNode
        accessibilityDelegate.sendSemanticsPropertyChangeEvents(newNodes)

        verify(container, never()).requestSendAccessibilityEvent(
            eq(androidComposeView),
            argThat(
                ArgumentMatcher {
                    it.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                }
            )
        )
    }

    @Test
    fun sendScrollEvent_whenScrollAxisRangeValueChanges() {
        val oldSemanticsNode = createSemanticsNodeWithProperties(2, false) {
            this.verticalScrollAxisRange = ScrollAxisRange(0f, 5f, false)
        }
        accessibilityDelegate.previousSemanticsNodes[2] =
            AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy(
                oldSemanticsNode,
                mapOf()
            )
        val newSemanticsNode = createSemanticsNodeWithProperties(2, false) {
            this.verticalScrollAxisRange = ScrollAxisRange(2f, 5f, false)
        }
        val newNodes = mutableMapOf<Int, SemanticsNode>()
        newNodes[2] = newSemanticsNode
        accessibilityDelegate.sendSemanticsPropertyChangeEvents(newNodes)

        verify(container, times(1)).requestSendAccessibilityEvent(
            eq(androidComposeView),
            argThat(
                ArgumentMatcher {
                    it.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && it.scrollY == 2 &&
                        it.maxScrollY == 5 &&
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            it.scrollDeltaY == 2
                        } else {
                            true
                        }
                }
            )
        )
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_whenPropertyAdded() {
        val oldSemanticsNode = createSemanticsNodeWithProperties(1, false) {}
        accessibilityDelegate.previousSemanticsNodes[1] =
            AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy(
                oldSemanticsNode,
                mapOf()
            )
        val newSemanticsNode = createSemanticsNodeWithProperties(1, false) {
            disabled()
        }
        val newNodes = mutableMapOf<Int, SemanticsNode>()
        newNodes[1] = newSemanticsNode
        accessibilityDelegate.sendSemanticsPropertyChangeEvents(newNodes)

        verify(container, times(1)).requestSendAccessibilityEvent(
            eq(androidComposeView),
            argThat(
                ArgumentMatcher {
                    it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                        it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                }
            )
        )
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_whenPropertyRemoved() {
        val oldSemanticsNode = createSemanticsNodeWithProperties(1, false) {
            disabled()
        }
        accessibilityDelegate.previousSemanticsNodes[1] =
            AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy(
                oldSemanticsNode,
                mapOf()
            )
        val newSemanticsNode = createSemanticsNodeWithProperties(1, false) {}
        val newNodes = mutableMapOf<Int, SemanticsNode>()
        newNodes[1] = newSemanticsNode
        accessibilityDelegate.sendSemanticsPropertyChangeEvents(newNodes)

        verify(container, times(1)).requestSendAccessibilityEvent(
            eq(androidComposeView),
            argThat(
                ArgumentMatcher {
                    it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                        it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                }
            )
        )
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_onlyOnce_whenMultiplePropertiesChange() {
        val oldSemanticsNode = createSemanticsNodeWithProperties(1, false) {
            disabled()
        }
        accessibilityDelegate.previousSemanticsNodes[1] =
            AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy(
                oldSemanticsNode,
                mapOf()
            )
        val newSemanticsNode = createSemanticsNodeWithProperties(1, false) {
            onClick { true }
        }
        val newNodes = mutableMapOf<Int, SemanticsNode>()
        newNodes[1] = newSemanticsNode
        accessibilityDelegate.sendSemanticsPropertyChangeEvents(newNodes)

        verify(container, times(1)).requestSendAccessibilityEvent(
            eq(androidComposeView),
            argThat(
                ArgumentMatcher {
                    it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                        it.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                }
            )
        )
    }

    private fun createSemanticsNodeWithProperties(
        id: Int,
        mergeDescendants: Boolean,
        properties: (SemanticsPropertyReceiver.() -> Unit)
    ): SemanticsNode {
        val semanticsModifier = SemanticsModifierCore(id, mergeDescendants, false, properties)
        return SemanticsNode(
            SemanticsWrapper(InnerPlaceable(LayoutNode()), semanticsModifier),
            true
        )
    }

    private fun containsAction(
        info: AccessibilityNodeInfoCompat,
        action: AccessibilityNodeInfoCompat.AccessibilityActionCompat
    ): Boolean {
        for (a in info.actionList) {
            if (a.id == action.id && a.label == action.label) {
                return true
            }
        }
        return false
    }
}
