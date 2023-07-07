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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil
import kotlin.math.roundToInt

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaseTextFieldDefaultWidthTest {

    @get:Rule
    val rule = createComposeRule()

    val density = Density(density = 1f, fontScale = 1f)

    @Test
    fun textField_hasDefaultWidth() {
        var size: Int? = null
        val fontSize = 4.sp

        rule.setContent {
            DefaultWidthTextField(
                text = "",
                fontSize = fontSize,
                modifier = Modifier.onGloballyPositioned { size = it.size.width },
                density = density
            )
        }

        assertThat(size).isEqualTo(defaultTextFieldSize(fontSize))
    }

    @Test
    fun minConstraint_greaterThan_defaultWidth_choosesMinConstraint() {
        var size: Int? = null
        val fontSize = 4.sp
        val minWidth = 80.dp

        rule.setContent {
            DefaultWidthTextField(
                text = "",
                fontSize = fontSize,
                modifier = Modifier
                    .defaultMinSize(minWidth)
                    .onGloballyPositioned { size = it.size.width },
                density = density
            )
        }

        with(density) {
            assertThat(size).isEqualTo(minWidth.roundToPx())
        }
    }

    @Test
    fun maxConstraint_smallerThan_defaultWidth_choosesMaxConstraint() {
        var size: Int? = null
        val fontSize = 4.sp
        val width = 0.dp

        rule.setContent {
            DefaultWidthTextField(
                text = "",
                fontSize = fontSize,
                modifier = Modifier
                    .requiredWidth(width)
                    .onGloballyPositioned { size = it.size.width },
                density = density
            )
        }

        with(density) {
            assertThat(size).isEqualTo(width.roundToPx())
        }
    }

    @Test
    fun textWidth_smallerThan_defaultWidth_choosesDefaultWidth() {
        var size: Int? = null
        val fontSize = 4.sp

        rule.setContent {
            DefaultWidthTextField(
                text = "abc",
                fontSize = fontSize,
                modifier = Modifier.onGloballyPositioned { size = it.size.width },
                density = density
            )
        }

        assertThat(size).isEqualTo(defaultTextFieldSize(fontSize))
    }

    @Test
    fun textWidth_greaterThan_defaultWidth_choosesTextWidth() {
        var size: Int? = null
        val fontSize = 4.sp
        val charCount = 12

        rule.setContent {
            DefaultWidthTextField(
                text = "H".repeat(charCount),
                fontSize = fontSize,
                modifier = Modifier.onGloballyPositioned { size = it.size.width },
                density = density
            )
        }

        assertThat(size).isEqualTo(defaultTextFieldSize(fontSize, charCount))
    }

    @Test
    fun respectsWidthSetByModifier() {
        val textFieldWidth = 100.dp
        val fontSize = 4.sp

        var size: Int? = null
        rule.setContent {
            DefaultWidthTextField(
                text = "abc",
                fontSize = fontSize,
                modifier = Modifier
                    .width(textFieldWidth)
                    .onGloballyPositioned { size = it.size.width },
                density = density
            )
        }

        with(density) {
            assertThat(size).isEqualTo(textFieldWidth.roundToPx())
        }
    }

    private fun defaultTextFieldSize(
        fontSize: TextUnit,
        charCount: Int = 10 // 10 is the configuration for default TextField width in glyphs
    ): Int = with(density) {
        ceil(charCount * fontSize.toPx()).roundToInt()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DefaultWidthTextField(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier,
    density: Density
) {
    CompositionLocalProvider(LocalDensity provides density) {
        androidx.compose.foundation.layout.Box {
            BasicTextField(
                value = text,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                onValueChange = {},
                modifier = modifier
            )
        }
    }
}
