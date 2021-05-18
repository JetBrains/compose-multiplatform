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