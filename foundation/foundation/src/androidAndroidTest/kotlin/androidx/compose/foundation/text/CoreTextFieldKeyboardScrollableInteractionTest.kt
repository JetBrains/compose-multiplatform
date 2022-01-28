/*
 * Copyright 2022 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.CoreTextFieldKeyboardScrollableInteractionTest.ScrollableType.LazyList
import androidx.compose.foundation.text.CoreTextFieldKeyboardScrollableInteractionTest.ScrollableType.ScrollableColumn
import androidx.compose.foundation.text.CoreTextFieldKeyboardScrollableInteractionTest.SoftInputMode.AdjustPan
import androidx.compose.foundation.text.CoreTextFieldKeyboardScrollableInteractionTest.SoftInputMode.AdjustResize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class CoreTextFieldKeyboardScrollableInteractionTest(
    private val scrollableType: ScrollableType,
    private val softInputMode: SoftInputMode,
    private val withDecorationPadding: Boolean,
) {
    enum class ScrollableType {
        ScrollableColumn,
        LazyList
    }

    enum class SoftInputMode {
        AdjustResize,
        AdjustPan
    }

    companion object {
        @JvmStatic
        @Parameters(name = "scrollableType={0}, softInputMode={1}, withDecorationPadding={2}")
        fun parameters(): Iterable<Array<*>> = crossProductOf(
            ScrollableType.values(),
            SoftInputMode.values(),
            arrayOf(false, true), // withDecorationPadding
        )
    }

    @get:Rule
    val rule = createComposeRule()

    private val ListTag = "list"
    private val keyboardHelper = KeyboardHelper(rule)

    @Test
    fun test() {
        // TODO(b/192043120) This is known broken when soft input mode is Resize.
        assumeTrue(softInputMode != AdjustResize)

        rule.setContent {
            keyboardHelper.view = LocalView.current
            TestContent()
        }

        // This test is all about the keyboard going from hidden to shown, so hide it to start.
        keyboardHelper.hideKeyboardIfShown()

        rule.onNodeWithTag(ListTag)
            .performTouchInput {
                // Click one pixel above the bottom of the list.
                click(bottomCenter - Offset(0f, 1f))
            }
        keyboardHelper.waitForKeyboardVisibility(visible = true)

        rule.onNode(isFocused()).assertIsDisplayed()
    }

    @Composable
    fun TestContent() {
        @Suppress("DEPRECATION")
        SoftInputMode(
            when (softInputMode) {
                AdjustResize -> SOFT_INPUT_ADJUST_RESIZE
                AdjustPan -> SOFT_INPUT_ADJUST_PAN
            }
        )

        val itemCount = 100
        when (scrollableType) {
            ScrollableColumn -> {
                Column(
                    Modifier
                        .testTag(ListTag)
                        .verticalScroll(rememberScrollState())
                ) {
                    repeat(itemCount) { index ->
                        TestTextField(index)
                    }
                }
            }
            LazyList -> {
                LazyColumn(Modifier.testTag(ListTag)) {
                    items(itemCount) { index ->
                        TestTextField(index)
                    }
                }
            }
        }
    }

    @Composable
    private fun TestTextField(index: Int) {
        var isFocused by remember { mutableStateOf(false) }
        CoreTextField(
            value = TextFieldValue(text = index.toString()),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    if (isFocused) {
                        drawRect(Color.Blue, style = Stroke(2.dp.toPx()))
                    }
                }
                .onFocusChanged { isFocused = it.isFocused }
                .testTag(index.toString()),
            decorationBox = { inner ->
                if (withDecorationPadding) {
                    Box(Modifier.padding(vertical = 24.dp)) {
                        inner()
                    }
                } else {
                    inner()
                }
            }
        )
    }

    @Composable
    private fun SoftInputMode(mode: Int) {
        val context = LocalContext.current
        DisposableEffect(mode) {
            val activity = context.findActivityOrNull() ?: return@DisposableEffect onDispose {}
            val originalMode = activity.window.attributes.softInputMode
            activity.window.setSoftInputMode(mode)
            onDispose {
                activity.window.setSoftInputMode(originalMode)
            }
        }
    }

    private tailrec fun Context.findActivityOrNull(): Activity? {
        return (this as? Activity)
            ?: (this as? ContextWrapper)?.baseContext?.findActivityOrNull()
    }
}

private fun crossProductOf(vararg values: Array<*>): List<Array<*>> =
    crossProductOf(values.map { it.asSequence() })
        .map { it.toList().toTypedArray() }
        .toList()

private fun crossProductOf(values: List<Sequence<*>>): Sequence<Sequence<*>> =
    when (values.size) {
        0 -> emptySequence()
        1 -> values[0].map { sequenceOf(it) }
        else -> sequence {
            for (subProduct in crossProductOf(values.subList(1, values.size)))
                for (firstValue in values[0]) {
                    yield(sequenceOf(firstValue) + subProduct)
                }
        }
    }