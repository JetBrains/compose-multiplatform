package com.sample.content

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import com.sample.style.AppStylesheet

private object SwitcherVariables {
    val labelWidth by variable<CSSpxValue>()
    val labelPadding by variable<CSSpxValue>()
}

object SwitcherStylesheet : StyleSheet(AppStylesheet) {
    val boxed by style {

        media(mediaMaxWidth(640.px)) {
            self style {
                SwitcherVariables.labelWidth(48.px)
                SwitcherVariables.labelPadding(5.px)
            }
        }

        desc(self, type("label")) style {
            display(DisplayStyle.InlineBlock)
            width(SwitcherVariables.labelWidth.value(56.px))
            padding(SwitcherVariables.labelPadding.value(10.px))
            property("transition", "all 0.3s")
            textAlign("center")
            boxSizing("border-box")

            border {
                style(LineStyle.Solid)
                width(3.px)
                color(Color("transparent"))
                borderRadius(20.px, 20.px, 20.px)
            }
            color(Color("#aaa"))
        }

        border {
            style(LineStyle.Solid)
            width(1.px)
            color(Color("#aaa"))
            padding(0.px)
            borderRadius(22.px, 22.px, 22.px)
        }

        type("input") + attrEquals(name = "type", value = "radio") style {
            display(DisplayStyle.None)
        }

        adjacent(
            sibling = type("input") + attrEquals(name = "type", value = "radio") + checked,
            selected = type("label")
        ) style {
            border {
                style(LineStyle.Solid)
                width(3.px)
                color(Color("#167dff"))
                borderRadius(20.px, 20.px, 20.px)
            }
            color(Color("#000"))
        }
    }
}

@Composable
fun CodeSampleSwitcher(count: Int, current: Int, onSelect: (Int) -> Unit) {
    Form(attrs = {
        classes(SwitcherStylesheet.boxed)
    }) {
        repeat(count) { ix ->
            Input(type = InputType.Radio, attrs = {
                name("code-snippet")
                value("snippet$ix")
                id("snippet$ix")
                if (current == ix) checked(true)
                onChange { onSelect(ix) }
            })
            Label(forId = "snippet$ix") { Text("${ix + 1}") }
        }
    }
}
