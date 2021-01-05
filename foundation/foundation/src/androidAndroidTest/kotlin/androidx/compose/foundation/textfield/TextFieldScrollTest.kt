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

package androidx.compose.foundation.textfield

import android.os.Build
import androidx.compose.animation.core.ExponentialDecay
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.TextFieldScrollerPosition
import androidx.compose.foundation.text.maxLinesHeight
import androidx.compose.foundation.text.textFieldScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class, InternalTextApi::class)
class TextFieldScrollTest {

    private val TextfieldTag = "textField"

    private val longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
        " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
        "fugiat nulla pariatur."

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testTextField_horizontallyScrollable_withLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                softWrap = false,
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 50.dp)
                    .maxLinesHeight(1, TextStyle.Default)
                    .textFieldScroll(
                        orientation = Orientation.Horizontal,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.maximum).isLessThan(Float.POSITIVE_INFINITY)
            assertThat(scrollerPosition.maximum).isGreaterThan(0f)
        }
    }

    @Test
    fun testTextField_verticallyScrollable_withLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 50.dp)
                    .maxLinesHeight(Int.MAX_VALUE, TextStyle.Default)
                    .textFieldScroll(
                        orientation = Orientation.Vertical,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef,
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.maximum).isLessThan(Float.POSITIVE_INFINITY)
            assertThat(scrollerPosition.maximum).isGreaterThan(0f)
        }
    }

    @Test
    fun testTextField_verticallyScrollable_withLongInput_whenMaxLinesProvided() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                modifier = Modifier
                    .preferredWidth(100.dp)
                    .maxLinesHeight(3, TextStyle.Default)
                    .textFieldScroll(
                        orientation = Orientation.Vertical,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef,
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.maximum).isLessThan(Float.POSITIVE_INFINITY)
            assertThat(scrollerPosition.maximum).isGreaterThan(0f)
        }
    }

    @Test
    fun testTextField_horizontallyNotScrollable_withShortInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue("text")

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                softWrap = false,
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 50.dp)
                    .maxLinesHeight(1, TextStyle.Default)
                    .textFieldScroll(
                        orientation = Orientation.Horizontal,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef,
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.maximum).isEqualTo(0f)
        }
    }

    @Test
    fun testTextField_verticallyNotScrollable_withShortInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue("text")

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 100.dp)
                    .textFieldScroll(
                        orientation = Orientation.Vertical,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef,
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.maximum).isEqualTo(0f)
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_horizontal_scrolledAndClipped() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        val parentSize = 200
        val textFieldSize = 50

        with(rule.density) {
            rule.setContent {
                val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
                Box(
                    Modifier
                        .preferredSize(parentSize.toDp())
                        .background(color = Color.White)
                        .testTag(TextfieldTag)
                ) {
                    CoreTextField(
                        value = value,
                        onValueChange = {},
                        onTextLayout = { textLayoutResultRef.value = it },
                        softWrap = false,
                        modifier = Modifier
                            .preferredSize(textFieldSize.toDp())
                            .textFieldScroll(
                                orientation = Orientation.Horizontal,
                                remember { scrollerPosition },
                                value,
                                VisualTransformation.None,
                                remember { InteractionState() },
                                textLayoutResultRef
                            )
                    )
                }
            }
        }

        rule.runOnIdle {}

        rule.onNodeWithTag(TextfieldTag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(parentSize, parentSize)) { position ->
                if (position.x > textFieldSize && position.y > textFieldSize) Color.White else null
            }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_vertical_scrolledAndClipped() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        val parentSize = 200
        val textFieldSize = 50

        with(rule.density) {
            rule.setContent {
                val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
                Box(
                    Modifier
                        .preferredSize(parentSize.toDp())
                        .background(color = Color.White)
                        .testTag(TextfieldTag)
                ) {
                    CoreTextField(
                        value = value,
                        onValueChange = {},
                        onTextLayout = { textLayoutResultRef.value = it },
                        modifier = Modifier
                            .preferredSize(textFieldSize.toDp())
                            .textFieldScroll(
                                orientation = Orientation.Vertical,
                                remember { scrollerPosition },
                                value,
                                VisualTransformation.None,
                                remember { InteractionState() },
                                textLayoutResultRef
                            )
                    )
                }
            }
        }

        rule.runOnIdle {}

        rule.onNodeWithTag(TextfieldTag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(parentSize, parentSize)) { position ->
                if (position.x > textFieldSize && position.y > textFieldSize) Color.White else null
            }
    }

    @Test
    fun testTextField_horizontalScroll_swipe_whenLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }

            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                softWrap = false,
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 50.dp)
                    .testTag(TextfieldTag)
                    .maxLinesHeight(1, TextStyle.Default)
                    .textFieldScroll(
                        Orientation.Horizontal,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.offset).isEqualTo(0f)
        }

        rule.onNodeWithTag(TextfieldTag)
            .performGesture { swipeLeft() }

        val firstSwipePosition = rule.runOnIdle {
            scrollerPosition.offset
        }
        assertThat(firstSwipePosition).isGreaterThan(0f)

        rule.onNodeWithTag(TextfieldTag)
            .performGesture { swipeRight() }
        rule.runOnIdle {
            assertThat(scrollerPosition.offset).isLessThan(firstSwipePosition)
        }
    }

    @Test
    fun testTextField_verticalScroll_swipe_whenLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }

            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 50.dp)
                    .testTag(TextfieldTag)
                    .textFieldScroll(
                        Orientation.Vertical,
                        remember { scrollerPosition },
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef
                    )
            )
        }

        rule.runOnIdle {
            assertThat(scrollerPosition.offset).isEqualTo(0f)
        }

        rule.onNodeWithTag(TextfieldTag)
            .performGesture { swipeUp() }

        val firstSwipePosition = rule.runOnIdle {
            scrollerPosition.offset
        }
        assertThat(firstSwipePosition).isGreaterThan(0f)

        rule.onNodeWithTag(TextfieldTag)
            .performGesture { swipeDown() }
        rule.runOnIdle {
            assertThat(scrollerPosition.offset).isLessThan(firstSwipePosition)
        }
    }

    @Test
    fun textFieldScroller_restoresScrollerPosition() {
        val restorationTester = StateRestorationTester(rule)
        var scrollerPosition = TextFieldScrollerPosition()
        val value = TextFieldValue(longText)

        restorationTester.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }

            scrollerPosition = rememberSavedInstanceState(
                saver = TextFieldScrollerPosition.Saver
            ) {
                TextFieldScrollerPosition()
            }
            CoreTextField(
                value = value,
                onValueChange = {},
                onTextLayout = { textLayoutResultRef.value = it },
                softWrap = false,
                modifier = Modifier
                    .preferredSize(width = 300.dp, height = 50.dp)
                    .testTag(TextfieldTag)
                    .maxLinesHeight(1, TextStyle.Default)
                    .textFieldScroll(
                        Orientation.Horizontal,
                        scrollerPosition,
                        value,
                        VisualTransformation.None,
                        remember { InteractionState() },
                        textLayoutResultRef
                    )
            )
        }

        rule.onNodeWithTag(TextfieldTag)
            .performGesture { swipeLeft() }

        val swipePosition = rule.runOnIdle {
            scrollerPosition.offset
        }
        assertThat(swipePosition).isGreaterThan(0f)

        rule.runOnIdle {
            scrollerPosition = TextFieldScrollerPosition()
            assertThat(scrollerPosition.offset).isEqualTo(0f)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(scrollerPosition.offset).isEqualTo(swipePosition)
        }
    }

    @Test
    fun testInspectorValue() {
        val position = TextFieldScrollerPosition(initial = 10f)
        val orientation = Orientation.Vertical
        val value = TextFieldValue()
        rule.setContent {
            val modifier = Modifier.textFieldScroll(
                orientation,
                position,
                value,
                VisualTransformation.None,
                remember { InteractionState() },
                Ref(),
                true
            ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("textFieldScroll")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "orientation",
                "scrollerPosition",
                "textFieldValue",
                "visualTransformation",
                "interactionState",
                "textLayoutResult",
                "enabled"
            )
        }
    }

    @Test
    fun testNestedScrolling() {
        val size = 300.dp
        val text = """
            First Line
            Second Line
            Third Line
            Fourth Line
        """.trimIndent()
        val value = TextFieldValue(text)

        val textFieldScrollPosition = TextFieldScrollerPosition()
        val scrollerPosition = ScrollState(
            0f,
            FlingConfig(ExponentialDecay()),
            ManualAnimationClock(0)
        )

        rule.setContent {
            val textLayoutResultRef: Ref<TextLayoutResult?> = remember { Ref() }
            ScrollableColumn(
                modifier = Modifier.preferredSize(size),
                scrollState = remember { scrollerPosition }
            ) {
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    onTextLayout = { textLayoutResultRef.value = it },
                    modifier = Modifier
                        .preferredSize(size, 50.dp)
                        .testTag(TextfieldTag)
                        .textFieldScroll(
                            Orientation.Vertical,
                            remember { textFieldScrollPosition },
                            value,
                            VisualTransformation.None,
                            remember { InteractionState() },
                            textLayoutResultRef
                        ),
                    textStyle = TextStyle(fontSize = 20.sp)
                )
                Box(Modifier.preferredSize(size))
                Box(Modifier.preferredSize(size))
            }
        }

        rule.runOnIdle {
            assertThat(textFieldScrollPosition.offset).isEqualTo(0f)
            assertThat(textFieldScrollPosition.maximum).isGreaterThan(0f)
            assertThat(scrollerPosition.value).isEqualTo(0f)
        }

        with(rule.density) {
            val x = 10.dp.toPx()
            val start = Offset(x, 40.dp.toPx())
            val end = Offset(x, 0f)
            rule.onNodeWithTag(TextfieldTag)
                .performGesture {
                    // scroll first two lines
                    swipe(start, end)
                    // scroll last two lines
                    swipe(start, end)
                    // scroll Scrollable column
                    swipe(start, end)
                }
        }

        rule.runOnIdle {
            assertThat(textFieldScrollPosition.offset).isGreaterThan(0f)
            assertThat(textFieldScrollPosition.offset).isEqualTo(textFieldScrollPosition.maximum)
            assertThat(scrollerPosition.value).isGreaterThan(0f)
        }
    }
}