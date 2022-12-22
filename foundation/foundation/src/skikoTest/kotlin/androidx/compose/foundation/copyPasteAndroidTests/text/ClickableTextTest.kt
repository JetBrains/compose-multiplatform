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

package androidx.compose.foundation.copyPasteAndroidTests.text

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ClickableTextTest {

    @Test
    fun onclick_callback() = runSkikoComposeUiTest {
        var counter = 0
        val onClick: (Int) -> Unit = { counter++ }
        setContent {
            ClickableText(
                modifier = Modifier.testTag("clickableText"),
                text = AnnotatedString("android"),
                onClick = onClick
            )
        }

        onNodeWithTag("clickableText").performClick()

        runOnIdle {
            assertEquals(1, counter)
        }
    }

    @Test
    fun onclick_callback_whenCallbackIsUpdated() = runSkikoComposeUiTest {
        var counter1 = 0
        var counter2 = 0
        val onClick1: (Int) -> Unit = { counter1++ }
        val onClick2: (Int) -> Unit = { counter2++ }
        val use2 = mutableStateOf(false)
        setContent {
            ClickableText(
                modifier = Modifier.testTag("clickableText"),
                text = AnnotatedString("android"),
                onClick = if (use2.value) onClick2 else onClick1
            )
        }
        use2.value = true
        waitForIdle()

        onNodeWithTag("clickableText").performClick()

        runOnIdle {
            assertEquals(0, counter1)
            assertEquals(1, counter2)
        }
    }
}
