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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextMinMaxLinesTest {

    @get:Rule
    val rule = createComposeRule()

    private val longText = "Lorem ipsum\n".repeat(10)
    private val shortText = "Lorem ipsum"

    @Test
    fun higherMinLines_shouldResultInHigherHeight() {
        var size1 = 0
        var size2 = 0
        val minLines1 = 1
        val minLines2 = 2
        val maxLines1 = 3
        val maxLines2 = 3

        rule.setContent {
            BasicText(
                shortText,
                minLines = minLines1,
                maxLines = maxLines1,
                onTextLayout = {
                    size1 = it.size.height
                }
            )

            BasicText(
                shortText,
                minLines = minLines2,
                maxLines = maxLines2,
                onTextLayout = {
                    size2 = it.size.height
                }
            )
        }

        rule.runOnIdle {
            assertThat(size2).isGreaterThan(size1)
        }
    }

    @Test
    fun lowerMaxLines_shouldResultInLowerHeight() {
        var size1 = 0
        var size2 = 0
        val minLines1 = 1
        val minLines2 = 1
        val maxLines1 = 3
        val maxLines2 = 2

        rule.setContent {
            BasicText(
                longText,
                minLines = minLines1,
                maxLines = maxLines1,
                onTextLayout = {
                    size1 = it.size.height
                }
            )

            BasicText(
                longText,
                minLines = minLines2,
                maxLines = maxLines2,
                onTextLayout = {
                    size2 = it.size.height
                }
            )
        }

        rule.runOnIdle {
            assertThat(size2).isLessThan(size1)
        }
    }

    @Test
    fun heightShouldGetLarger_asMinLinesIncrease() {
        val size = mutableListOf<Int>()
        var minLines by mutableStateOf(2)

        rule.setContent {
            BasicText(
                shortText,
                minLines = minLines,
                onTextLayout = {
                    size += it.size.height
                }
            )
        }

        minLines += 1

        rule.runOnIdle {
            assertThat(size.size).isEqualTo(2)
            assertThat(size[0]).isLessThan(size[1])
        }
    }

    @Test
    fun heightShouldGetSmaller_asMaxLinesDecrease() {
        val size = mutableListOf<Int>()
        var maxLines by mutableStateOf(5)

        rule.setContent {
            BasicText(
                longText,
                maxLines = maxLines,
                onTextLayout = {
                    size += it.size.height
                }
            )
        }

        maxLines -= 1

        rule.runOnIdle {
            assertThat(size.size).isEqualTo(2)
            assertThat(size[1]).isLessThan(size[0])
        }
    }

    @Test
    fun ifMinLinesAreIntroducedLater_itShouldTakeAffect() {
        val size = mutableListOf<Int>()
        var minLines by mutableStateOf(1)

        rule.setContent {
            BasicText(
                shortText,
                minLines = minLines,
                onTextLayout = {
                    size += it.size.height
                }
            )
        }

        minLines += 1

        rule.runOnIdle {
            assertThat(size.size).isEqualTo(2)
            assertThat(size[1]).isGreaterThan(size[0])
        }
    }

    @Test
    fun ifMaxLinesAreIntroducedLater_itShouldTakeAffect() {
        val size = mutableListOf<Int>()
        var maxLines by mutableStateOf(Int.MAX_VALUE)

        rule.setContent {
            BasicText(
                longText,
                maxLines = maxLines,
                onTextLayout = {
                    size += it.size.height
                }
            )
        }

        maxLines = 2

        rule.runOnIdle {
            assertThat(size.size).isEqualTo(2)
            assertThat(size[1]).isLessThan(size[0])
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMaxLinesGreaterThanMinLines() {
        rule.setContent {
            BasicText(
                shortText,
                minLines = 2,
                maxLines = 1
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMinLinesPositive() {
        rule.setContent {
            BasicText(
                shortText,
                minLines = 0,
                maxLines = 1
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMaxLinesPositive() {
        rule.setContent {
            BasicText(
                shortText,
                minLines = 1,
                maxLines = 0
            )
        }
    }
}