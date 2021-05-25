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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.testing

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.bottom
import androidx.compose.ui.test.bottomCenter
import androidx.compose.ui.test.bottomLeft
import androidx.compose.ui.test.bottomRight
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.centerLeft
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.centerX
import androidx.compose.ui.test.centerY
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.down
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.hasNoScrollAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.height
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.isNotFocused
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.left
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.move
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.movePointerBy
import androidx.compose.ui.test.movePointerTo
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAncestors
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.right
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.top
import androidx.compose.ui.test.topCenter
import androidx.compose.ui.test.topLeft
import androidx.compose.ui.test.topRight
import androidx.compose.ui.test.up
import androidx.compose.ui.test.width
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN as ActionDown
import android.view.KeyEvent.KEYCODE_A as KeyCodeA

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/testing-cheatsheet.html
 *
 * No action required if it's modified.
 */

@Composable
private fun TestingCheatSheetFinders() {
    // FINDERS
    composeTestRule.onNode(matcher)
    composeTestRule.onAllNodes(matcher)
    composeTestRule.onNodeWithContentDescription("label")
    composeTestRule.onAllNodesWithContentDescription("label")
    composeTestRule.onNodeWithTag("tag")
    composeTestRule.onAllNodesWithTag("tag")
    composeTestRule.onNodeWithText("text")
    composeTestRule.onAllNodesWithText("text")
    composeTestRule.onRoot()

    // OPTIONS
    composeTestRule.onNode(matcher, useUnmergedTree = true)

    // SELECTORS
    composeTestRule.onAllNodes(matcher)
        .filter(matcher)
        .filterToOne(matcher)
    composeTestRule.onNode(matcher)
        .onAncestors()
    composeTestRule.onNode(matcher)
        .onChild()
        .onChildAt(0)
        .onChildren()
        .onFirst()
    composeTestRule.onAllNodes(matcher)
        .onLast()
        .onParent()
        .onSibling()
        .onSiblings()

    // HIERARCHICAL
    composeTestRule.onNode(
        hasAnyAncestor(matcher) and
            hasAnyChild(matcher) and
            hasAnyDescendant(matcher) and
            hasAnySibling(matcher) and
            hasParent(matcher)
    )

    // MATCHERS
    composeTestRule.onNode(
        hasClickAction() and
            hasNoClickAction() and
            hasContentDescription("label") and
            hasImeAction(ImeAction.Default) and
            hasProgressBarRangeInfo(rangeInfo) and
            hasScrollAction() and
            hasNoScrollAction() and
            hasSetTextAction() and
            hasStateDescription("label") and
            hasTestTag("tag") and
            hasText("text") and
            isDialog() and
            isEnabled() and
            isFocusable() and
            isFocused() and
            isHeading() and
            isNotEnabled() and
            isNotFocusable() and
            isNotFocused() and
            isNotSelected() and
            isOff() and
            isOn() and
            isPopup() and
            isRoot() and
            isSelectable() and
            isSelected() and
            isToggleable()
    )
}

@Composable
private fun TestingCheatSheetActions() {
    composeTestRule.onRoot()
        .performClick()
        .performGesture { longClick() }
        .performScrollTo()
        .performSemanticsAction(SemanticsActions.OnLongClick)
    composeTestRule.onRoot()
        .performKeyPress(keyEvent2)
    composeTestRule.onRoot()
        .performImeAction()
    composeTestRule.onRoot()
        .performTextClearance()
    composeTestRule.onRoot()
        .performTextInput("text")
    composeTestRule.onRoot()
        .performTextReplacement("text")

    // GESTURES

    composeTestRule.onRoot().performGesture {
        click()
        longClick()
        doubleClick()
        swipe(this.center, offset)
        pinch(offset, offset, offset, offset)
        swipeWithVelocity(offset, offset, 1f)
        swipeUp()
        swipeDown()
        swipeLeft()
        swipeRight()

        // PARTIAL GESTURES
        down(offset)
        moveTo(offset)
        movePointerTo(0, offset)
        moveBy(offset)
        movePointerBy(0, offset)
        move()
        percentOffset()
        up()
        cancel()

        visibleSize

        bottom
        bottomCenter
        bottomLeft
        bottomRight
        center
        centerLeft
        centerRight
        centerX
        centerY
        height
        left
        right
        top
        topCenter
        topLeft
        topRight
        width
    }
}

@Composable
private fun TestingCheatSheetAssertions() {
    composeTestRule.onRoot().apply {
        assert(matcher)
        assertContentDescriptionContains("label")
        assertContentDescriptionEquals("label")
        assertHasClickAction()
        assertHasNoClickAction()
        assertIsDisplayed()
        assertIsEnabled()
        assertIsFocused()
        assertIsNotDisplayed()
        assertIsNotEnabled()
        assertIsNotFocused()
        assertIsNotSelected()
        assertIsOff()
        assertIsOn()
        assertIsSelectable()
        assertIsSelected()
        assertIsToggleable()
        assertRangeInfoEquals(rangeInfo)
        assertTextContains("text")
        assertTextEquals("text")
        assertValueEquals("value")
    }

    composeTestRule.onRoot().apply {
        assertDoesNotExist()
        assertExists()
    }

    // COLLECTIONS
    composeTestRule.onAllNodes(matcher)
        .assertAll(matcher)
        .assertAny(matcher)
        .assertCountEquals(1)

    // BOUNDS
    composeTestRule.onRoot()
        .assertWidthIsEqualTo(1.dp)
        .assertHeightIsEqualTo(1.dp)
        .assertWidthIsAtLeast(1.dp)
        .assertHeightIsAtLeast(1.dp)
        .assertPositionInRootIsEqualTo(1.dp, 1.dp)
        .assertTopPositionInRootIsEqualTo(1.dp)
        .assertLeftPositionInRootIsEqualTo(1.dp)

    composeTestRule.onNodeWithTag("button")
        .getAlignmentLinePosition(FirstBaseline)

    composeTestRule.onRoot()
        .getUnclippedBoundsInRoot()
}

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.O)
private fun TestingCheatSheetOther() {

    // COMPOSE TEST RULE
    nonAndroidComposeTestRule.apply {
        setContent { }
        density
        runOnIdle { }
        runOnUiThread { }
        waitForIdle()
        waitUntil { true }
        mainClock.apply {
            autoAdvance
            currentTime
            advanceTimeBy(1L)
            advanceTimeByFrame()
            advanceTimeUntil { true }
        }
        registerIdlingResource(idlingResource)
        unregisterIdlingResource(idlingResource)
    }
    GlobalScope.launch {
        nonAndroidComposeTestRule.awaitIdle()
    }

    // ANDROID COMPOSE TEST RULE
    composeTestRule.activity
    composeTestRule.activityRule

    // Capture and debug
    composeTestRule.onRoot().apply {
        printToLog("TAG")
        printToString()
        captureToImage()
    }
    // MATCHERS
    matcher.matches(composeTestRule.onRoot().fetchSemanticsNode())
}

/*
Fakes needed for snippets to build:
 */
private val matcher = isDialog()
private class FakeActivity : ComponentActivity()
private val composeTestRule = createAndroidComposeRule<FakeActivity>()
private val nonAndroidComposeTestRule = createComposeRule()
private val keyEvent2 = KeyEvent(AndroidKeyEvent(ActionDown, KeyCodeA))
private val offset = Offset(0f, 0f)
private val rangeInfo = ProgressBarRangeInfo(0f, 0f..1f)
private val idlingResource = object : IdlingResource {
    override val isIdleNow: Boolean
        get() = TODO("Stub!")
}
