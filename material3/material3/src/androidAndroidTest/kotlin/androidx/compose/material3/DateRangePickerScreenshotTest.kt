/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class DateRangePickerScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.Center)
    private val wrapperTestTag = "dateRangePickerWrapper"

    @Test
    fun dateRangePicker_initialMonth() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 1, dayOfMonth = 1)
                DateRangePicker(
                    state = rememberDateRangePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis
                    )
                )
            }
        }
        assertAgainstGolden("dateRangePicker_initialMonth_${scheme.name}")
    }

    @Test
    fun dateRangePicker_initialMonthAndSelection() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 1)
                val startSelectionMillis =
                    dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
                val endSelectionMillis =
                    dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 10)
                DateRangePicker(
                    state = rememberDateRangePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis,
                        initialSelectedStartDateMillis = startSelectionMillis,
                        initialSelectedEndDateMillis = endSelectionMillis
                    )
                )
            }
        }
        assertAgainstGolden("dateRangePicker_initialMonthAndSelection_${scheme.name}")
    }

    @Test
    fun dateRangePicker_selectionSpanningMonths() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 1)
                val startSelectionMillis =
                    dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 25)
                val endSelectionMillis =
                    dayInUtcMilliseconds(year = 2021, month = 4, dayOfMonth = 5)
                DateRangePicker(
                    state = rememberDateRangePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis,
                        initialSelectedStartDateMillis = startSelectionMillis,
                        initialSelectedEndDateMillis = endSelectionMillis
                    )
                )
            }
        }
        assertAgainstGolden("dateRangePicker_selectionSpanningMonths_${scheme.name}")
    }

    @Test
    fun dateRangePicker_invalidSundaySelection() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2000, month = 6, dayOfMonth = 1)
                DateRangePicker(
                    state = rememberDateRangePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis
                    ),
                    dateValidator = { utcDateInMills ->
                        val localDate =
                            Instant.ofEpochMilli(utcDateInMills).atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        val dayOfWeek = localDate.dayOfWeek
                        dayOfWeek != DayOfWeek.SUNDAY
                    }
                )
            }
        }
        assertAgainstGolden("dateRangePicker_invalidSundaySelection_${scheme.name}")
    }

    // Returns the given date's day as milliseconds from epoch. The returned value is for the day's
    // start on midnight.
    private fun dayInUtcMilliseconds(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth)
            .atTime(LocalTime.MIDNIGHT)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper("lightTheme", lightColorScheme()),
            ColorSchemeWrapper("darkTheme", darkColorScheme()),
        )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}