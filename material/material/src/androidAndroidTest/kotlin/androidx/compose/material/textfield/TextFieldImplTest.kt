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

package androidx.compose.material.textfield

import android.os.Build
import androidx.compose.runtime.remember
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BaseTextField
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.TextFieldScroller
import androidx.compose.material.TextFieldScrollerPosition
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.swipeDown
import androidx.ui.test.swipeUp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextFieldImplTest {

    private val TextfieldTag = "textField"

    private val LONG_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur."

    @get:Rule
    val testRule = createComposeRule()

    @Test
    fun testTextField_scrollable_withLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        testRule.setContent {
            Stack {
                TextFieldScroller(
                    remember { scrollerPosition },
                    Modifier.preferredSize(width = 300.dp, height = 50.dp)
                ) {
                    BaseTextField(
                        value = TextFieldValue(LONG_TEXT),
                        onValueChange = {}
                    )
                }
            }
        }

        runOnIdle {
            assertThat(scrollerPosition.maximum).isLessThan(Float.POSITIVE_INFINITY)
            assertThat(scrollerPosition.maximum).isGreaterThan(0f)
        }
    }

    @Test
    fun testTextField_notScrollable_withShortInput() {
        val text = "text"
        val scrollerPosition = TextFieldScrollerPosition()
        testRule.setContent {
            Stack {
                TextFieldScroller(
                    remember { scrollerPosition },
                    Modifier.preferredSize(width = 300.dp, height = 50.dp)
                ) {
                    BaseTextField(
                        value = TextFieldValue(text),
                        onValueChange = {}
                    )
                }
            }
        }

        runOnIdle {
            assertThat(scrollerPosition.maximum).isEqualTo(0f)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_scrolledAndClipped() {
        val scrollerPosition = TextFieldScrollerPosition()

        val parentSize = 200
        val textFieldSize = 50

        with(testRule.density) {
            testRule.setContent {
                Stack(
                    Modifier
                        .preferredSize(parentSize.toDp())
                        .background(color = Color.White)
                        .testTag(TextfieldTag)
                ) {
                    TextFieldScroller(
                        remember { scrollerPosition },
                        Modifier.preferredSize(textFieldSize.toDp())
                    ) {
                        BaseTextField(
                            value = TextFieldValue(LONG_TEXT),
                            onValueChange = {}
                        )
                    }
                }
            }
        }

        runOnIdle {}

        onNodeWithTag(TextfieldTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(parentSize, parentSize)) { position ->
                if (position.x > textFieldSize && position.y > textFieldSize) Color.White else null
            }
    }

    @Test
    fun testTextField_swipe_whenLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()

        testRule.setContent {
            Stack {
                TextFieldScroller(
                    remember { scrollerPosition },
                    Modifier.preferredSize(width = 300.dp, height = 50.dp).testTag(TextfieldTag)
                ) {
                    BaseTextField(
                        value = TextFieldValue(LONG_TEXT),
                        onValueChange = {}
                    )
                }
            }
        }

        runOnIdle {
            assertThat(scrollerPosition.current).isEqualTo(0f)
        }

        onNodeWithTag(TextfieldTag)
            .performGesture { swipeDown() }

        val firstSwipePosition = runOnIdle {
            scrollerPosition.current
        }
        assertThat(firstSwipePosition).isGreaterThan(0f)

        onNodeWithTag(TextfieldTag)
            .performGesture { swipeUp() }
        runOnIdle {
            assertThat(scrollerPosition.current).isLessThan(firstSwipePosition)
        }
    }

    @Test
    fun textFieldScroller_restoresScrollerPosition() {
        val restorationTester = StateRestorationTester(testRule)
        var scrollerPosition = TextFieldScrollerPosition()

        restorationTester.setContent {
            scrollerPosition = rememberSavedInstanceState(
                saver = TextFieldScrollerPosition.Saver
            ) {
                TextFieldScrollerPosition()
            }
            TextFieldScroller(
                scrollerPosition,
                Modifier.preferredSize(width = 300.dp, height = 50.dp).testTag(TextfieldTag)
            ) {
                BaseTextField(
                    value = TextFieldValue(LONG_TEXT),
                    onValueChange = {}
                )
            }
        }

        onNodeWithTag(TextfieldTag)
            .performGesture { swipeDown() }

        val swipePosition = runOnIdle {
            scrollerPosition.current
        }
        assertThat(swipePosition).isGreaterThan(0f)

        runOnIdle {
            scrollerPosition = TextFieldScrollerPosition()
            assertThat(scrollerPosition.current).isEqualTo(0f)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        runOnIdle {
            assertThat(scrollerPosition.current).isEqualTo(swipePosition)
        }
    }
}