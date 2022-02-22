package org.jetbrains.compose.web.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.renderComposableInBody
import org.jetbrains.compose.web.sample.tests.launchTestCase
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.ExperimentalComposeWebStyleApi
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.url.URLSearchParams

class State {
    var isDarkTheme by mutableStateOf(false)
}

val globalState = State()
val globalInt = mutableStateOf(1)

object MyCSSVariables {
    val myVar by variable<CSSColorValue>()
    val myVar2 by variable<StylePropertyString>()
}

object AppStyleSheet : StyleSheet() {
    val bounce by keyframes {
        from {
            property("transform", "translateX(50%)")
        }

        to {
            property("transform", "translateX(-50%)")
        }
    }

    val myClass by style {
        color(Color.green)
        animation(bounce) {
            duration(2.s)
            timingFunction(AnimationTimingFunction.EaseIn)
            direction(AnimationDirection.Alternate)
        }
    }

    val classWithNested by style {
        color(Color.green)

        MyCSSVariables.myVar(Color("blue"))
        MyCSSVariables.myVar2("red")

        hover(self) style {
            color(Color.red)
        }

        border {
            width(1.px)
            style(LineStyle.Solid)
            color(MyCSSVariables.myVar.value())
        }

        media(mediaMaxWidth(640.px)) {
            self style {
                backgroundColor(MyCSSVariables.myVar.value())
                property("color", MyCSSVariables.myVar2.value())
            }
        }
    }
}

object Auto : StyleSheet(AppStyleSheet)

const val MyClassName = "MyClassName"

@Composable
fun CounterApp(counter: MutableState<Int>) {
    Counter(counter.value)

    Button(
        {
            style {
                color(if (counter.value % 2 == 0) Color.green else Color.red)
                width((counter.value + 200).px)
                fontSize(if (counter.value % 2 == 0) 25.px else 30.px)
                margin(15.px)
            }

            onClick { counter.value = counter.value + 1 }
        }
    ) {
        Text("Increment ${counter.value}")
    }
}

@Composable
fun Counter(value: Int) {
    Div(
        attrs = {
            classes("counter")
            id("counter")
            draggable(Draggable.True)
            attr("title", "This is a counter!")
            onDrag { println("DRAGGING NOW!!!!") }

            style {
                color(Color.red)
            }
        }
    ) {
        Text("Counter = $value")
    }
}

@OptIn(ExperimentalComposeWebStyleApi::class)
fun main() {
    val urlParams = URLSearchParams(window.location.search)

    if (urlParams.has("test")) {
        launchTestCase(urlParams.get("test") ?: "")
        return
    }

    renderComposableInBody {
        println("renderComposable")
        val counter = remember { mutableStateOf(0) }

        CheckboxInput(checked = false) {
            onInput {
                println("Checkbox input = ${it.value}")
            }
            onChange {
                println("Checkbox onChange = ${it.value}")
            }
        }

        var emailState by remember { mutableStateOf("") }
        var rangeState by remember { mutableStateOf<Number>(10) }

        TextInput(value = emailState) {
            onInput {
                println("Typed value = ${it.value}")
                emailState = it.value
            }
        }

        NumberInput(value = 10) {
            onBeforeInput { println(("number onBeforeInput = ${it.value}")) }
            onInput { println(("number onInput = ${it.value}")) }
            onChange { println(("number onChange = ${it.value}")) }
        }

        RangeInput(rangeState) {
            onBeforeInput { println(("RangeInput onBeforeInput = ${it.value}")) }
            onInput {
                println(("RangeInput onInput = ${it.value}"))
                rangeState = it.value ?: 0
            }
        }

        MonthInput(value = "2021-10") {
            onInput {
                println("Month = ${it.value}")
            }
        }

        CounterApp(counter)

        val inputValue = remember { mutableStateOf("") }

        smallColoredTextWithState(
            text = derivedStateOf {
                if (inputValue.value.isNotEmpty()) {
                    " ___ " + inputValue.value
                } else {
                    ""
                }
            }
        )

        A(href = "http://127.0.0.1") {
            Text("Click Me")
        }

        MyInputComponent(text = inputValue) {
            inputValue.value = it
        }

        Text("inputValue.value" + inputValue.value)

        Style {
            className(MyClassName) style {
                opacity(0.3)
            }

            className(MyClassName) + hover style {
                opacity(1)
            }

            ".${AppStyleSheet.myClass}:hover" {
                color(Color.red)
            }

            media(mediaMinWidth(500.px) and mediaMaxWidth(700.px)) {
                className(MyClassName) style {
                    fontSize(40.px)
                }
            }
        }
        Style(AppStyleSheet)

        Div(
            attrs = {
                classes(
                    AppStyleSheet.classWithNested
                )
            }
        ) {
            Text("My text")
        }

        Div(
            attrs = {
                classes(MyClassName)
            }
        ) {
            Text("My text")
        }

        Div({
            classes(
                AppStyleSheet.myClass
            )

            style {
                opacity(0.3)
            }
        }) {
            Text("My text")
        }

        Div(
            attrs = {
                style {
                    color(Color.pink)
                    opacity(30.percent)
                }
            }
        ) {
            Text("My text")
        }

        KotlinCodeSnippets()
    }

    MainScope().launch {
        while (true) {
            delay(3000)
            globalState.isDarkTheme = !globalState.isDarkTheme
        }
    }
}

@Composable
fun MyInputComponent(text: State<String>, onChange: (String) -> Unit) {
    Div({
        style {
            padding(50.px)
        }

        onTouchStart {
            println("On touch start")
        }
        onTouchEnd {
            println("On touch end")
        }
    }) {
        Text("Test onMouseDown")
    }

    Div {
        TextArea(
            value = text.value,
            attrs = {
                onKeyDown {
                    println("On keyDown key = : ${it.getNormalizedKey()}")
                }
                onInput {
                    onChange(it.value)
                }
                onKeyUp {
                    println("On keyUp key = : ${it.getNormalizedKey()}")
                }
            }
        )
    }
    Div {
        Input(type = InputType.Checkbox, attrs = {
            onInput {
                println("From div - Checked: " + it.value)
            }
        })
        Input(type = InputType.Text, attrs = {
            value(value = "Hi, ")
        })
    }
    Div {
        Input(
            type = InputType.Radio,
            attrs = {
                onInput {
                    println("Radio 1 - Checked: " + it.value)
                }
                name("f1")
            }
        )
        Input(
            type = InputType.Radio,
            attrs = {
                onInput {
                    println("Radio 2 - Checked: " + it.value)
                }
                name("f1")
            }
        )
        Input(type = InputType.Radio, attrs = {})
    }
}

@Composable
fun smallColoredTextWithState(text: State<String>) {
    smallColoredText(text = text.value)
}

@Composable
fun smallColoredText(text: String) {
    if (globalInt.value < 5) {
        Div(
            attrs = {
                if (globalInt.value > 2) {
                    id("someId-${globalInt.value}")
                }

                classes("someClass")

                attr("customAttr", "customValue")

                onClick {
                    globalInt.value = globalInt.value + 1
                }

                ref { element ->
                    println("DIV CREATED ${element.id}")
                    onDispose { println("DIV REMOVED ${element.id}") }
                }

                style {
                    if (globalState.isDarkTheme) {
                        color(Color.black)
                    } else {
                        color(Color.green)
                    }
                }
            },
        ) {
            Text("Text = $text")
        }
    }
}
