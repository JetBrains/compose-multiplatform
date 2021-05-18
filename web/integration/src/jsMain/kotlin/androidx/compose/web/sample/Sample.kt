package androidx.compose.web.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.attributes.Draggable
import androidx.compose.web.attributes.InputType
import androidx.compose.web.attributes.name
import androidx.compose.web.css.CSSVariables
import androidx.compose.web.css.Color
import androidx.compose.web.css.LineStyle
import androidx.compose.web.css.selectors.className
import androidx.compose.web.css.color
import androidx.compose.web.css.fontSize
import androidx.compose.web.css.margin
import androidx.compose.web.css.opacity
import androidx.compose.web.css.padding
import androidx.compose.web.css.percent
import androidx.compose.web.css.px
import androidx.compose.web.css.selectors.hover
import androidx.compose.web.css.selectors.plus
import androidx.compose.web.css.width
import androidx.compose.web.elements.A
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Input
import androidx.compose.web.elements.Style
import androidx.compose.web.elements.Text
import androidx.compose.web.elements.TextArea
import androidx.compose.web.css.Style
import androidx.compose.web.css.StyleSheet
import androidx.compose.web.css.and
import androidx.compose.web.css.backgroundColor
import androidx.compose.web.css.border
import androidx.compose.web.css.maxWidth
import androidx.compose.web.css.media
import androidx.compose.web.css.minWidth
import androidx.compose.web.css.value
import androidx.compose.web.css.variable
import androidx.compose.web.renderComposableInBody
import androidx.compose.web.sample.tests.launchTestCase
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.url.URLSearchParams

class State {
    var isDarkTheme by mutableStateOf(false)
}

val globalState = State()
val globalInt = mutableStateOf(1)

object MyCSSVariables : CSSVariables {
    val myVar by variable<Color>()
    val myVar2 by variable<String>()
}

object AppStyleSheet : StyleSheet() {
    val myClass by style {
        color("green")
    }

    val classWithNested by style {
        color("green")

        MyCSSVariables.myVar(Color("blue"))
        MyCSSVariables.myVar2("red")

        hover(self) style {
            color("red")
        }

        border {
            width(1.px)
            style(LineStyle.Solid)
            color(MyCSSVariables.myVar.value())
        }

        media(maxWidth(640.px)) {
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
        style = {
            color(if (counter.value % 2 == 0) "green" else "red")
            width((counter.value + 200).px)
            fontSize(if (counter.value % 2 == 0) 25.px else 30.px)
            margin(15.px)
        },
        attrs = {
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
        },
        style = {
            color("red")
        }
    ) {
        Text("Counter = $value")
    }
}

fun main() {
    val urlParams = URLSearchParams(window.location.search)

    if (urlParams.has("test")) {
        launchTestCase(urlParams.get("test") ?: "")
        return
    }

    renderComposableInBody {
        println("renderComposable")
        val counter = remember { mutableStateOf(0) }

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

            className(MyClassName) + hover() style {
                opacity(1)
            }

            ".${AppStyleSheet.myClass}:hover" {
                color("red")
            }

            media(minWidth(500.px) and maxWidth(700.px)) {
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

        Div(
            attrs = {
                classes(
                    AppStyleSheet.myClass
                )
            },
            style = {
                opacity(0.3)
            }
        ) {
            Text("My text")
        }

        Div(
            attrs = {
                classes(
                    Auto.css {
                        color("pink")
                        hover(self) style {
                            color("blue")
                        }
                    }
                )
            },
            style = {
                opacity(30.percent)
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
    Div(
        style = {
            padding(50.px)
        },
        attrs = {
            onTouchStart {
                println("On touch start")
            }
            onTouchEnd {
                println("On touch end")
            }
        }
    ) {
        Text("Test onMouseDown")
    }

    Div {
        TextArea(
            value = text.value,
            attrs = {
                onKeyDown {
                    println("On keyDown key = : ${it.getNormalizedKey()}")
                }
                onTextInput {
                    onChange(it.inputValue)
                    println("On input = : ${it.nativeEvent.isComposing} - ${it.inputValue}")
                }
                onKeyUp {
                    println("On keyUp key = : ${it.getNormalizedKey()}")
                }
            }
        )
    }
    Div(
        attrs = {
            onCheckboxInput {
                println("From div - Checked: " + it.checked)
            }
        }
    ) {
        Input(type = InputType.Checkbox, attrs = {})
        Input(value = "Hi, ")
    }
    Div {
        Input(
            type = InputType.Radio,
            attrs = {
                onRadioInput {
                    println("Radio 1 - Checked: " + it.checked)
                }
                name("f1")
            }
        )
        Input(
            type = InputType.Radio,
            attrs = {
                onRadioInput {
                    println("Radio 2 - Checked: " + it.checked)
                }
                name("f1")
            }
        )
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
            },
            style = {
                if (globalState.isDarkTheme) {
                    color("black")
                } else {
                    color("green")
                }
            }
        ) {
            Text("Text = $text")
        }
    }
}
