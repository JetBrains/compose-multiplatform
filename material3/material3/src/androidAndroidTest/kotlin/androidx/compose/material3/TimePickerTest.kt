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

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.SelectableGroup
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.dx.mockito.inline.extended.MockedMethod
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.quality.Strictness

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TimePickerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun timePicker_initialState() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) {
            TimePicker(state)
        }

        rule.onAllNodesWithText("23").assertCountEquals(1)

        rule.onNodeWithText("02").assertIsSelected()

        rule.onNodeWithText("AM").assertExists()

        rule.onNodeWithText("PM").assertExists().assertIsSelected()
    }

    @Test
    fun timePicker_switchToMinutes() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) {
            TimePicker(state)
        }

        rule.onNodeWithText("23").performClick()

        rule.onNodeWithText("55").assertExists()
    }

    @Test
    fun timePicker_selectHour() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) {
            TimePicker(state)
        }

        rule.onNodeWithText("6").performClick()

        // shows 06 in display
        rule.onNodeWithText("06").assertExists()

        // switches to minutes
        rule.onNodeWithText("23").assertIsSelected()

        // state updated
        assertThat(state.hour).isEqualTo(18)
    }

    @Test
    fun timePicker_switchToAM() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) {
            TimePicker(state)
        }

        assertThat(state.hour).isEqualTo(14)

        rule.onNodeWithText("AM").performClick()

        assertThat(state.hour).isEqualTo(2)
    }

    @Test
    fun timePicker_dragging() {
        val state = TimePickerState(initialHour = 0, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) {
            TimePicker(state)
        }

        rule.onAllNodes(keyIsDefined(SelectableGroup), useUnmergedTree = true)
            .onLast()
            .performTouchInput {
                down(topCenter)
                // 3 O'Clock
                moveTo(centerRight)
                up()
            }

        rule.runOnIdle {
            assertThat(state.hour).isEqualTo(3)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun timePickerState_format_12h() {
        lateinit var context: Context
        lateinit var state: TimePickerState
        val session = mockitoSession()
            .spyStatic(DateFormat::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        try {
            rule.setMaterialContent(lightColorScheme()) {
                context = LocalContext.current
                doReturn(false).`when`(object : MockedMethod<Boolean> {
                    override fun get(): Boolean {
                        return DateFormat.is24HourFormat(context)
                    }
                })

                state = rememberTimePickerState()
            }
        } finally {
            session.finishMocking()
        }

        assertThat(state.is24hour).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun timePickerState_format_24h() {
        lateinit var context: Context
        lateinit var state: TimePickerState
        val session = mockitoSession()
            .spyStatic(DateFormat::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        try {
            rule.setMaterialContent(lightColorScheme()) {
                context = LocalContext.current
                doReturn(true).`when`(object : MockedMethod<Boolean> {
                    override fun get(): Boolean {
                        return DateFormat.is24HourFormat(context)
                    }
                })

                state = rememberTimePickerState()
            }
        } finally {
            session.finishMocking()
        }

        assertThat(state.is24hour).isTrue()
    }

    @Test
    fun timePicker_toggle_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var contentDescription: String
        rule.setMaterialContent(lightColorScheme()) {
            contentDescription = getString(Strings.TimePickerPeriodToggle)
            TimePicker(state)
        }

        rule.onNodeWithContentDescription(contentDescription)
            .onChildren()
            .assertAll(isSelectable())
    }

    @Test
    fun timePicker_display_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var minuteDescription: String
        lateinit var hourDescription: String
        rule.setMaterialContent(lightColorScheme()) {
            minuteDescription = getString(Strings.TimePickerMinuteSelection)
            hourDescription = getString(Strings.TimePickerHourSelection)
            TimePicker(state)
        }

        rule.onNodeWithContentDescription(minuteDescription)
            .assertIsSelectable()
            .assertIsNotSelected()
            .assert(expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertHasClickAction()

        rule.onNodeWithContentDescription(hourDescription)
            .assertIsSelectable()
            .assertIsSelected()
            .assert(expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertHasClickAction()
    }

    @Test
    fun timePicker_clockFace_hour_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var hourDescription: String

        rule.setMaterialContent(lightColorScheme()) {
            hourDescription = getString(Strings.TimePickerHourSuffix, 2)
            TimePicker(state)
        }

        rule.onAllNodesWithContentDescription(hourDescription)
            .onLast()
            .onSiblings()
            .filter(isFocusable())
            .assertCountEquals(11)
            .assertAll(
                hasContentDescription(
                    value = "o'clock",
                    substring = true,
                    ignoreCase = true
                )
            )
    }

    @Test
    fun timePicker_clockFace_selected_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) {
            TimePicker(state)
        }

        rule.onAllNodesWithText("14")
            .filter(isFocusable())
            .assertAll(isSelected())
    }

    @Test
    fun timePicker_clockFace_minutes_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var minuteDescription: String

        rule.setMaterialContent(lightColorScheme()) {
            minuteDescription = getString(Strings.TimePickerMinuteSuffix, 55)
            TimePicker(state)
        }

        // Switch to minutes
        rule.onNodeWithText("23").performClick()

        rule.waitForIdle()

        rule.onNodeWithContentDescription(minuteDescription)
            .assertExists()
            .onSiblings()
            .assertCountEquals(11)
            .assertAll(
                hasContentDescription(
                    value = "minutes",
                    substring = true,
                    ignoreCase = true
                )
            )
    }

    @Test
    fun timeInput_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("14")
            .assert(isFocusable())
            .assert(hasImeAction(ImeAction.Next))
            .assert(isFocused())

        rule.onAllNodesWithText("23")
            .filterToOne(isSelectable())
            .assert(isNotSelected())
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_valid() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("10")
            .performKeyInput {
                pressKey(Key.Zero)
                pressKey(Key.Four)
            }

        rule.waitForIdle()

        // Switched to minutes text field
        rule.onNodeWithText("23")
            .performKeyInput {
                pressKey(Key.Five)
                pressKey(Key.Two)
            }

        assertThat(state.minute).isEqualTo(52)
        assertThat(state.hour).isEqualTo(4)
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_outOfRange() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("10")
            .performKeyInput {
                pressKey(Key.Four)
                pressKey(Key.Four)
            }

        // only the first 4 is accepted
        assertThat(state.hour).isEqualTo(4)
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_Nan() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("10")
            .performKeyInput {
                pressKey(Key.A)
                pressKey(Key.B)
                pressKey(Key.C)
                pressKey(Key.NumPadDot)
                pressKey(Key.Comma)
                pressKey(Key.NumPadComma)
            }

        // Value didn't change
        assertThat(state.hour).isEqualTo(10)
    }

    @Test
    fun timeInput_keyboardInput_switchAmPm() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("PM")
            .performClick()

        // Value didn't change
        assertThat(state.hour).isEqualTo(22)
    }

    @Test
    fun timeInput_24Hour_noAmPm_Toggle() {
        val state = TimePickerState(initialHour = 22, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("PM").assertDoesNotExist()

        rule.onNodeWithText("AM").assertDoesNotExist()
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun timeInput_24Hour_writeAfternoonHour() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) {
            TimeInput(state)
        }

        rule.onNodeWithText("10")
            .performKeyInput {
                pressKey(Key.Two)
                pressKey(Key.Two)
            }

        assertThat(state.hour).isEqualTo(22)
    }

    @Test
    fun state_restoresTimePickerState() {
        val restorationTester = StateRestorationTester(rule)
        var state: TimePickerState?
        restorationTester.setContent {
            state = rememberTimePickerState(initialHour = 14, initialMinute = 54, is24Hour = true)
        }

        state = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state?.hour).isEqualTo(14)
            assertThat(state?.minute).isEqualTo(54)
            assertThat(state?.is24hour).isTrue()
        }
    }

    @Test
    fun clockFace_24Hour_everyValue() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(state, TimePickerDefaults.colors())
        }

        repeat(24) { number ->
            rule.onNodeWithText(number.toString()).performClick()
            rule.runOnIdle {
                state.selection = Selection.Hour
                assertThat(state.hour).isEqualTo(number)
            }
        }
    }

    @Test
    fun clockFace_12Hour_everyValue() {
        val state = TimePickerState(initialHour = 0, initialMinute = 0, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(state, TimePickerDefaults.colors())
        }

        repeat(24) { number ->
            if (number >= 12) {
                state.isAfternoonToggle = true
            }

            val hour = when {
                number == 0 -> 12
                number > 12 -> number - 12
                else -> number
            }

            rule.onNodeWithText("$hour").performClick()
            rule.runOnIdle {
                state.selection = Selection.Hour
                assertThat(state.hour).isEqualTo(number)
            }
        }
    }
}
