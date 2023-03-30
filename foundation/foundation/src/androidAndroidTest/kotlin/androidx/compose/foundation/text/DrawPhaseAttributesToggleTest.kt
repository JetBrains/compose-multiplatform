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

import android.os.Build
import androidx.compose.foundation.text.matchers.assertThat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@MediumTest
class DrawPhaseAttributesToggleTest(private val config: Config) {

    private val textTag = "text"

    class Config(
        private val description: String,
        val updateStyle: (TextStyle) -> TextStyle,
        val initializeStyle: (TextStyle) -> TextStyle = { it }
    ) {
        override fun toString(): String = "toggling $description"
    }

    @OptIn(ExperimentalTextApi::class)
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            Config(
                "color unspecified/color/unspecified",
                initializeStyle = { it.copy(color = Color.Unspecified) },
                updateStyle = { it.copy(color = Color.Blue) },
            ),
            Config(
                "color colorA/colorB/colorA",
                initializeStyle = { it.copy(color = Color.Black) },
                updateStyle = { it.copy(color = Color.Blue) },
            ),
            Config(
                "color colorA/brushA/colorA",
                initializeStyle = {
                    it.copy(color = Color.Red)
                },
                updateStyle = {
                    it.copy(brush = Brush.verticalGradient(listOf(Color.Blue, Color.Magenta)))
                }
            ),
            Config(
                "brush brushA/brushB/brushA",
                initializeStyle = {
                    it.copy(brush = Brush.horizontalGradient(listOf(Color.Black, Color.Blue)))
                },
                updateStyle = {
                    it.copy(brush = Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
                }
            ),
            Config(
                "brush brushA/colorA/brushA",
                initializeStyle = {
                    it.copy(brush = Brush.horizontalGradient(listOf(Color.Black, Color.Blue)))
                },
                updateStyle = {
                    it.copy(color = Color.Red)
                }
            ),
            Config(
                "alpha",
                initializeStyle = {
                    it.copy(
                        alpha = 1f,
                        brush = Brush.verticalGradient(0f to Color.Blue, 1f to Color.Magenta)
                    )
                },
                updateStyle = { it.copy(alpha = 0.5f, brush = it.brush) },
            ),
            Config(
                "textDecoration none/lineThrough/none",
                initializeStyle = { it.copy(textDecoration = TextDecoration.None) },
                updateStyle = { it.copy(textDecoration = TextDecoration.LineThrough) }
            ),
            Config(
                "textDecoration lineThrough/none/lineThrough",
                initializeStyle = { it.copy(textDecoration = TextDecoration.LineThrough) },
                updateStyle = { it.copy(textDecoration = TextDecoration.None) }
            ),
            Config(
                "textDecoration null/lineThrough/null",
                initializeStyle = { it.copy(textDecoration = null) },
                updateStyle = { it.copy(textDecoration = TextDecoration.LineThrough) }
            ),
            Config(
                "shadow null/shadow/null",
                initializeStyle = { it.copy(shadow = null) },
                updateStyle = { it.copy(shadow = Shadow(Color.Black, blurRadius = 4f)) }
            ),
            Config(
                "shadow shadowA/shadowB/shadowA",
                initializeStyle = { it.copy(shadow = Shadow(Color.Black, blurRadius = 1f)) },
                updateStyle = { it.copy(shadow = Shadow(Color.Black, blurRadius = 4f)) }
            ),
            Config(
                "shadow shadowA/null/shadowA",
                initializeStyle = { it.copy(shadow = Shadow(Color.Black, blurRadius = 1f)) },
                updateStyle = { it.copy(shadow = null) }
            ),
            Config(
                "drawStyle null/drawStyle/null",
                initializeStyle = { it.copy(drawStyle = null) },
                updateStyle = { it.copy(drawStyle = Stroke(width = 2f)) }
            ),
            Config(
                "drawStyle drawStyleA/drawStyleB/drawStyleA",
                initializeStyle = { it.copy(drawStyle = Stroke(width = 1f)) },
                updateStyle = { it.copy(drawStyle = Stroke(width = 2f)) }
            ),
            Config(
                "drawStyle drawStyle/null/drawStyle",
                initializeStyle = { it.copy(drawStyle = Stroke(width = 1f)) },
                updateStyle = { it.copy(drawStyle = null) }
            ),
            Config(
                "drawStyle stroke/fill/stroke",
                initializeStyle = { it.copy(drawStyle = Stroke(width = 1f)) },
                updateStyle = { it.copy(drawStyle = Fill) }
            )
        )
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun basicText() {
        var style by mutableStateOf(
            TextStyle(
                color = Color.Black,
                textDecoration = null,
                shadow = null
            ).let(config.initializeStyle)
        )

        rule.setContent {
            BasicText(
                "TextPainter",
                style = style,
                modifier = Modifier.testTag(textTag)
            )
        }

        rule.waitForIdle()
        val initialBitmap = rule.onNodeWithTag(textTag).captureToImage().asAndroidBitmap()

        style = config.updateStyle(style)

        rule.waitForIdle()
        val updatedBitmap = rule.onNodeWithTag(textTag).captureToImage().asAndroidBitmap()
        assertThat(initialBitmap).isNotEqualToBitmap(updatedBitmap)

        style = config.initializeStyle(style)

        rule.waitForIdle()
        val finalBitmap = rule.onNodeWithTag(textTag).captureToImage().asAndroidBitmap()
        assertThat(finalBitmap).isNotEqualToBitmap(updatedBitmap)

        assertThat(finalBitmap).isEqualToBitmap(initialBitmap)
    }

    @Test
    fun basicTextField() {
        var style by mutableStateOf(config.initializeStyle(TextStyle(color = Color.Black)))

        rule.setContent {
            BasicTextField(
                "ABC",
                onValueChange = {},
                textStyle = style,
                modifier = Modifier.testTag(textTag)
            )
        }

        rule.waitForIdle()
        val initialBitmap = rule.onNodeWithTag(textTag).captureToImage().asAndroidBitmap()

        style = config.updateStyle(style)

        rule.waitForIdle()
        val updatedBitmap = rule.onNodeWithTag(textTag).captureToImage().asAndroidBitmap()
        assertThat(initialBitmap).isNotEqualToBitmap(updatedBitmap)

        style = config.initializeStyle(style)

        rule.waitForIdle()
        val finalBitmap = rule.onNodeWithTag(textTag).captureToImage().asAndroidBitmap()
        assertThat(finalBitmap).isNotEqualToBitmap(updatedBitmap)

        assertThat(finalBitmap).isEqualToBitmap(initialBitmap)
    }
}