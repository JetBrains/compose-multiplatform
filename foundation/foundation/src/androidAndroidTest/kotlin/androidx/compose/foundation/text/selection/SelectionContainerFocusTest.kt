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

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.CoreText
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@LargeTest
@RunWith(AndroidJUnit4::class)
class SelectionContainerFocusTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private val textContent = "Text Demo Text"
    private val fontFamily = TEST_FONT_FAMILY

    private val selection1 = mutableStateOf<Selection?>(null)
    private val selection2 = mutableStateOf<Selection?>(null)
    private val fontSize = 20.sp
    private val boxSize = 40.dp

    private val hapticFeedback = mock<HapticFeedback>()

    @FlakyTest(bugId = 179770443)
    @Test
    fun click_anywhere_to_cancel() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        createSelectionContainer()
        // Touch position. In this test, each character's width and height equal to fontSize.
        // Position is computed so that (position, position) is the center of the first character.
        val positionInText = with(Density(view.context)) {
            fontSize.toPx() / 2
        }
        rule.onNode(hasTestTag("selectionContainer1"))
            .performGesture { longClick(Offset(x = positionInText, y = positionInText)) }
        rule.runOnIdle {
            assertThat(selection1.value).isNotNull()
        }

        // Touch position. In this test, each character's width and height equal to fontSize.
        // Position is computed so that (position, position) is the center of the first character.
        val positionInBox = with(Density(view.context)) {
            boxSize.toPx() / 2
        }
        // Act.
        rule.onNode(hasTestTag("box"))
            .performGesture { click(Offset(x = positionInBox, y = positionInBox)) }

        // Assert.
        rule.runOnIdle {
            assertThat(selection1.value).isNull()
            assertThat(selection2.value).isNull()
            verify(
                hapticFeedback,
                times(2)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    fun select_anotherContainer_cancelOld() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        createSelectionContainer()
        // Touch position. In this test, each character's width and height equal to fontSize.
        // Position is computed so that (position, position) is the center of the first character.
        val positionInText = with(Density(view.context)) {
            fontSize.toPx() / 2
        }
        rule.onNode(hasTestTag("selectionContainer1"))
            .performGesture { longClick(Offset(x = positionInText, y = positionInText)) }
        rule.runOnIdle {
            assertThat(selection1.value).isNotNull()
        }

        // Act.
        rule.onNode(hasTestTag("selectionContainer2"))
            .performGesture { longClick(Offset(x = positionInText, y = positionInText)) }

        // Assert.
        rule.runOnIdle {
            assertThat(selection1.value).isNull()
            assertThat(selection2.value).isNotNull()
            // There will be 2 times from the first SelectionContainer and 1 time from the second
            // SelectionContainer.
            verify(
                hapticFeedback,
                times(3)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    private fun createSelectionContainer(isRtl: Boolean = false) {
        val measureLatch = CountDownLatch(1)

        val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides hapticFeedback,
                LocalLayoutDirection provides layoutDirection,
                LocalTextToolbar provides mock()
            ) {
                Column {
                    SelectionContainer(
                        modifier = Modifier.onGloballyPositioned {
                            measureLatch.countDown()
                        }.testTag("selectionContainer1"),
                        selection = selection1.value,
                        onSelectionChange = {
                            selection1.value = it
                        }
                    ) {
                        CoreText(
                            AnnotatedString(textContent),
                            Modifier.fillMaxWidth(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            maxLines = Int.MAX_VALUE,
                            inlineContent = mapOf(),
                            onTextLayout = {}
                        )
                    }

                    SelectionContainer(
                        modifier = Modifier.onGloballyPositioned {
                            measureLatch.countDown()
                        }.testTag("selectionContainer2"),
                        selection = selection2.value,
                        onSelectionChange = {
                            selection2.value = it
                        }
                    ) {
                        CoreText(
                            AnnotatedString(textContent),
                            Modifier.fillMaxWidth(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            maxLines = Int.MAX_VALUE,
                            inlineContent = mapOf(),
                            onTextLayout = {}
                        )
                    }

                    Box(Modifier.size(boxSize, boxSize).testTag("box"))
                }
            }
        }
        rule.activityRule.scenario.onActivity {
            view = it.findViewById<ViewGroup>(android.R.id.content)
        }
    }
}