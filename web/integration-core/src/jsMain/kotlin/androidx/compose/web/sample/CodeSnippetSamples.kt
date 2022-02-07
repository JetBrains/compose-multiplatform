package org.jetbrains.compose.web.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

@Composable
fun KotlinCodeSnippets() {
    val currentSnippet = remember { mutableStateOf("") }

    Div(
        {
            style {
                padding(5.px)
            }
        }
    ) {
        H4 {
            Text("Choose code snippet:")
        }
        Input(
            type = InputType.Radio,
            attrs = {
                name("code-snippet")
                onInput {
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
                onInput {
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
            DisposableEffect(code) {
                scopeElement.setHighlightedCode(code)
                onDispose {  }
            }
        }
    }
}

private fun HTMLElement.setHighlightedCode(code: String) {
    innerText = code
    HighlightJs.highlightElement(this)
}
