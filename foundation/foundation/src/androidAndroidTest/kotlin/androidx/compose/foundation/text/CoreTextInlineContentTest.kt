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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextInlineContentTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun placeholder_changeSize_updateInlineContentSize() {
        // Callback to monitor the size changes of a composable.
        val onSizeChanged: (IntSize) -> Unit = mock()
        var size by mutableStateOf(IntSize(50, 50))

        rule.setContent {
            val inlineTextContent = InlineTextContent(
                placeholder = Placeholder(
                    size.width.sp,
                    size.height.sp,
                    PlaceholderVerticalAlign.AboveBaseline
                )
            ) {
                Box(modifier = Modifier.fillMaxSize().onSizeChanged(onSizeChanged))
            }

            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 1f)
            ) {
                CoreText(
                    text = buildAnnotatedString {
                        append("Hello")
                        appendInlineContent("box")
                        append("World")
                    },
                    style = TextStyle(fontSize = 100.sp),
                    inlineContent = mapOf("box" to inlineTextContent),
                    maxLines = Int.MAX_VALUE,
                    onTextLayout = {},
                    overflow = TextOverflow.Clip,
                    softWrap = true
                )
            }
        }

        rule.runOnIdle {
            // Verify that the initial size is (50, 50).
            verify(onSizeChanged).invoke(IntSize(50, 50))
            size = IntSize(100, 100)
        }
        rule.waitForIdle()
        // Verify that the size has been updated to (100, 100).
        verify(onSizeChanged).invoke(IntSize(100, 100))
    }
}