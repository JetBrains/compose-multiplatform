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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import java.util.concurrent.CountDownLatch
import kotlin.math.max
import kotlin.math.sign
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SelectionContainerTest {
    @get:Rule
    val rule = createComposeRule().also {
        it.mainClock.autoAdvance = false
    }

    private val textContent = "Text Demo Text"
    private val fontFamily = TEST_FONT_FAMILY
    private val selection = mutableStateOf<Selection?>(null)
    private val fontSize = 20.sp
    private val log = PointerInputChangeLog()

    private val tag1 = "tag1"
    private val tag2 = "tag2"

    private val hapticFeedback = mock<HapticFeedback>()

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun press_to_cancel() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        createSelectionContainer()
        val position = 50f
        rule.onSelectionContainer()
            .performTouchInput { longClick(Offset(x = position, y = position)) }
        rule.runOnIdle {
            assertThat(selection.value).isNotNull()
        }

        // Act.
        rule.onSelectionContainer()
            .performTouchInput { click(Offset(x = position, y = position)) }

        // Assert.
        rule.runOnIdle {
            assertThat(selection.value).isNull()
            verify(
                hapticFeedback,
                times(2)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun tapToCancelDoesNotBlockUp() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        createSelectionContainer()
        val position = 50f
        rule.onSelectionContainer()
            .performTouchInput { longClick(Offset(x = position, y = position)) }

        log.entries.clear()

        // Act.
        rule.onSelectionContainer()
            .performTouchInput { click(Offset(x = position, y = position)) }

        // Assert.
        rule.runOnIdle {
            // We are interested in looking at the final up event.
            assertThat(log.entries.last().pass).isEqualTo(PointerEventPass.Final)
            assertThat(log.entries.last().changes).hasSize(1)
            assertThat(log.entries.last().changes[0].changedToUp()).isTrue()
        }
    }

    @Test
    fun long_press_select_a_word() {
        with(rule.density) {
            // Setup.
            // Long Press "m" in "Demo", and "Demo" should be selected.
            createSelectionContainer()
            val characterSize = fontSize.toPx()
            val expectedLeftX = fontSize.toDp().times(textContent.indexOf('D'))
            val expectedLeftY = fontSize.toDp()
            val expectedRightX = fontSize.toDp().times(textContent.indexOf('o') + 1)
            val expectedRightY = fontSize.toDp()

            // Act.
            rule.onSelectionContainer()
                .performTouchInput {
                    longClick(
                        Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize)
                    )
                }

            rule.mainClock.advanceTimeByFrame()
            // Assert. Should select "Demo".
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
            verify(
                hapticFeedback,
                times(1)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)

            // Check the position of the anchors of the selection handles. We don't need to compare
            // to the absolute position since the semantics report selection relative to the
            // container composable, not the screen.
            rule.onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedLeftX, expectedLeftY)
            rule.onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedRightX, expectedRightY)
        }
    }

    @Test
    fun long_press_select_a_word_rtl_layout() {
        with(rule.density) {
            // Setup.
            // Long Press "m" in "Demo", and "Demo" should be selected.
            createSelectionContainer(isRtl = true)
            val characterSize = fontSize.toPx()
            val expectedLeftX = rule.rootWidth() - fontSize.toDp().times(textContent.length)
            val expectedLeftY = fontSize.toDp()
            val expectedRightX = rule.rootWidth() - fontSize.toDp().times(" Demo Text".length)
            val expectedRightY = fontSize.toDp()

            // Act.
            rule.onSelectionContainer()
                .performTouchInput {
                    longClick(
                        Offset(
                            rule.rootWidth().toSp().toPx() - ("xt Demo Text").length *
                                characterSize,
                            0.5f * characterSize
                        )
                    )
                }

            rule.mainClock.advanceTimeByFrame()

            // Assert. Should select "Demo".
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('T'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('t') + 1)
            verify(
                hapticFeedback,
                times(1)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)

            // Check the position of the anchors of the selection handles. We don't need to compare
            // to the absolute position since the semantics report selection relative to the
            // container composable, not the screen.
            rule.onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedLeftX, expectedLeftY)
            rule.onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedRightX, expectedRightY)
        }
    }

    @Test
    fun selectionContinues_toBelowText() = with(rule.density) {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                )
            }
        }

        startSelection(tag1)
        dragHandleTo(Handle.SelectionEnd, offset = characterBox(tag2, 3).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 4, selectableId = 2)
    }

    @Test
    fun selectionContinues_toAboveText() = with(rule.density) {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                )
            }
        }

        startSelection(tag2, offset = 6) // second word should be selected
        dragHandleTo(Handle.SelectionStart, offset = characterBox(tag1, 5).bottomLeft)

        assertAnchorInfo(selection.value?.start, offset = 5, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 9, selectableId = 2)
    }

    @Test
    fun selectionContinues_toNextText_skipsDisableSelection() = with(rule.density) {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                )
                DisableSelection {
                    BasicText(
                        AnnotatedString(textContent),
                        Modifier
                            .fillMaxWidth()
                            .testTag(tag2),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                    )
                }
            }
        }

        startSelection(tag1)
        dragHandleTo(Handle.SelectionEnd, offset = characterBox(tag2, 3).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = textContent.length, selectableId = 1)
    }

    @Test
    fun selectionHandle_remainsInComposition_whenTextIsOverflowed_clipped_softwrapDisabled() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString("$textContent ".repeat(100)),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    softWrap = false
                )
                DisableSelection {
                    BasicText(
                        textContent,
                        Modifier.fillMaxWidth(),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        softWrap = false
                    )
                }
                BasicText(
                    AnnotatedString("$textContent ".repeat(100)),
                    Modifier
                        .fillMaxWidth()
                        .testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    softWrap = false
                )
            }
        }

        startSelection(tag1)
        dragHandleTo(Handle.SelectionEnd, offset = characterBox(tag2, 3).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 4, selectableId = 2)
    }

    @Test
    fun allTextIsSelected_whenTextIsOverflowed_clipped_maxLines1() = with(rule.density) {
        val longText = "$textContent ".repeat(100)
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(longText),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    maxLines = 1
                )
            }
        }

        startSelection(tag1)
        dragHandleTo(
            handle = Handle.SelectionEnd,
            offset = characterBox(tag1, 4).bottomRight + Offset(x = 0f, y = fontSize.toPx())
        )

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = longText.length, selectableId = 1)
    }

    @Test
    fun allTextIsSelected_whenTextIsOverflowed_ellipsized_maxLines1() = with(rule.density) {
        val longText = "$textContent ".repeat(100)
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(longText),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        startSelection(tag1)
        dragHandleTo(
            handle = Handle.SelectionEnd,
            offset = characterBox(tag1, 4).bottomRight + Offset(x = 0f, y = fontSize.toPx())
        )

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = longText.length, selectableId = 1)
    }

    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    fun selection_doesCopy_whenCopyKeyEventSent() {
        lateinit var clipboardManager: ClipboardManager
        createSelectionContainer {
            clipboardManager = LocalClipboardManager.current
            clipboardManager.setText(AnnotatedString("Clipboard content at start of test."))
            Column {
                BasicText(
                    text = "ExpectedText",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag1),
                )
            }
        }

        startSelection(tag1)

        rule.onNodeWithTag(tag1)
            .performKeyInput {
                keyDown(Key.CtrlLeft)
                keyDown(Key.C)
                keyUp(Key.C)
                keyUp(Key.CtrlLeft)
            }

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.text).isEqualTo("ExpectedText")
        }
    }

    private fun startSelection(
        tag: String,
        offset: Int = 0
    ) {
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        val boundingBox = textLayoutResult.getBoundingBox(offset)
        rule.onNodeWithTag(tag).performTouchInput {
            longClick(boundingBox.center)
        }
    }

    private fun characterBox(
        tag: String,
        offset: Int
    ): Rect {
        val nodePosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset).translate(nodePosition)
    }

    private fun dragHandleTo(
        handle: Handle,
        offset: Offset
    ) {
        val position = rule.onNode(isSelectionHandle(handle))
            .fetchSemanticsNode()
            .config[SelectionHandleInfoKey]
            .position
        // selection handles are anchored at the bottom of a line.

        rule.onNode(isSelectionHandle(handle)).performTouchInput {
            val delta = offset - position
            down(center)
            movePastSlopBy(delta)
            up()
        }
    }

    /**
     * Moves the first pointer by [delta] past the touch slop threshold on each axis.
     * If [delta] is 0 on either axis it will stay 0.
     */
    private fun TouchInjectionScope.movePastSlopBy(delta: Offset) {
        val slop = Offset(
            x = viewConfiguration.touchSlop * delta.x.sign,
            y = viewConfiguration.touchSlop * delta.y.sign
        )
        moveBy(delta + slop)
    }

    private fun assertAnchorInfo(
        anchorInfo: Selection.AnchorInfo?,
        resolvedTextDirection: ResolvedTextDirection = ResolvedTextDirection.Ltr,
        offset: Int = 0,
        selectableId: Long = 0
    ) {
        assertThat(anchorInfo).isEqualTo(
            Selection.AnchorInfo(
                resolvedTextDirection,
                offset,
                selectableId
            )
        )
    }

    private fun createSelectionContainer(
        isRtl: Boolean = false,
        content: (@Composable () -> Unit)? = null
    ) {
        val measureLatch = CountDownLatch(1)

        val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides hapticFeedback,
                LocalLayoutDirection provides layoutDirection
            ) {
                TestParent(
                    Modifier
                        .testTag("selectionContainer")
                        .gestureSpy(log)
                ) {
                    SelectionContainer(
                        modifier = Modifier.onGloballyPositioned {
                            measureLatch.countDown()
                        },
                        selection = selection.value,
                        onSelectionChange = {
                            selection.value = it
                        }
                    ) {
                        content?.invoke() ?: BasicText(
                            AnnotatedString(textContent),
                            Modifier.fillMaxSize(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            maxLines = Int.MAX_VALUE,
                            inlineContent = mapOf(),
                            onTextLayout = {}
                        )
                    }
                }
            }
        }
    }
}

private fun SemanticsNodeInteractionsProvider.onSelectionContainer() =
    onNode(isRoot() and hasAnyChild(hasTestTag("selectionContainer")))

private fun ComposeTestRule.rootWidth(): Dp = onRoot().getUnclippedBoundsInRoot().width

private class PointerInputChangeLog : (PointerEvent, PointerEventPass) -> Unit {

    val entries = mutableListOf<PointerInputChangeLogEntry>()

    override fun invoke(p1: PointerEvent, p2: PointerEventPass) {
        entries.add(PointerInputChangeLogEntry(p1.changes.map { it }, p2))
    }
}

private data class PointerInputChangeLogEntry(
    val changes: List<PointerInputChange>,
    val pass: PointerEventPass
)

private fun Modifier.gestureSpy(
    onPointerInput: (PointerEvent, PointerEventPass) -> Unit
): Modifier = composed {
    val spy = remember { GestureSpy() }
    spy.onPointerInput = onPointerInput
    spy
}

private class GestureSpy : PointerInputModifier {

    lateinit var onPointerInput: (PointerEvent, PointerEventPass) -> Unit

    override val pointerInputFilter = object : PointerInputFilter() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize
        ) {
            onPointerInput(pointerEvent, pass)
        }

        override fun onCancel() {
            // Nothing to implement
        }
    }
}

/**
 * Returns the text layout result caught by [SemanticsActions.GetTextLayoutResult]
 * under this node. Throws an AssertionError if the node has not defined GetTextLayoutResult
 * semantics action.
 */
fun SemanticsNodeInteraction.fetchTextLayoutResult(): TextLayoutResult {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
        it(textLayoutResults)
    }
    return textLayoutResults.first()
}

@Composable
fun TestParent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val width = placeables.fold(0) { maxWidth, placeable ->
            max(maxWidth, (placeable.width))
        }

        val height = placeables.fold(0) { minWidth, placeable ->
            max(minWidth, (placeable.height))
        }

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(0, 0)
            }
        }
    }
}
