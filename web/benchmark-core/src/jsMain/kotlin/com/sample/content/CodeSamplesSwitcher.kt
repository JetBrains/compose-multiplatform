package com.sample.content

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import com.sample.style.AppStylesheet

private object SwitcherVariables : CSSVariables {
    val labelWidth by variable<CSSpxValue>()
    val labelPadding by variable<CSSpxValue>()
}

object SwitcherStylesheet : StyleSheet(AppStylesheet) {

    val boxed by style {

        media(maxWidth(640.px)) {
            self style {
                SwitcherVariables.labelWidth(48.px)
                SwitcherVariables.labelPadding(5.px)
            }
        }

        descendant(self, CSSSelector.Type("label")) style {
            display(DisplayStyle.InlineBlock)
            property("width", SwitcherVariables.labelWidth.value(56.px))
            property("padding", SwitcherVariables.labelPadding.value(10.px))
            property("transition", "all 0.3s")
            property("text-align", "center")
            property("box-sizing", "border-box")

            border {
                style(LineStyle.Solid)
                width(3.px)
                color(Color("transparent"))
                borderRadius(20.px, 20.px, 20.px)
            }
            color("#aaa")
        }

        border {
            style(LineStyle.Solid)
            width(1.px)
            color(Color("#aaa"))
            padding(0.px)
            borderRadius(22.px, 22.px, 22.px)
        }

        descendant(self, selector("input[type=\"radio\"]")) style {
            display(DisplayStyle.None)
        }

        descendant(self, selector("input[type=\"radio\"]:checked + label")) style {
            border {
                style(LineStyle.Solid)
                width(3.px)
                color(Color("#167dff"))
                borderRadius(20.px, 20.px, 20.px)
            }
            color("#000")
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
                if (current == ix) checked()
                onRadioInput { onSelect(ix) }
            })
            Label(forId = "snippet$ix") { Text("${ix + 1}") }
        }
    }
}
