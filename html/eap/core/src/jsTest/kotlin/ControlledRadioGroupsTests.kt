package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.attributes.builders.controlledRadioGroups
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.dom.RadioInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.web.testutils.*

class ControlledRadioGroupsTests {

    @Test
    fun controlledRadioGroupGetsUpdated() = runTest {
        var countOfRadio by mutableStateOf(0)

        composition {
            repeat(countOfRadio) {
                key (it) {
                    RadioInput(checked = false) {
                        id("r$it")
                        name("group1")
                    }
                }
            }
        }

        assertEquals(0, controlledRadioGroups.size)

        countOfRadio = 5
        waitForRecompositionComplete()

        assertEquals(1, controlledRadioGroups.size)
        assertTrue(controlledRadioGroups.keys.first() == "group1")
        assertEquals(5, controlledRadioGroups["group1"]!!.size)

        countOfRadio = 2
        waitForRecompositionComplete()

        assertEquals(1, controlledRadioGroups.size)
        assertTrue(controlledRadioGroups.keys.first() == "group1")
        assertEquals(2, controlledRadioGroups["group1"]!!.size)

        countOfRadio = 0
        waitForRecompositionComplete()

        assertEquals(0, controlledRadioGroups.size)
    }

    @Test
    fun multipleControlledRadioGroupsGetUpdated() = runTest {
        var countOfRadioG1 by mutableStateOf(0)
        var countOfRadioG2 by mutableStateOf(0)

        composition {
            Div {
                repeat(countOfRadioG1) {
                    key(it) {
                        RadioInput(checked = false) {
                            id("r1-$it")
                            name("group1")
                        }
                    }
                }
            }

            Div {
                repeat(countOfRadioG2) {
                    key(it) {
                        RadioInput(checked = false) {
                            id("r2-$it")
                            name("group2")
                        }
                    }
                }
            }
        }

        assertEquals(0, controlledRadioGroups.size)

        countOfRadioG1 = 5
        countOfRadioG2 = 10
        waitForRecompositionComplete()

        assertEquals(2, controlledRadioGroups.size)
        assertEquals(5, controlledRadioGroups["group1"]!!.size)
        assertEquals(10, controlledRadioGroups["group2"]!!.size)

        countOfRadioG2 = 2
        waitForRecompositionComplete()

        assertEquals(2, controlledRadioGroups.size)
        assertEquals(5, controlledRadioGroups["group1"]!!.size)
        assertEquals(2, controlledRadioGroups["group2"]!!.size)

        countOfRadioG1 = 0
        waitForRecompositionComplete()

        assertEquals(1, controlledRadioGroups.size)
        assertEquals(2, controlledRadioGroups["group2"]!!.size)

        countOfRadioG2 = 0
        waitForRecompositionComplete()

        assertEquals(0, controlledRadioGroups.size)
    }
}
