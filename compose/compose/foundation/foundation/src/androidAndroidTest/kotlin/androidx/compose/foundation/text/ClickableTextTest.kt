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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onclick_callback() {
        val onClick: (Int) -> Unit = mock()
        rule.setContent {
            ClickableText(
                modifier = Modifier.testTag("clickableText"),
                text = AnnotatedString("android"),
                onClick = onClick
            )
        }

        rule.onNodeWithTag("clickableText").performClick()

        rule.runOnIdle {
            verify(onClick, times(1)).invoke(any())
        }
    }

    @Test
    fun onclick_callback_whenCallbackIsUpdated() {
        val onClick1: (Int) -> Unit = mock()
        val onClick2: (Int) -> Unit = mock()
        val use2 = mutableStateOf(false)
        rule.setContent {
            ClickableText(
                modifier = Modifier.testTag("clickableText"),
                text = AnnotatedString("android"),
                onClick = if (use2.value) onClick2 else onClick1
            )
        }
        use2.value = true
        rule.waitForIdle()

        rule.onNodeWithTag("clickableText").performClick()

        rule.runOnIdle {
            verify(onClick1, times(0)).invoke(any())
            verify(onClick2, times(1)).invoke(any())
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
    @Test
    fun onhover_callback() {
        val onHover: (Int?) -> Unit = mock()
        val onClick: (Int) -> Unit = mock()
        rule.setContent {
            ClickableText(
                modifier = Modifier.testTag("clickableText"),
                text = AnnotatedString("android"),
                onHover = onHover,
                onClick = onClick,
            )
        }

        rule.onNodeWithTag("clickableText")
            .performMouseInput {
                moveTo(Offset(-1f, -1f), 0) // outside bounds
                moveTo(Offset(1f, 1f), 0) // inside bounds
                moveTo(Offset(-1f, -1f), 0) // outside bounds again
                moveTo(Offset(1f, 1f), 0) // inside bounds again
                moveTo(Offset(1f, 2f), 0) // move but stay on the same character
                moveTo(Offset(50f, 1f), 0) // move to different character
            }

        rule.runOnIdle {
            onHover.inOrder {
                verify().invoke(0) // first enter
                verify().invoke(null) // first exit
                verify().invoke(0) // second enter
                verify().invoke(argWhere { it > 0 }) // move to different character
                verifyNoMoreInteractions()
            }
            verifyZeroInteractions(onClick)
        }
    }
}
