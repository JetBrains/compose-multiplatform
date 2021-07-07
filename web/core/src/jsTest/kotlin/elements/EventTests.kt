package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.Options
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventTests {

    @Test
    fun buttonClickHandled() = runTest {
        var handled = false

        composition {
            Button(
                {
                    onClick { handled = true }
                }
            ) {}
        }

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))

        assertTrue(handled)
    }

    @Test
    fun multipleEventListenersCanBeAddedAndUsed() = runTest {
        var handled1 = false
        var handled2 = false

        composition {
            Button(
                {
                    onClick { handled1 = true }
                    onClick { handled2 = true }
                }
            ) {}
        }

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))

        assertTrue(handled1)
        assertTrue(handled2)
    }

    @Test
    fun multipleButtonClickHandled() = runTest {
        var count = 0

        composition {
            Button(attrs = { onClick { count++ } })
        }

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))

        assertEquals(3, count)
    }

    @Test
    fun buttonOnClickHandlerWithOnceOptionCalledOnlyOnce() = runTest {
        var count = 0

        composition {
            Button(attrs = { onClick(options = Options(once = Options.BooleanValue.True)) { count++ } })
        }

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))

        assertEquals(1, count)
    }

    @Test
    fun buttonOnClickHandlerWithOnceOptionCalledAgainAfterRecomposition() = runTest {
        var count = 0
        var composedTimes = 0

        var recomposeScope: RecomposeScope? = null

        composition {
            recomposeScope = currentRecomposeScope
            composedTimes++

            Button(attrs = { onClick(options = Options(once = Options.BooleanValue.True)) { count++ } })
        }

        assertEquals(1, composedTimes)

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))

        assertEquals(1, count)

        recomposeScope!!.invalidate()
        waitForAnimationFrame()

        assertEquals(2, composedTimes)

        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))
        btn.dispatchEvent(MouseEvent("click"))

        assertEquals(2, count)

    }

    @Test
    fun removedEventListenersGetNeverCalled() = runTest {
        var count = 0
        var composedTimes = 0

        var recomposeScope: RecomposeScope? = null

        composition {
            recomposeScope = currentRecomposeScope
            composedTimes++
            Button(attrs = { onClick { count++ } })
        }

        assertEquals(1, composedTimes)

        val btn = root.firstChild as HTMLElement

        btn.dispatchEvent(MouseEvent("click"))
        assertEquals(1, count)

        // This will force the update, thus it will cause the listeners being removed/added:
        recomposeScope!!.invalidate()
        waitForAnimationFrame()
        assertEquals(2, composedTimes)

        assertEquals(1, count)

        btn.dispatchEvent(MouseEvent("click"))
        assertEquals(2, count, "Removed listener should never be called")
    }

    @Test
    fun removedEventListenersWithNotDefaultOptionsGetNeverCalled() = runTest {
        var count = 0
        var composedTimes = 0

        var recomposeScope: RecomposeScope? = null

        composition {
            recomposeScope = currentRecomposeScope
            composedTimes++
            Button(attrs = { onClick(Options(passive = Options.BooleanValue.True)) { count++ } })
        }

        assertEquals(1, composedTimes)

        val btn = root.firstChild as HTMLElement

        btn.dispatchEvent(MouseEvent("click"))
        assertEquals(1, count)

        // This will force the update, thus it will cause the listeners being removed/added:
        recomposeScope!!.invalidate()
        waitForAnimationFrame()
        assertEquals(2, composedTimes)

        assertEquals(1, count)

        btn.dispatchEvent(MouseEvent("click"))
        assertEquals(2, count, "Removed listener should never be called")
    }

    @Test
    fun captureOptionEnsuresCorrectOrderOfListeners() = runTest {
        var callOrder = ""

        composition {
            Div(attrs = {
                onClick(options = Options(capture = Options.BooleanValue.True)) {
                    callOrder += "1"
                }
            }) {
                Div(attrs = {
                    onClick(options = Options(capture = Options.BooleanValue.True)) {
                        callOrder += "2"
                    }
                }) {
                    P(attrs = {
                        onClick {
                            callOrder += "3"
                        }
                    }) { Text("SomeText") }
                }
            }
        }

        val paragraph = root.firstChild!!.firstChild!!.firstChild as HTMLParagraphElement
        paragraph.dispatchEvent(MouseEvent("click"))

        assertEquals("123", callOrder)
    }

    @Test
    fun checkboxInputHandled() = runTest {
        var handled = false

        composition {
            Input(
                type = InputType.Checkbox,
                attrs = {
                    onInput { handled = true }
                }
            )
        }

        val checkbox = root.firstChild as HTMLInputElement
        checkbox.dispatchEvent(Event("input"))

        assertTrue(handled)
    }

    @Test
    fun radioButtonInputHandled() = runTest {
        var handled = false

        composition {
            Input(
                type = InputType.Radio,
                attrs = {
                    onInput { handled = true }
                }
            )
        }

        val radio = root.firstChild as HTMLInputElement
        radio.dispatchEvent(Event("input"))
        assertEquals(false, radio.checked)

        assertTrue(handled)
    }

    @Test
    fun textAreaInputHandled() = runTest {
        var handled = false

        composition {
            TextArea(
                {
                    onInput { handled = true }
                },
                value = ""
            )
        }

        val radio = root.firstChild as HTMLTextAreaElement
        radio.dispatchEvent(InputEvent("input"))

        assertEquals("", radio.value)

        assertTrue(handled)
    }
}
