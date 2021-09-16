package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.RadioGroup
import org.jetbrains.compose.web.dom.RadioInput
import org.jetbrains.compose.web.sample.tests.TestText
import org.jetbrains.compose.web.sample.tests.testCase

class RadioGroupTestCases {

    @OptIn(ExperimentalComposeWebApi::class)
    val radioGroupItemsCanBeChecked by testCase {
        var checked by remember { mutableStateOf("None") }

        TestText(value = checked)

        RadioGroup(
            checkedValue = checked
        ) {
           RadioInput(value = "r1", id = "id1") {
               onInput { checked = "r1" }
           }
           RadioInput(value = "r2", id = "id2") {
               onInput { checked = "r2" }
           }
           RadioInput(value = "r3", id = "id3") {
               onInput { checked = "r3" }
           }
        }
    }

    @OptIn(ExperimentalComposeWebApi::class)
    val twoRadioGroupsChangedIndependently by testCase {
        var checked1 by remember { mutableStateOf("None") }
        var checked2 by remember { mutableStateOf("None") }

        Div {
            TestText(value = checked1)
        }
        Div {
            TestText(value = checked2, id = "txt2")
        }

        RadioGroup(
            checkedValue = checked1
        ) {
            RadioInput(value = "r1", id = "id1") {
                onInput { checked1 = "r1" }
            }
            RadioInput(value = "r2", id = "id2") {
                onInput { checked1 = "r2" }
            }
            RadioInput(value = "r3", id = "id3") {
                onInput { checked1 = "r3" }
            }
        }

        RadioGroup(
            checkedValue = checked2
        ) {
            RadioInput(value = "ra", id = "ida") {
                onInput { checked2 = "ra" }
            }
            RadioInput(value = "rb", id = "idb") {
                onInput { checked2 = "rb" }
            }
            RadioInput(value = "rc", id = "idc") {
                onInput { checked2 = "rc" }
            }
        }
    }
}
