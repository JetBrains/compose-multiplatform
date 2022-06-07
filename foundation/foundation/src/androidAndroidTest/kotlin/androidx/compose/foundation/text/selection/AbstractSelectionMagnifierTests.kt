/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.MagnifierPositionInRoot
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import kotlin.math.sign
import org.junit.Rule
import org.junit.Test

/**
 * Shared tests for both [SelectionContainer]+[BasicText] and [BasicTextField] magnifiers.
 * The `check*` methods here should be called from tests in both [SelectionContainerMagnifierTest]
 * and [TextFieldMagnifierTest].
 */
internal abstract class AbstractSelectionMagnifierTests {

    @get:Rule
    val rule = createComposeRule()

    protected val defaultMagnifierSize = IntSize.Zero
    private val tag = "tag"

    @Composable
    abstract fun TestContent(
        text: String,
        modifier: Modifier,
        style: TextStyle,
        onTextLayout: (TextLayoutResult) -> Unit
    )

    @Test
    fun centerIsUnspecified_whenNoSelection() {
        val manager = SelectionManager(SelectionRegistrarImpl())
        val center = calculateSelectionMagnifierCenterAndroid(manager, defaultMagnifierSize)
        assertThat(center).isEqualTo(Offset.Unspecified)
    }

    @Test
    fun centerIsUnspecified_whenNotDragging() {
        val manager = SelectionManager(SelectionRegistrarImpl())
        manager.selection = Selection(
            start = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 0, 0),
            end = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 1, 0)
        )
        val center = calculateSelectionMagnifierCenterAndroid(manager, defaultMagnifierSize)
        assertThat(center).isEqualTo(Offset.Unspecified)
    }

    @Test
    fun magnifier_hidden_whenTextIsEmpty() {
        rule.setContent {
            Content("", Modifier.testTag(tag))
        }

        // Initiate selection.
        // TODO(b/209698586) Select programmatically once that's fixed.
        rule.onNodeWithTag(tag).performTouchInput { longClick() }

        // No magnifier yet.
        assertNoMagnifierExists()
    }

    @Test
    fun magnifier_hidden_whenSelectionWithoutHandleTouch() {
        rule.setContent {
            Content("aaaa aaaa aaaa", Modifier.testTag(tag))
        }
        // Initiate selection.
        // TODO(b/209698586) Select programmatically once that's fixed.
        rule.onNodeWithTag(tag).performTouchInput { longClick() }
        // No magnifier yet.
        assertNoMagnifierExists()
    }

    @Test
    fun magnifier_appears_duringInitialLongPressDrag_expandingForwards() {
        checkMagnifierShowsDuringInitialLongPressDrag(expandForwards = true)
    }

    @Test
    fun magnifier_appears_duringInitialLongPressDrag_expandingBackwards() {
        checkMagnifierShowsDuringInitialLongPressDrag(expandForwards = false)
    }

    @Test
    fun magnifier_appears_duringInitialLongPressDrag_expandingForwards_rtl() {
        checkMagnifierShowsDuringInitialLongPressDrag(
            expandForwards = true,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_appears_duringInitialLongPressDrag_expandingBackwards_rtl() {
        checkMagnifierShowsDuringInitialLongPressDrag(
            expandForwards = false,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_appears_whileStartHandleTouched() {
        checkMagnifierAppears_whileHandleTouched(Handle.SelectionStart)
    }

    @Test
    fun magnifier_appears_whileEndHandleTouched() {
        checkMagnifierAppears_whileHandleTouched(Handle.SelectionEnd)
    }

    @Test
    fun magnifier_followsStartHandleHorizontally_whenDragged() {
        checkMagnifierFollowsHandleHorizontally(Handle.SelectionStart)
    }

    @Test
    fun magnifier_followsEndHandleHorizontally_whenDragged() {
        checkMagnifierFollowsHandleHorizontally(Handle.SelectionEnd)
    }

    @Test
    fun magnifier_staysAtLineStart_whenDraggedPastStart() {
        checkMagnifierConstrainedToLineHorizontalBounds(Handle.SelectionStart)
    }

    @Test
    fun magnifier_staysAtLineEnd_whenDraggedPastEnd() {
        checkMagnifierConstrainedToLineHorizontalBounds(Handle.SelectionEnd)
    }

    @Test
    fun magnifier_hidden_whenDraggedFarPastStartOfLine() {
        checkMagnifierHiddenWhenDraggedTooFar(Handle.SelectionStart)
    }

    @Test
    fun magnifier_hidden_whenDraggedFarPastEndOfLine() {
        checkMagnifierHiddenWhenDraggedTooFar(Handle.SelectionEnd)
    }

    @Test
    fun magnifier_followsStartHandleHorizontally_whenDragged_rtl() {
        checkMagnifierFollowsHandleHorizontally(
            Handle.SelectionStart,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_followsEndHandleHorizontally_whenDragged_rtl() {
        checkMagnifierFollowsHandleHorizontally(
            Handle.SelectionEnd,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_staysAtLineStart_whenDraggedPastStart_rtl() {
        checkMagnifierConstrainedToLineHorizontalBounds(
            Handle.SelectionStart,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_staysAtLineEnd_whenDraggedPastEnd_rtl() {
        checkMagnifierConstrainedToLineHorizontalBounds(
            Handle.SelectionEnd,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_hidden_whenDraggedFarPastStartOfLine_rtl() {
        checkMagnifierHiddenWhenDraggedTooFar(
            Handle.SelectionStart,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_hidden_whenDraggedFarPastEndOfLine_rtl() {
        checkMagnifierHiddenWhenDraggedTooFar(
            Handle.SelectionEnd,
            layoutDirection = LayoutDirection.Rtl
        )
    }

    @Test
    fun magnifier_doesNotFollowStartHandleVertically_whenDraggedWithinLine() {
        checkMagnifierDoesNotFollowHandleVerticallyWithinLine(Handle.SelectionStart)
    }

    @Test
    fun magnifier_doesNotFollowEndHandleVertically_whenDraggedWithinLine() {
        checkMagnifierDoesNotFollowHandleVerticallyWithinLine(Handle.SelectionEnd)
    }

    @Test
    fun magnifier_followsStartHandle_whenDraggedToNextLine() {
        checkMagnifierFollowsHandleVerticallyBetweenLines(Handle.SelectionStart)
    }

    @Test
    fun magnifier_followsEndHandle_whenDraggedToNextLine() {
        checkMagnifierFollowsHandleVerticallyBetweenLines(Handle.SelectionEnd)
    }

    // Abstract composable functions can't have default parameters.
    @Composable
    private fun Content(
        text: String,
        modifier: Modifier,
        style: TextStyle = TextStyle.Default,
        onTextLayout: (TextLayoutResult) -> Unit = {}
    ) = TestContent(text, modifier, style, onTextLayout)

    protected fun checkMagnifierAppears_whileHandleTouched(handle: Handle) {
        rule.setContent {
            Content("aaaa aaaa aaaa", Modifier.testTag(tag))
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }

        assertThat(getMagnifierCenterOffset()).isNotEqualTo(Offset.Zero)

        // Stop touching the handle to hide the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { up() }

        assertNoMagnifierExists()
    }

    protected fun checkMagnifierShowsDuringInitialLongPressDrag(
        expandForwards: Boolean,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        val dragDistance = Offset(10f, 0f)
        val dragDirection = if (expandForwards) 1f else -1f
        rule.setContent {
            Content(
                if (layoutDirection == LayoutDirection.Ltr) {
                    "aaaa aaaa aaaa"
                } else {
                    "באמת באמת באמת"
                },
                Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag)
            )
        }

        // Initiate selection.
        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset.Zero, delayMillis = viewConfiguration.longPressTimeoutMillis + 100)
            }

        // Magnifier should show after long-press starts.
        val magnifierInitialPosition = getMagnifierCenterOffset()
        assertThat(magnifierInitialPosition).isNotEqualTo(Offset.Zero)

        // Drag horizontally - the magnifier should follow.
        rule.onNodeWithTag(tag)
            .performTouchInput {
                // Don't need to worry about touch slop for this test since the drag starts as soon
                // as the long click is detected.
                moveBy(dragDistance * dragDirection)
            }

        assertThat(getMagnifierCenterOffset())
            .isEqualTo(magnifierInitialPosition + (dragDistance * dragDirection))
    }

    protected fun checkMagnifierFollowsHandleHorizontally(
        handle: Handle,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        val dragDistance = Offset(1f, 0f)
        rule.setContent {
            Content(
                if (layoutDirection == LayoutDirection.Ltr) {
                    "aaaa aaaa aaaa"
                } else {
                    "באמת באמת באמת"
                },
                Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag)
            )
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }
        val magnifierInitialPosition = getMagnifierCenterOffset()

        // Drag the handle horizontally - the magnifier should follow.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { movePastSlopBy(dragDistance) }

        assertThat(getMagnifierCenterOffset())
            .isEqualTo(magnifierInitialPosition + dragDistance)
    }

    protected fun checkMagnifierConstrainedToLineHorizontalBounds(
        handle: Handle,
        checkStart: Boolean = handle == Handle.SelectionStart,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        val dragDistance = Offset(1f, 0f)
        val dragDirection = if (checkStart xor (layoutDirection == LayoutDirection.Rtl)) -1f else 1f
        val fillerWord = if (layoutDirection == LayoutDirection.Ltr) "aaaa" else "באמת"
        // When testing the cursor, we use an empty line so it doesn't have room to move in either
        // direction. For other handles, the line needs to have some text to select.
        val middleLine = if (handle == Handle.Cursor) "" else fillerWord
        rule.setContent {
            Content(
                // Center line of text is shorter than others.
                "$fillerWord$fillerWord\n$middleLine\n$fillerWord$fillerWord",
                Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag),
                style = TextStyle(textAlign = TextAlign.Center)
            )
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }
        val magnifierInitialPosition = getMagnifierCenterOffset()

        // Drag just a little past the end of the line.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { movePastSlopBy(dragDistance * dragDirection) }

        // The magnifier shouldn't have moved.
        assertThat(getMagnifierCenterOffset()).isEqualTo(magnifierInitialPosition)
    }

    protected fun checkMagnifierHiddenWhenDraggedTooFar(
        handle: Handle,
        checkStart: Boolean = handle == Handle.SelectionStart,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        var screenWidth = 0
        val dragDirection = if (checkStart) -1f else 1f
        rule.setContent {
            Content(
                if (layoutDirection == LayoutDirection.Ltr) {
                    "aaaa aaaa\naaaa\naaaa aaaa"
                } else {
                    "באמתבאמת\nבאמת\nבאמתבאמת"
                },
                Modifier
                    .onSizeChanged { screenWidth = it.width }
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag),
                style = TextStyle(textAlign = TextAlign.Center)
            )
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }

        // Drag all the way past the end of the line.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput {
                moveBy(Offset(screenWidth.toFloat(), 0f) * dragDirection)
            }

        // The magnifier should be gone.
        assertNoMagnifierExists()
    }

    protected fun checkMagnifierFollowsHandleVerticallyBetweenLines(handle: Handle) {
        val dragDistance = Offset(0f, 1f)
        var lineHeight = 0f
        rule.setContent {
            Content(
                "aaaa aaaa aaaa\naaaa aaaa aaaa\naaaa aaaa aaaa",
                Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag),
                onTextLayout = { lineHeight = it.getLineBottom(2) - it.getLineBottom(1) }
            )
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }
        val magnifierInitialPosition = getMagnifierCenterOffset()

        // Drag the handle down - the magnifier should follow.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { movePastSlopBy(dragDistance) }

        val (x, y) = getMagnifierCenterOffset()
        assertThat(x).isEqualTo(magnifierInitialPosition.x)
        assertThat(y)
            .isWithin(1f)
            .of(magnifierInitialPosition.y + lineHeight)
    }

    protected fun checkMagnifierDoesNotFollowHandleVerticallyWithinLine(handle: Handle) {
        val dragDistance = Offset(0f, 1f)
        rule.setContent {
            Content(
                "aaaa aaaa aaaa\naaaa aaaa aaaa\naaaa aaaa aaaa",
                Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag)
            )
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }
        val magnifierInitialPosition = getMagnifierCenterOffset()

        // Drag the handle up - the magnifier should not follow.
        // Note that dragging it down *should* cause it to move to the line below, so only drag up.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput {
                movePastSlopBy(-dragDistance)
            }

        assertThat(getMagnifierCenterOffset())
            .isEqualTo(magnifierInitialPosition)
    }

    private fun isSelectionHandle(handle: Handle) = SemanticsMatcher("is $handle handle") { node ->
        node.config.getOrNull(SelectionHandleInfoKey)?.handle == handle
    }

    private fun showHandle(handle: Handle) {
        if (handle == Handle.Cursor) {
            rule.onNodeWithTag(tag).performClick()
        } else {
            // TODO(b/209698586) Select programmatically once that's fixed.
            rule.onNodeWithTag(tag).performTouchInput { longClick() }
        }
    }

    /**
     * Moves the first pointer by [delta] past the touch slop threshold on each axis.
     * If [delta] is 0 on either axis it will stay 0.
     */
    // TODO(b/210545925) This is here because we can't disable the touch slop in a popup. When
    //  that's fixed we can just disable slop and delete this function.
    private fun TouchInjectionScope.movePastSlopBy(delta: Offset) {
        val slop = Offset(
            x = viewConfiguration.touchSlop * delta.x.sign,
            y = viewConfiguration.touchSlop * delta.y.sign
        )
        moveBy(delta + slop)
    }

    private fun getMagnifierCenterOffset(): Offset =
        rule.onNode(keyIsDefined(MagnifierPositionInRoot))
            .fetchSemanticsNode()
            .config[MagnifierPositionInRoot]
            .let(rule::runOnIdle)

    /**
     * Asserts that there is no magnifier being displayed. This may be because no
     * `Modifier.magnifier` modifiers are currently set on any nodes, or because all the magnifiers
     * that exist have an unspecified position.
     */
    private fun assertNoMagnifierExists() {
        // The magnifier semantics will be present whenever the modifier is, even if the modifier
        // isn't actually showing a magnifier because the position is unspecified. So instead of
        // just checking that no semantics property exists, we need to check that the value of each
        // property won't show a magnifier.
        val magnifierNodes = rule.onAllNodes(keyIsDefined(MagnifierPositionInRoot))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .map { it.config[MagnifierPositionInRoot].invoke() }
        assertThat(magnifierNodes.all { it.isUnspecified }).isTrue()
    }
}