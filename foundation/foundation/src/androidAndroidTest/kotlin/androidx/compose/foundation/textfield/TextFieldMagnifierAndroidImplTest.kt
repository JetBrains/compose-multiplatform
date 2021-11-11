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

package androidx.compose.foundation.textfield

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.TextFieldDelegate
import androidx.compose.foundation.text.TextFieldMagnifierOffsetProperty
import androidx.compose.foundation.text.TextFieldState
import androidx.compose.foundation.text.TextLayoutResultProxy
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.textFieldMagnifier
import androidx.compose.foundation.text.textFieldMagnifierAndroidImpl
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalFoundationTextApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldMagnifierAndroidImplTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun androidSupportsTextMagnifierOn28AndAbove() {
        // Need a non-null state to return a real modifier.
        val modifier = Modifier.textFieldMagnifier(setupSelectionManagedMagnifier())

        if (Build.VERSION.SDK_INT >= 28) {
            assertThat(modifier).isNotSameInstanceAs(Modifier)
        } else {
            assertThat(modifier).isSameInstanceAs(Modifier)
        }
    }

    @Test
    fun doesNotModify_whenStateIsNull() {
        val manager = TextFieldSelectionManager()
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        manager = manager,
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true,
                    )
            )
        }

        onMagnifierNode().assertDoesNotExist()
    }

    @Test
    fun hidesMagnifier_whenGetCursorRectReturnsNull() {
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        draggingHandle = { null },
                        fieldValue = { TextFieldValue() },
                        transformTextOffset = { it },
                        getCursorRect = { null },
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true
                    )
            )
        }

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset.Unspecified)
    }

    @Test
    fun hidesMagnifier_whenNotDraggingHandle() {
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        draggingHandle = { null },
                        fieldValue = { TextFieldValue() },
                        transformTextOffset = { it },
                        getCursorRect = { Rect.Zero },
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true
                    )
            )
        }

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset.Unspecified)
    }

    @Test
    fun showsMagnifier_whenDraggingCursor() {
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        draggingHandle = { Handle.Cursor },
                        fieldValue = { TextFieldValue("hello", selection = TextRange(3)) },
                        transformTextOffset = { it },
                        getCursorRect = {
                            Rect(left = it.toFloat(), top = 0f, right = it.toFloat(), bottom = 2f)
                        },
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true
                    )
            )
        }

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset(3f, 1f))
    }

    @Test
    fun showsMagnifier_whenDraggingStart() {
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        draggingHandle = { Handle.SelectionStart },
                        fieldValue = {
                            TextFieldValue(
                                "hello",
                                selection = TextRange(start = 1, end = 4)
                            )
                        },
                        transformTextOffset = { it },
                        getCursorRect = {
                            Rect(left = it.toFloat(), top = 0f, right = it.toFloat(), bottom = 2f)
                        },
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true
                    )
            )
        }

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset(1f, 1f))
    }

    @Test
    fun showsMagnifier_whenDraggingEnd() {
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        draggingHandle = { Handle.SelectionEnd },
                        fieldValue = {
                            TextFieldValue(
                                "hello",
                                selection = TextRange(start = 1, end = 4)
                            )
                        },
                        transformTextOffset = { it },
                        getCursorRect = {
                            Rect(left = it.toFloat(), top = 0f, right = it.toFloat(), bottom = 2f)
                        },
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true
                    )
            )
        }

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset(4f, 1f))
    }

    @Test
    fun magnifierFollowsCursorDrag() {
        testMagnifierDragging { cursorDragObserver() }
    }

    @Test
    fun magnifierFollowsSelectionStartDrag() {
        testMagnifierDragging { handleDragObserver(isStartHandle = true) }
    }

    @Test
    fun magnifierFollowsSelectionEndDrag() {
        testMagnifierDragging { handleDragObserver(isStartHandle = false) }
    }

    private fun testMagnifierDragging(
        dragObserver: TextFieldSelectionManager.() -> TextDragObserver
    ) {
        val selectionManager = setupSelectionManagedMagnifier()

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset.Unspecified)

        // Start dragging the handle in the top-left corner so that the cursor will be before the
        // first character.
        rule.runOnIdle {
            selectionManager.dragObserver().onStart(Offset.Zero)
        }

        // Make sure the magnifier showed.
        onMagnifierNode().assertMagnifiedOffsetEquals(selectionManager.getCursorCenter(0))

        // Move the handle.
        rule.runOnIdle {
            selectionManager.dragObserver().onDrag(Offset.Infinite)
        }

        // Magnifier should be centered after the last character.
        onMagnifierNode().assertMagnifiedOffsetEquals(
            selectionManager.getCursorCenter(Text.length)
        )

        // Stop dragging the handle.
        rule.runOnIdle {
            selectionManager.dragObserver().onStop()
        }

        onMagnifierNode().assertMagnifiedOffsetEquals(Offset.Unspecified)
    }

    private fun TextFieldSelectionManager.getCursorCenter(offset: Int): Offset {
        val expectedCursorPosition = state!!.layoutResult!!.value.getCursorRect(offset)
        assertThat(expectedCursorPosition).isNotEqualTo(Offset.Zero)
        assertThat(expectedCursorPosition).isNotEqualTo(Offset.Unspecified)
        return expectedCursorPosition.center
    }

    private fun setupSelectionManagedMagnifier(): TextFieldSelectionManager {
        val selectionManager = TextFieldSelectionManager()
        rule.setContent {
            val resourceLoader = LocalFontLoader.current
            val density = LocalDensity.current
            selectionManager.value = TextFieldValue(Text)
            // The value won't ever change so we don't need to worry about ever updating the state.
            selectionManager.state = remember {
                TextFieldState(
                    TextDelegate(
                        text = AnnotatedString(Text),
                        style = TextStyle.Default,
                        density = density,
                        resourceLoader = resourceLoader
                    )
                )
            }
            // Required for the drag observers to actually update the selection.
            selectionManager.onValueChange = {
                selectionManager.value = it
            }

            DisposableEffect(Unit) {
                val (_, _, result) = TextFieldDelegate.layout(
                    selectionManager.state!!.textDelegate,
                    constraints = Constraints(),
                    layoutDirection = LayoutDirection.Ltr
                )
                selectionManager.state!!.layoutResult = TextLayoutResultProxy(result)
                onDispose {}
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .textFieldMagnifierAndroidImpl(
                        manager = selectionManager,
                        androidMagnifier = { Modifier },
                        isTextMagnifierSemanticsEnabled = true
                    )
            )
        }
        return selectionManager
    }

    private fun onMagnifierNode(): SemanticsNodeInteraction =
        rule.onNode(
            useUnmergedTree = true,
            matcher = SemanticsMatcher("node with TextFieldMagnifierOffsetProperty") { node ->
                TextFieldMagnifierOffsetProperty in node.config
            })

    private fun SemanticsNodeInteraction.assertMagnifiedOffsetEquals(expectedOffset: Offset) {
        val actualOffset = fetchSemanticsNode().config[TextFieldMagnifierOffsetProperty]
        assertThat(actualOffset).isEqualTo(expectedOffset)
    }

    private companion object {
        const val Text = "hello"
    }
}
