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

package androidx.compose.ui.text.input

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.filters.LargeTest
import androidx.ui.integration.test.RandomTextGenerator
import androidx.ui.integration.test.cartesian
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(InternalTextApi::class)
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
        data class TestScenario(val ops: List<EditOperation>, val name: String) {
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
                TestScenario(listOf(CommitTextEditOp("Android", 1)), "Insert a text"),
                TestScenario(listOf(SetComposingTextEditOp("Android", 1)), "Insert composition"),
                TestScenario(listOf(SetComposingRegionEditOp(0, 1)), "Set composition"),
                TestScenario(listOf(DeleteSurroundingTextEditOp(0, 1)), "Delete text"),
                TestScenario(
                    listOf(DeleteSurroundingTextInCodePointsEditOp(0, 1)),
                    "Delete text in code points"
                ),
                TestScenario(listOf(SetSelectionEditOp(0, 1)), "Set selection"),
                TestScenario(listOf(BackspaceKeyEditOp()), "Backspace"),
                TestScenario(listOf(MoveCursorEditOp(1)), "Cursor movement")
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
                    onNewState(
                        TextFieldValue(
                            text = initText.text,
                            selection = TextRange(5)
                        ),
                        null, // text input service, not used.
                        0 // session token, not used
                    )
                }
            }

            ep.onEditCommands(scenario.ops)
        }
    }
}