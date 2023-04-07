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
                    backgroundColor(Color.red)
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
                    backgroundColor(rgb(220, 220, 220))
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
                state = it.selection()
            }
        })
    }

    val selectEventInTextAreaUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }
        var selectedIndexes by remember { mutableStateOf("None") }

        P(attrs = { style { height(50.px) } }) { TestText(state) }
        P(attrs = { style { height(50.px) } }) { TestText(selectedIndexes, id = "txt2") }

        TextArea(value = "This is a text to be selected", attrs = {
            id("textArea")
            onSelect {
                state = it.selection()
                selectedIndexes = "${it.selectionStart},${it.selectionEnd}"
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
                backgroundColor(Color.red)
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
                    backgroundColor(Color.red)
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

    val copyPasteEventsTest by testCase {
        var state by remember { mutableStateOf("None") }

        P {
            TestText(state)
        }

        Div(attrs = {
            onCopy {
                it.preventDefault()
                it.setData("text", "COPIED_TEXT_WAS_OVERRIDDEN")
            }
        }) {
            TestText("SomeTestTextToCopy1", id = "txt_to_copy")
        }

        Div {
            TestText("SomeTestTextToCopy2", id = "txt_to_copy2")
        }

        TextInput(value = state) {
            id("textinput")
            onPaste {
                state = it.getData("text")?.lowercase() ?: "None"
            }
        }
    }

    val cutPasteEventsTest by testCase {
        var state by remember { mutableStateOf("None") }

        var stateToCut by remember { mutableStateOf("TextToCut") }

        P {
            TestText(state)
        }

        TextInput(value = stateToCut) {
            id("textinput1")
            onCut {
                state = "Text was cut"
                stateToCut = ""
            }
        }

        TextInput(value = state) {
            id("textinput2")
            onPaste {
                state = "Modified pasted text = ${it.getData("text")}"
            }
        }
    }

    val keyDownKeyUpTest by testCase {
        var stateDown by remember { mutableStateOf("None") }
        var stateUp by remember { mutableStateOf("None") }

        P {
            TestText(stateDown, id = "txt_down")
        }
        P {
            TestText(stateUp, id = "txt_up")
        }

        TextInput(value = "") {
            id("textinput")
            onKeyDown {
                stateDown = "keydown = ${it.key}"
                it.preventDefault()
            }
            onKeyUp {
                stateUp = "keyup = ${it.key}"
                it.preventDefault()
            }
        }
    }

    val touchEventsDispatched by testCase {
        var touchStart by remember { mutableStateOf("None") }
        var touchMove by remember { mutableStateOf("None") }
        var touchEnd by remember { mutableStateOf("None") }

        P {
            TestText(touchStart, id = "txt_start")
        }
        P {
            TestText(touchMove, id = "txt_move")
        }
        P {
            TestText(touchEnd, id = "txt_end")
        }

        Div(attrs = {
            id("box")

            onTouchStart {
                touchStart = "STARTED"
            }

            onTouchMove {
                touchMove = "MOVED"
            }

            onTouchEnd {
                touchEnd = "ENDED"
            }

            style {
                width(300.px)
                height(300.px)
                backgroundColor(Color.red)
            }
        }) {
            Text("Touch me and move the pointer")
        }
    }

    val animationEventsDispatched by testCase {
        var animationStart by remember { mutableStateOf("None") }
        var animationEnd by remember { mutableStateOf("None") }

        var shouldAddBounceClass by remember { mutableStateOf(false) }

        Style(AppStyleSheetWithAnimation)

        P {
            TestText(value = animationStart, id = "txt_start")
        }
        P {
            TestText(value = animationEnd, id = "txt_end")
        }

        Div(attrs = {
            id("box")
            if (shouldAddBounceClass) classes(AppStyleSheetWithAnimation.bounceClass)

            onClick {
                shouldAddBounceClass = true
            }
            onAnimationStart {
                animationStart = "STARTED - ${it.animationName}"
            }
            onAnimationEnd {
                shouldAddBounceClass = false
                animationEnd = "ENDED"
            }
            style {
                backgroundColor(Color.red)
            }
        }) {
            Text("Click to Animate")
        }
    }

    val onSubmitEventForFormDispatched by testCase {
        var state by remember { mutableStateOf("None") }

        P { TestText(value = state) }

        Form(action = "#", attrs = {
            onSubmit {
                it.preventDefault()
                state = "Form submitted"
            }
        }) {
            Button(attrs = {
                id("send_form_btn")
                type(ButtonType.Submit)
            }) {
                Text("Send Form")
            }
        }
    }

    val onResetEventForFormDispatched by testCase {
        var state by remember { mutableStateOf("None") }

        P { TestText(value = state) }

        Form(action = "#", attrs = {
            onReset {
                it.preventDefault()
                state = "Form reset"
            }
        }) {
            Button(attrs = {
                id("reset_form_btn")
                type(ButtonType.Reset)
            }) {
                Text("Send Form")
            }
        }
    }
}


object AppStyleSheetWithAnimation : StyleSheet() {
    val bounce by keyframes {
        from {
            property("transform", "translateX(50%)")
        }

        to {
            property("transform", "translateX(-50%)")
        }
    }

    val bounceClass by style {
        color(Color.green)
        animation(bounce) {
            duration(500.ms)
            timingFunction(AnimationTimingFunction.EaseIn)
            direction(AnimationDirection.Alternate)
        }
    }
}
