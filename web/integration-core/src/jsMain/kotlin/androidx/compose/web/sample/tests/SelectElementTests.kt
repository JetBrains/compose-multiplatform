package androidx.compose.web.sample.tests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.sample.tests.TestText
import org.jetbrains.compose.web.sample.tests.testCase
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.asList

class SelectElementTests {

    @Composable
    private fun ChooseAPetSelect(
        canSelectMultiple: Boolean = false,
        onInput: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit,
        onChange: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        Label(forId = "pet-select") {
            Text("Choose a pet:")
        }

        Select(multiple = canSelectMultiple, attrs = {
            name("pets")
            id("pet-select")

            onInput { onInput(it) }
            onChange { onChange(it) }

        }) {
            Option(value = "") { Text("--Please choose an option--") }
            Option(value = "dog") { Text("Dog") }
            Option(value = "cat") { Text("Cat") }
            Option(value = "hamster") { Text("Hamster") }
            Option(value = "parrot") { Text("Parrot") }
            Option(value = "spider") { Text("Spider") }
            Option(value = "goldfish") { Text("Goldfish") }
        }
    }

    val selectDispatchesInputAndChangeAndBeforeInputEvents by testCase {
        var onInputState by remember { mutableStateOf("None") }
        var onChangeState by remember { mutableStateOf("None") }

        P { TestText(value = onInputState, id = "txt_oninput") }
        P { TestText(value = onChangeState, id = "txt_onchange") }

        ChooseAPetSelect(
            onInput = { onInputState = it.value ?: "" },
            onChange = { onChangeState = it.value ?: "" }
        )
    }

    val selectMultipleItems by testCase {
        var selectedItemsText by remember { mutableStateOf("None") }

        P { TestText(value = selectedItemsText) }

        ChooseAPetSelect(
            canSelectMultiple = true,
            onInput = {},
            onChange = {
                selectedItemsText = it.target.selectedOptions.asList().joinToString(separator = ", ") {
                    it.asDynamic().value
                }
            }
        )
    }
}
