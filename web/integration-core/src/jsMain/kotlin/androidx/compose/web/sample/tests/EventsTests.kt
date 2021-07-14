package org.jetbrains.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

class EventsTests {

    val doubleClickUpdatesText by testCase {
        var state by remember { mutableStateOf("") }

        TestText(state)

        Div(
            attrs = {
                id("box")
                style {
                    width(100.px)
                    height(100.px)
                    backgroundColor("red")
                }
                onDoubleClick { state = "Double Click Works!" }
            }
        ) {
            Text("Clickable Box")
        }
    }

    val focusInAndFocusOutUpdateTheText by testCase {
        var state by remember { mutableStateOf("") }

        TestText(state)

        Input(type = InputType.Text, attrs = {
            id("focusableInput")
            onFocusIn {
                state = "focused"
            }
            onFocusOut {
                state = "not focused"
            }
        })
    }

    val focusAndBlurUpdateTheText by testCase {
        var state by remember { mutableStateOf("") }

        TestText(state)

        Input(type = InputType.Text, attrs = {
            id("focusableInput")
            onFocus {
                state = "focused"
            }
            onBlur {
                state = "blured"
            }
        })
    }

    val scrollUpdatesText by testCase {
        var state by remember { mutableStateOf("") }

        TestText(state)

        Div(
            attrs = {
                id("box")
                style {
                    property("overflow-y", "scroll")
                    height(200.px)
                    backgroundColor(Color.RGB(220, 220, 220))
                }
                onScroll {
                    state = "Scrolled"
                }
            }
        ) {
            repeat(500) {
                P {
                    Text("Scrollable content in Div - $it")
                }
            }
        }
    }

    val selectEventUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }

        P(attrs = { style { height(50.px) } }) { TestText(state) }

        Input(type = InputType.Text, attrs = {
            value("This is a text to be selected")
            id("selectableText")
            onSelect {
                state = "Text Selected"
            }
        })
    }

    val mouseEnterPlusExtraButtonsPressedUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }

        P(
            attrs = {
                style { height(50.px) }
            }
        ) { TestText(state) }

        Div(attrs = {
            id("box")
            style {
                backgroundColor("red")
                padding(50.px)
                width(300.px)
            }
            onMouseEnter {
                val buttonsPressed = mutableListOf<String>()
                if (it.altKey) buttonsPressed.add("ALT")
                if (it.ctrlKey) buttonsPressed.add("CTRL")
                if (it.shiftKey) buttonsPressed.add("SHIFT")
                if (it.metaKey) buttonsPressed.add("META")

                state = "ENTERED+${buttonsPressed.joinToString(separator = ",")}"
            }

        }) {
            Text("Enter mouse over me with buttons pressed (META, ALT, SHIFT, CTRL)")
        }
    }

    val onMouseContextMenuUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }

        P(
            attrs = {
                id("box")
                style { height(50.px) }
                onContextMenu {
                    if (it.button == 2.toShort()) {
                        it.preventDefault()
                        it.stopImmediatePropagation()
                        state = "MOUSE CONTEXT MENU"
                    }
                }
            }
        ) { TestText(state) }
    }

    val displayMouseCoordinates by testCase {
        var state by remember { mutableStateOf("None") }

        Div(
            attrs = {
                id("box")
                style {
                    width(200.px)
                    height(200.px)
                    backgroundColor("red")
                }
                onMouseMove {
                    state = "${it.x},${it.y}|${it.offsetX},${it.offsetY}"
                }
            }
        )

        P(
            attrs = {
                style { height(50.px) }
            }
        ) { TestText(state) }
    }
}
