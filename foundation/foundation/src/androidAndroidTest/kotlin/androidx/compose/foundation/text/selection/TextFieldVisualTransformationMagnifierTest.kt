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

import androidx.compose.foundation.MagnifierPositionInRoot
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.RequiresDevice
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.sign
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@SdkSuppress(minSdkVersion = 28)
@RunWith(Parameterized::class)
internal class TextFieldVisualTransformationMagnifierTest(
    val config: VisualTransformationMagnifierTestConfig
) {

    @get:Rule
    val rule = createComposeRule()

    private val tag = "tag"

    @Composable
    fun TestContent(
        text: String,
        modifier: Modifier
    ) {
        BasicTextField(
            text,
            onValueChange = {},
            modifier = modifier,
            visualTransformation = config.visualTransformation
        )
    }

    @Test
    fun magnifier_appears_whileHandleTouched() {
        rule.setContent {
            TestContent(
                if (config.layoutDirection == LayoutDirection.Ltr) {
                    "text ".repeat(10)
                } else {
                    "באמת ".repeat(10)
                },
                Modifier.testTag(tag)
            )
        }

        val handle = config.handle
        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }

        assertThat(getMagnifierCenterOffset()).isNotEqualTo(Offset.Zero)
    }

    @Ignore("b/266233836")
    @RequiresDevice // b/264701475
    @Test
    fun checkMagnifierFollowsHandleHorizontally() {
        val handle = config.handle
        val layoutDirection = config.layoutDirection
        val dragDistance = Offset(if (layoutDirection == LayoutDirection.Ltr) 1f else -1f, 0f)
        rule.setContent {
            TestContent(
                if (layoutDirection == LayoutDirection.Ltr) {
                    "text ".repeat(10)
                } else {
                    "באמת ".repeat(10)
                },
                Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag)
            )
        }

        showHandle(handle)

        // Touch the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { down(center) }
        val magnifierInitialPosition = getMagnifierCenterOffset()

        // Drag the handle horizontally - the magnifier should follow.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput { movePastSlopBy(dragDistance) }

        assertThat(getMagnifierCenterOffset())
            .isEqualTo(magnifierInitialPosition + dragDistance)
    }

    // Below utility functions were taken from AbstractSelectionMagnifierTests.
    private fun isSelectionHandle(handle: Handle) = SemanticsMatcher("is $handle handle") { node ->
        node.config.getOrNull(SelectionHandleInfoKey)?.handle == handle
    }

    private fun showHandle(handle: Handle) = with(rule.onNodeWithTag(tag)) {
        if (handle == Handle.Cursor) {
            performClick()
        } else {
            // TODO(b/209698586) Select programmatically once that's fixed.
            performTouchInput { longClick() }
        }
    }

    /**
     * Moves the first pointer by [delta] past the touch slop threshold on each axis.
     * If [delta] is 0 on either axis it will stay 0.
     */
    // TODO(b/210545925) This is here because we can't disable the touch slop in a popup. When
    //  that's fixed we can just disable slop and delete this function.
    private fun TouchInjectionScope.movePastSlopBy(delta: Offset) {
        val slop = Offset(
            x = viewConfiguration.touchSlop * delta.x.sign,
            y = viewConfiguration.touchSlop * delta.y.sign
        )
        moveBy(delta + slop)
    }

    private fun getMagnifierCenterOffset(): Offset =
        rule.onNode(SemanticsMatcher.keyIsDefined(MagnifierPositionInRoot))
            .fetchSemanticsNode()
            .config[MagnifierPositionInRoot]
            .let(rule::runOnIdle)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<VisualTransformationMagnifierTestConfig>().apply {
            val visualTransformations = listOf(
                ReducedVisualTransformation(),
                IncreasedVisualTransformation()
            )
            for (handle in Handle.values()) {
                for (vt in visualTransformations) {
                    for (ld in LayoutDirection.values()) {
                        add(VisualTransformationMagnifierTestConfig(vt, ld, handle))
                    }
                }
            }
        }
    }
}

internal class VisualTransformationMagnifierTestConfig(
    val visualTransformation: VisualTransformation,
    val layoutDirection: LayoutDirection,
    val handle: Handle
) {
    override fun toString(): String {
        return "visualTransformation=$visualTransformation " +
            "layoutDirection=$layoutDirection " +
            "handle=$handle"
    }
}

/**
 * Adds a `-` between every single character in the original text
 */
internal class IncreasedVisualTransformation(private val char: Char = 'a') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(text.text.map { "${it}$char" }.joinToString("")),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = 2 * offset

                override fun transformedToOriginal(offset: Int) = offset / 2
            }
        )
    }

    override fun toString(): String = "IncreasedVisualTransformation($char)"
}

/**
 * Removes every odd indexed character.
 *
 * `abcde` becomes `ace`
 * `abcdef` becomes `ace`
 * `` becomes ``
 * `a` becomes `a`
 */
internal class ReducedVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(text.text.filterIndexed { index, _ -> index % 2 == 0 }),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset / 2

                override fun transformedToOriginal(offset: Int) = offset * 2
            }
        )
    }

    override fun toString(): String = "ReducedVisualTransformation"
}
