package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.css.backgroundColor
import androidx.compose.web.css.height
import androidx.compose.web.css.px
import androidx.compose.web.css.width
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Text

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
                    backgroundColor("red")
                }
            }
        ) {}
    }
}