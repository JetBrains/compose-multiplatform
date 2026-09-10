/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeWebApi::class)
class CommonInputElementsTest {
    @Test
    fun rendersEveryInputHelperWithExpectedSerializableState() {
        val html = composeHtmlToString {
            CheckboxInput(checked = true)
            DateInput(value = "2026-08-18")
            DateTimeLocalInput(value = "2026-08-18T12:30")
            EmailInput(value = "user@example.com")
            FileInput(value = "ignored.txt")
            HiddenInput {
                value("token")
            }
            MonthInput(value = "2026-08")
            NumberInput(value = 10, min = 1, max = 20)
            PasswordInput(value = "secret")
            RadioInput(checked = true) {
                value("choice")
            }
            RangeInput(value = 5, min = 0, max = 10, step = 2)
            SearchInput(value = "query")
            SubmitInput {
                value("Send")
            }
            TelInput(value = "+41 12 345 67 89")
            TextInput(value = "text")
            TimeInput(value = "12:30")
            UrlInput(value = "https://example.com")
            WeekInput(value = "2026-W34")
        }

        assertEquals(
            "<input type=\"checkbox\">" +
                "<input type=\"date\">" +
                "<input type=\"datetime-local\">" +
                "<input type=\"email\">" +
                "<input type=\"file\">" +
                "<input type=\"hidden\" value=\"token\">" +
                "<input type=\"month\">" +
                "<input type=\"number\" min=\"1\" max=\"20\">" +
                "<input type=\"password\">" +
                "<input type=\"radio\" value=\"choice\">" +
                "<input type=\"range\" min=\"0\" max=\"10\" step=\"2\">" +
                "<input type=\"search\">" +
                "<input type=\"submit\" value=\"Send\">" +
                "<input type=\"tel\">" +
                "<input type=\"text\">" +
                "<input type=\"time\">" +
                "<input type=\"url\">" +
                "<input type=\"week\">",
            html,
        )
    }

    @Test
    fun rendersStringRadioGroup() {
        val html = composeHtmlToString {
            RadioGroup(checkedValue = "second", name = "string-options") {
                RadioInput(value = "first", id = "first-option")
                RadioInput(value = "second", id = "second-option")
            }
        }

        assertEquals(
            "<input type=\"radio\" id=\"first-option\" name=\"string-options\" value=\"first\">" +
                "<input type=\"radio\" id=\"second-option\" name=\"string-options\" value=\"second\">",
            html,
        )
    }

    @Test
    fun rendersEnumRadioGroup() {
        val html = composeHtmlToString {
            RadioGroup(checkedValue = RadioOption.Second, name = "enum-options") {
                RadioInput(value = RadioOption.First)
                RadioInput(value = RadioOption.Second)
            }
        }

        assertEquals(
            "<input type=\"radio\" name=\"enum-options\" value=\"First\">" +
                "<input type=\"radio\" name=\"enum-options\" value=\"Second\">",
            html,
        )
    }

    private enum class RadioOption {
        First,
        Second,
    }
}
