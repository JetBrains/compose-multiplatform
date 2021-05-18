/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.web.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.web.attributes.InputType
import androidx.compose.web.attributes.name
import androidx.compose.web.css.padding
import androidx.compose.web.css.px
import androidx.compose.web.elements.Code
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.H4
import androidx.compose.web.elements.Input
import androidx.compose.web.elements.Pre
import androidx.compose.web.elements.Text
import org.w3c.dom.HTMLElement

@Composable
fun KotlinCodeSnippets() {
    val currentSnippet = remember { mutableStateOf("") }

    Div(
        style = {
            padding(5.px)
        }
    ) {
        H4 {
            Text("Choose code snippet:")
        }
        Input(
            type = InputType.Radio,
            attrs = {
                name("code-snippet")
                onRadioInput {
                    currentSnippet.value = """
                        /* Adds two integers */
                        fun add(i: Int, j: Int): Int {
                            return i + j
                        }
                    """.trimIndent()
                }
            }
        )
        Input(
            type = InputType.Radio,
            attrs = {
                name("code-snippet")
                onRadioInput {
                    currentSnippet.value = """
                        /* Does some calculations */
                        fun calculate(i: Int, j: Int): Int {
                            return i / j + add(i, j)
                        }
                    """.trimIndent()
                }
            }
        )
    }

    CodeSnippet(currentSnippet.value)
}

@Composable
fun CodeSnippet(code: String, language: String = "kotlin") {
    Pre {
        Code(
            attrs = {
                classes(language)
            }
        ) {
            DomSideEffect(code) {
                it.setHighlightedCode(code)
            }
        }
    }
}

private fun HTMLElement.setHighlightedCode(code: String) {
    innerText = code
    HighlightJs.highlightElement(this)
}