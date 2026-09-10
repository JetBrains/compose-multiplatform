package org.jetbrains.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.CSSSelector
import org.jetbrains.compose.web.dom.*

class TestCases1 {
    val helloWorldText by testCase {
        Div { Text("Hello World!") }
    }

    val buttonClicksUpdateCounterValue by testCase {
        var count by remember { mutableStateOf(0) }

        TestText(value = count.toString())

        Button(
            attrs = {
                id("btn")
                onClick { count += 1 }
            }
        ) {
            Text("Button")
        }
    }

    val hoverOnDivUpdatesText by testCase {
        var hovered by remember { mutableStateOf(false) }

        TestText(
            value = if (hovered) "hovered" else "not hovered"
        )

        Div(
            {
                id("box")
                onMouseEnter {
                    println("Mouse enter")
                    hovered = true
                }
                onMouseLeave {
                    println("Mouse leave")
                    hovered = false
                }

                style {
                    width(100.px)
                    height(100.px)
                    backgroundColor(Color.red)
                }
            }
        ) {}
    }

    val smallWidthChangesTheTextColor by testCase {
        Style(AppStyleSheet)
        Span(attrs = {
            id("span1")
            classes(AppStyleSheet.textClass)
        }) {
            Text("This a colored text")
            Span { Text(" [expanded]") }
        }
    }
}

private object AppStyleSheet : StyleSheet() {
    val textClass by style {
        color(rgba(0, 200, 0, 0.92))
        media(mediaMaxWidth(600.px)) {
            self style {
                color(rgba(255, 200, 0, 0.99))
            }

            child(self, type("span")) style {
                display(DisplayStyle.None)
            }
        }
    }
}
