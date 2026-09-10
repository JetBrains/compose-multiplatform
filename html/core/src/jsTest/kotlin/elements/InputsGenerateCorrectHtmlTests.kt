package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*

class InputsGenerateCorrectHtmlTests {

    @Test
    fun checkBoxInput() = runTest {
        composition {
            CheckboxInput(checked = true) {
                id("checkboxId")
            }
        }

        val checkboxInput = root.firstChild as HTMLInputElement

        assertEquals("checkbox", checkboxInput.getAttribute("type"))
        assertEquals("checkboxId", checkboxInput.getAttribute("id"))
        assertEquals(true, checkboxInput.checked)
    }

    @Test
    fun checkBoxInputWithDefaults() = runTest {
        composition { CheckboxInput() }

        val checkboxInput = root.firstChild as HTMLInputElement

        assertEquals("checkbox", checkboxInput.getAttribute("type"))
        assertEquals(null, checkboxInput.getAttribute("id"))
        assertEquals(false, checkboxInput.checked)
    }

    @Test
    fun dateInput() = runTest {
        composition {
            DateInput(value = "2021-10-10") {
                id("dateInputId")
            }
        }

        val dateInput = root.firstChild as HTMLInputElement

        assertEquals("date", dateInput.getAttribute("type"))
        assertEquals("dateInputId", dateInput.getAttribute("id"))
        assertEquals("2021-10-10", dateInput.value)
    }

    @Test
    fun dateInputWithDefaults() = runTest {
        composition { DateInput() }

        val dateInput = root.firstChild as HTMLInputElement

        assertEquals("date", dateInput.getAttribute("type"))
        assertEquals(null, dateInput.getAttribute("id"))
        assertEquals("", dateInput.value)
    }

    @Test
    fun emailInput() = runTest {
        composition {
            EmailInput(value = "user@mail.com") {
                id("emailInputId")
            }
        }

        val emailInput = root.firstChild as HTMLInputElement

        assertEquals("email", emailInput.getAttribute("type"))
        assertEquals("emailInputId", emailInput.getAttribute("id"))
        assertEquals("user@mail.com", emailInput.value)
    }

    @Test
    fun emailInputWithDefaults() = runTest {
        composition { EmailInput() }

        val emailInput = root.firstChild as HTMLInputElement

        assertEquals("email", emailInput.getAttribute("type"))
        assertEquals(null, emailInput.getAttribute("id"))
        assertEquals("", emailInput.value)
    }

    @Test
    fun monthInput() = runTest {
        composition {
            MonthInput(value = "2017-06") {
                id("monthInputId")
            }
        }

        val monthInput = root.firstChild as HTMLInputElement

        assertEquals("month", monthInput.getAttribute("type"))
        assertEquals("monthInputId", monthInput.getAttribute("id"))
        assertEquals("2017-06", monthInput.value)
    }

    @Test
    fun monthInputWithDefaults() = runTest {
        composition { MonthInput() }

        val monthInput = root.firstChild as HTMLInputElement

        assertEquals("month", monthInput.getAttribute("type"))
        assertEquals(null, monthInput.getAttribute("id"))
        assertEquals("", monthInput.value)
    }

    @Test
    fun numberInput() = runTest {
        composition {
            NumberInput(value = 100, min = 10, max = 200) {
                id("numberInputId")
            }
        }

        val numberInput = root.firstChild as HTMLInputElement

        assertEquals("number", numberInput.getAttribute("type"))
        assertEquals("numberInputId", numberInput.getAttribute("id"))
        assertEquals("200", numberInput.getAttribute("max"))
        assertEquals("10", numberInput.getAttribute("min"))
        assertEquals(100, numberInput.valueAsNumber.toInt())
    }

    @Test
    fun numberInputWithDefaults() = runTest {
        composition {
            NumberInput {
                id("numberInputId")
            }
        }

        val numberInput = root.firstChild as HTMLInputElement

        assertEquals("number", numberInput.getAttribute("type"))
        assertEquals("numberInputId", numberInput.getAttribute("id"))
        assertEquals(null, numberInput.getAttribute("max"))
        assertEquals(null, numberInput.getAttribute("min"))
        assertEquals("", numberInput.value)
    }


    @Test
    fun passwordInput() = runTest {
        composition {
            PasswordInput(value = "somepassword") {
                id("passwordInputId")
            }
        }

        val passwordInput = root.firstChild as HTMLInputElement

        assertEquals("password", passwordInput.getAttribute("type"))
        assertEquals("passwordInputId", passwordInput.getAttribute("id"))
        assertEquals("somepassword", passwordInput.value)
    }

    @Test
    fun passwordInputWithDefaults() = runTest {
        composition { PasswordInput() }

        val passwordInput = root.firstChild as HTMLInputElement

        assertEquals("password", passwordInput.getAttribute("type"))
        assertEquals(null, passwordInput.getAttribute("id"))
        assertEquals("", passwordInput.value)
    }

    @Test
    fun radioInput() = runTest {
        composition {
            RadioInput(checked = true) {
                id("radioInputId")
            }
        }

        val radioInput = root.firstChild as HTMLInputElement

        assertEquals("radio", radioInput.getAttribute("type"))
        assertEquals("radioInputId", radioInput.getAttribute("id"))
        assertEquals(true, radioInput.checked)
    }

    @Test
    fun radioInputWithDefaults() = runTest {
        composition { RadioInput() }

        val radioInput = root.firstChild as HTMLInputElement

        assertEquals("radio", radioInput.getAttribute("type"))
        assertEquals(null, radioInput.getAttribute("id"))
        assertEquals(false, radioInput.checked)
    }

    @Test
    fun rangeInput() = runTest {
        composition {
            RangeInput(value = 20, min = 10, max = 30, step = 2) {
                id("rangeInputId")
            }
        }

        val rangeInput = root.firstChild as HTMLInputElement

        assertEquals("range", rangeInput.getAttribute("type"))
        assertEquals("rangeInputId", rangeInput.getAttribute("id"))
        assertEquals("10", rangeInput.getAttribute("min"))
        assertEquals("30", rangeInput.getAttribute("max"))
        assertEquals("2", rangeInput.getAttribute("step"))
        assertEquals(20, rangeInput.valueAsNumber.toInt())
    }

    @Test
    fun rangeInputWithDefaults() = runTest {
        composition { RangeInput() }

        val rangeInput = root.firstChild as HTMLInputElement

        assertEquals("range", rangeInput.getAttribute("type"))
        assertEquals(null, rangeInput.getAttribute("id"))
        assertEquals(null, rangeInput.getAttribute("min"))
        assertEquals(null, rangeInput.getAttribute("max"))
        assertEquals("1", rangeInput.getAttribute("step"))
    }

    @Test
    fun searchInput() = runTest {
        composition {
            SearchInput(value = "Search Term") {
                id("searchInputId")
            }
        }

        val searchInput = root.firstChild as HTMLInputElement

        assertEquals("search", searchInput.getAttribute("type"))
        assertEquals("searchInputId", searchInput.getAttribute("id"))
        assertEquals("Search Term", searchInput.value)
    }

    @Test
    fun searchInputWithDefaults() = runTest {
        composition { SearchInput() }

        val searchInput = root.firstChild as HTMLInputElement

        assertEquals("search", searchInput.getAttribute("type"))
        assertEquals(null, searchInput.getAttribute("id"))
        assertEquals("", searchInput.value)
    }

    @Test
    fun telInput() = runTest {
        composition {
            TelInput(value = "0123456789") {
                id("telInputId")
            }
        }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("tel", textInput.getAttribute("type"))
        assertEquals("telInputId", textInput.getAttribute("id"))
        assertEquals("0123456789", textInput.value)
    }

    @Test
    fun telInputWithDefaults() = runTest {
        composition { TelInput() }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("tel", textInput.getAttribute("type"))
        assertEquals(null, textInput.getAttribute("id"))
        assertEquals("", textInput.value)
    }

    @Test
    fun textInput() = runTest {
        composition {
            TextInput(value = "Some value") {
                id("textInputId")
            }
        }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("text", textInput.getAttribute("type"))
        assertEquals("textInputId", textInput.getAttribute("id"))
        assertEquals("Some value", textInput.value)
    }

    @Test
    fun textInputWithDefaults() = runTest {
        composition { TextInput() }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("text", textInput.getAttribute("type"))
        assertEquals(null, textInput.getAttribute("id"))
        assertEquals("", textInput.value)
    }

    @Test
    fun timeInput() = runTest {
        composition {
            TimeInput(value = "12:20") {
                id("timeInputId")
            }
        }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("time", textInput.getAttribute("type"))
        assertEquals("timeInputId", textInput.getAttribute("id"))
        assertEquals("12:20", textInput.value)
    }

    @Test
    fun timeInputWithDefaults() = runTest {
        composition { TimeInput() }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("time", textInput.getAttribute("type"))
        assertEquals(null, textInput.getAttribute("id"))
        assertEquals("", textInput.value)
    }

    @Test
    fun urlInput() = runTest {
        composition {
            UrlInput(value = "http://127.0.0.1") {
                id("urlInputId")
            }
        }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("url", textInput.getAttribute("type"))
        assertEquals("urlInputId", textInput.getAttribute("id"))
        assertEquals("http://127.0.0.1", textInput.value)
    }

    @Test
    fun urlInputWithDefaults() = runTest {
        composition { UrlInput() }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("url", textInput.getAttribute("type"))
        assertEquals(null, textInput.getAttribute("id"))
        assertEquals("", textInput.value)
    }

    @Test
    fun weekInput() = runTest {
        composition {
            WeekInput(value = "2017-W01") {
                id("weekInputId")
            }
        }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("week", textInput.getAttribute("type"))
        assertEquals("weekInputId", textInput.getAttribute("id"))
        assertEquals("2017-W01", textInput.value)
    }

    @Test
    fun weekInputWithDefaults() = runTest {
        composition { WeekInput() }

        val textInput = root.firstChild as HTMLInputElement

        assertEquals("week", textInput.getAttribute("type"))
        assertEquals(null, textInput.getAttribute("id"))
        assertEquals("", textInput.value)
    }

    @Test
    fun textInputWithAutoComplete() = runTest {
        composition {
            TextInput {
                autoComplete(AutoComplete.name)
            }
        }
        assertEquals("""<input type="text" autocomplete="name">""", root.innerHTML)
    }

    @Test
    fun textAreaWithAutoComplete() = runTest {
        composition {
            TextArea(attrs = {
                autoComplete(AutoComplete.email)
            })
        }
        assertEquals("""<textarea autocomplete="email"></textarea>""", root.innerHTML)
    }

    @Test
    fun formWithAutoComplete() = runTest {
        var autoCompleteEnabled by mutableStateOf(true)

        composition {
            Form(attrs = {
                autoComplete(autoCompleteEnabled)
            })
        }

        assertEquals("""<form autocomplete="on"></form>""", root.innerHTML)

        autoCompleteEnabled = false
        waitForChanges()

        assertEquals("""<form autocomplete="off"></form>""", root.innerHTML)
    }

    @Test
    fun selectWithAutoComplete() = runTest {
        composition {
            Select({
                autoComplete(AutoComplete.tel)
            })
        }
        assertEquals("""<select autocomplete="tel"></select>""", root.innerHTML)
    }

    @Test
    fun textInputChangesItsValueFromTextToEmpty() = runTest {
        var state by mutableStateOf("text")

        composition {
            TextInput(value = state)
        }

        assertEquals("text", (root.firstChild as HTMLInputElement).value)

        state = ""
        waitForRecompositionComplete()

        assertEquals("", (root.firstChild as HTMLInputElement).value)
    }

    @Test
    fun textInputChangesItsValueFromEmptyToText() = runTest {
        var state by mutableStateOf("")

        composition {
            TextInput(value = state)
        }

        assertEquals("", (root.firstChild as HTMLInputElement).value)

        state = "text"
        waitForRecompositionComplete()

        assertEquals("text", (root.firstChild as HTMLInputElement).value)
    }

    @Test
    fun textAreaChangesItsValueFromTextToEmpty() = runTest {
        var state by mutableStateOf("text")

        composition {
            TextArea(value = state)
        }

        assertEquals("text", (root.firstChild as HTMLTextAreaElement).value)

        state = ""
        waitForRecompositionComplete()

        assertEquals("", (root.firstChild as HTMLTextAreaElement).value)
    }

    @Test
    fun textAreaChangesItsValueFromEmptyToText() = runTest {
        var state by mutableStateOf("")

        composition {
            TextArea(value = state)
        }

        assertEquals("", (root.firstChild as HTMLTextAreaElement).value)

        state = "text"
        waitForRecompositionComplete()

        assertEquals("text", (root.firstChild as HTMLTextAreaElement).value)
    }

    @Test
    fun textAreaWithDefaultValueAndWithoutIt() = runTest {
        composition {
            TextArea()
            TextArea {
                defaultValue("not-empty-default-value")
            }
        }

        assertEquals("<textarea></textarea><textarea>not-empty-default-value</textarea>", root.innerHTML)
    }
}
