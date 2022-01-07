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

import android.os.Build
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.TextFieldDelegate
import androidx.compose.foundation.text.TextFieldState
import androidx.compose.foundation.text.TextLayoutResultProxy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalFoundationTextApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
internal class TextFieldMagnifierTest : AbstractSelectionMagnifierTests() {

    @Composable
    override fun TestContent(
        text: String,
        modifier: Modifier,
        style: TextStyle,
        onTextLayout: (TextLayoutResult) -> Unit
    ) {
        BasicTextField(
            text,
                onValueChange = {},
                modifier = modifier,
                textStyle = style,
                onTextLayout = onTextLayout
            )
        }

    @Test
    fun magnifier_appears_whileStartCursorTouched() {
        checkMagnifierAppears_whileHandleTouched(Handle.Cursor)
    }

    @Test
    fun magnifier_followsCursorHorizontally_whenDragged() {
        checkMagnifierFollowsHandleHorizontally(Handle.Cursor)
    }

    @Test
    fun magnifier_staysAtLineEnd_whenCursorDraggedPastStart() {
        checkMagnifierConstrainedToLineHorizontalBounds(
            Handle.Cursor,
            checkStart = true
        )
    }

    @Test
    fun magnifier_staysAtLineEnd_whenCursorDraggedPastEnd() {
        checkMagnifierConstrainedToLineHorizontalBounds(
            Handle.Cursor,
            checkStart = false
        )
    }

    @Test
    fun magnifier_hidden_whenCursorDraggedFarPastStartOfLine() {
        checkMagnifierHiddenWhenDraggedTooFar(Handle.Cursor, checkStart = true)
    }

    @Test
    fun magnifier_hidden_whenCursorDraggedFarPastEndOfLine() {
        checkMagnifierHiddenWhenDraggedTooFar(Handle.Cursor, checkStart = false)
    }

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
        val center = calculateSelectionMagnifierCenterAndroid(manager, defaultMagnifierSize)

        assertThat(center).isEqualTo(Offset.Unspecified)
    }

    private fun setupSelectionManagedMagnifier(): TextFieldSelectionManager {
        val selectionManager = TextFieldSelectionManager()
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val density = LocalDensity.current
            selectionManager.value = TextFieldValue(Text)
            val scope = currentRecomposeScope
            // The value won't ever change so we don't need to worry about ever updating the state.
            selectionManager.state = remember {
                TextFieldState(
                    TextDelegate(
                        text = AnnotatedString(Text),
                        style = TextStyle.Default,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver
                    ),
                    scope
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
        }
        return selectionManager
    }

    private companion object {
        const val Text = "hello"
    }
}
