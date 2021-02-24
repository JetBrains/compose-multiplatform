/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.benchmark.input

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.benchmark.RandomTextGenerator
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.benchmark.cartesian
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.MoveCursorCommand
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class EditProcessorBenchmark(val initText: InitialText, val scenario: TestScenario) {
    companion object {

        /**
         * Helper class for describing the parameter in test result
         */
        data class InitialText(val text: String, val name: String) {
            override fun toString(): String = name
        }

        private val longText = RandomTextGenerator().nextParagraph(500)
        private val shortText = RandomTextGenerator().nextParagraph(50)

        /**
         * Helper class for describing the parameter in test result
         */
        data class TestScenario(val ops: List<EditCommand>, val name: String) {
            override fun toString(): String = name
        }

        @JvmStatic
        @Parameterized.Parameters(name = "initText={0}, senario={1}")
        fun initParameters(): List<Array<Any>> = cartesian(
            arrayOf(
                InitialText(longText, "Long Text"),
                InitialText(shortText, "Short Text")
            ),
            arrayOf(
                TestScenario(listOf(CommitTextCommand("Android", 1)), "Insert a text"),
                TestScenario(listOf(SetComposingTextCommand("Android", 1)), "Insert composition"),
                TestScenario(listOf(SetComposingRegionCommand(0, 1)), "Set composition"),
                TestScenario(listOf(DeleteSurroundingTextCommand(0, 1)), "Delete text"),
                TestScenario(
                    listOf(DeleteSurroundingTextInCodePointsCommand(0, 1)),
                    "Delete text in code points"
                ),
                TestScenario(listOf(SetSelectionCommand(0, 1)), "Set selection"),
                TestScenario(listOf(BackspaceCommand()), "Backspace"),
                TestScenario(listOf(MoveCursorCommand(1)), "Cursor movement")
            )
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun scenarioTest() {
        benchmarkRule.measureRepeated {
            val ep = runWithTimingDisabled {
                EditProcessor().apply {
                    reset(
                        TextFieldValue(
                            text = initText.text,
                            selection = TextRange(5)
                        ),
                        null // text input service, not used.
                    )
                }
            }

            ep.apply(scenario.ops)
        }
    }
}