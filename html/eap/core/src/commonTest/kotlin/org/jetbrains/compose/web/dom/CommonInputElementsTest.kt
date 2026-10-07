/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals

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
            "<input type=\"checkbox\" checked>" +
                "<input type=\"date\" value=\"2026-08-18\">" +
                "<input type=\"datetime-local\" value=\"2026-08-18T12:30\">" +
                "<input type=\"email\" value=\"user@example.com\">" +
                "<input type=\"file\" value=\"ignored.txt\">" +
                "<input type=\"hidden\" value=\"token\">" +
                "<input type=\"month\" value=\"2026-08\">" +
                "<input type=\"number\" min=\"1\" max=\"20\" value=\"10\">" +
                "<input type=\"password\" value=\"secret\">" +
                "<input type=\"radio\" value=\"choice\" checked>" +
                "<input type=\"range\" min=\"0\" max=\"10\" step=\"2\" value=\"5\">" +
                "<input type=\"search\" value=\"query\">" +
                "<input type=\"submit\" value=\"Send\">" +
                "<input type=\"tel\" value=\"+41 12 345 67 89\">" +
                "<input type=\"text\" value=\"text\">" +
                "<input type=\"time\" value=\"12:30\">" +
                "<input type=\"url\" value=\"https://example.com\">" +
                "<input type=\"week\" value=\"2026-W34\">",
            html,
        )
    }
}
