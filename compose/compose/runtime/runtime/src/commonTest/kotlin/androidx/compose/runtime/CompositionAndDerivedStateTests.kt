/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime

import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the interaction between [derivedStateOf] and composition.
 */
@Stable
class CompositionAndDerivedStateTests {

    @Test
    fun derivedStateOfChangesInvalidate() = compositionTest {
        var a by mutableStateOf(31)
        var b by mutableStateOf(10)
        val answer by derivedStateOf { a + b }

        compose {
            Text("The answer is $answer")
        }

        validate {
            Text("The answer is ${a + b}")
        }

        a++
        expectChanges()

        b++
        expectChanges()

        revalidate()
    }

    @Test
    fun onlyInvalidatesIfResultIsDifferent() = compositionTest {
        var a by mutableStateOf(32)
        var b by mutableStateOf(10)
        val answer by derivedStateOf { a + b }

        compose {
            Text("The answer is $answer")
        }

        validate {
            Text("The answer is ${a + b}")
        }

        // A snapshot is necessary here otherwise the ui thread might see one changed but not
        // the other. A snapshot ensures that both modifications will be seen together.
        Snapshot.withMutableSnapshot {
            a += 1
            b -= 1
        }

        expectNoChanges()
        revalidate()

        a += 1

        // Change just one should reflect a change.
        expectChanges()
        revalidate()

        b -= 1

        // Change just one should reflect a change.
        expectChanges()
        revalidate()

        Snapshot.withMutableSnapshot {
            a += 1
            b -= 1
        }

        // Again, the change should not cause an invalidate.
        expectNoChanges()
        revalidate()
    }

    @Test
    fun onlyInvalidatesIfResultIsStructurallyDifferent() = compositionTest {
        var a by mutableStateOf(mutableListOf(32), referentialEqualityPolicy())
        var b by mutableStateOf(mutableListOf(10), referentialEqualityPolicy())
        val answer by derivedStateOf { a + b }

        compose {
            Text("The answer is $answer")
        }

        validate {
            Text("The answer is ${a + b}")
        }

        // A snapshot is necessary here otherwise the ui thread might see one changed but not
        // the other. A snapshot ensures that both modifications will be seen together.
        Snapshot.withMutableSnapshot {
            a = mutableListOf(32)
            b = mutableListOf(10)
        }

        // Change both to different instance should not result in a change.
        expectNoChanges()
        revalidate()

        a = mutableListOf(32)
        // Change just one should not result in a change.
        expectNoChanges()
        revalidate()
    }

    @Test
    fun onlyEvaluateDerivedStatesThatAreLive() = compositionTest {
        var a by mutableStateOf(11)

        val useNone = 0x00
        val useD = 0x01
        val useE = 0x02
        val useF = 0x04

        var use by mutableStateOf(useD)

        fun useToString(use: Int): String {
            var result = ""
            if (use and useD != 0) {
                result = "useD"
            }
            if (use and useE != 0) {
                if (result.isNotEmpty()) result += ", "
                result += "useE"
            }
            if (use and useF != 0) {
                if (result.isNotEmpty()) result += ", "
                result += "useF"
            }
            return result
        }

        var dCalculated = 0
        val d = "d" to derivedStateOf {
            dCalculated++
            a
        }

        var eCalculated = 0
        val e = "e" to derivedStateOf {
            eCalculated++
            a + 100
        }

        var fCalculated = 0
        val f = "f" to derivedStateOf {
            fCalculated++
            a + 1000
        }

        var dExpected = 0
        var eExpected = 0
        var fExpected = 0

        fun expect(modified: Int, previous: Int = -1) {
            if (modified and useD == useD) dExpected++
            if (modified and useE == useE) eExpected++
            if (modified and useF == useF) fExpected++

            val additionalInfo = if (previous >= 0) {
                " switching from ${useToString(previous)} to ${useToString(modified)}"
            } else ""
            assertEquals(dExpected, dCalculated, "d calculated an unexpected amount$additionalInfo")
            assertEquals(eExpected, eCalculated, "e calculated an unexpected amount$additionalInfo")
            assertEquals(fExpected, fCalculated, "f calculated an unexpected amount$additionalInfo")
        }

        // Nothing should be calculated yet.
        expect(useNone)

        compose {
            if (use and useD == useD) {
                Display(d)
            }
            if (use and useE == useE) {
                Display(e)
            }
            if (use and useF == useF) {
                Display(f)
            }
            if ((use and (useD or useE)) == useD or useE) {
                Display(d, e)
            }
            if ((use and (useD or useF)) == useD or useF) {
                Display(d, f)
            }
            if ((use and (useE or useF)) == useE or useF) {
                Display(e, f)
            }
            if ((use and (useD or useE or useF)) == useD or useE or useF) {
                Display(d, e, f)
            }
        }

        validate {
            if (use and useD != 0) {
                Text("d = $a")
            }
            if (use and useE != 0) {
                Text("e = ${a + 100}")
            }
            if (use and useF != 0) {
                Text("f = ${a + 1000}")
            }
            if ((use and (useD or useE)) == useD or useE) {
                Text("d = $a")
                Text("e = ${a + 100}")
            }
            if ((use and (useD or useF)) == useD or useF) {
                Text("d = $a")
                Text("f = ${a + 1000}")
            }
            if ((use and (useE or useF)) == useE or useF) {
                Text("e = ${a + 100}")
                Text("f = ${a + 1000}")
            }
            if ((use and (useD or useE or useF)) == useD or useE or useF) {
                Text("d = $a")
                Text("e = ${a + 100}")
                Text("f = ${a + 1000}")
            }
        }

        expect(useD)

        // Modify A
        a++
        expectChanges()
        revalidate()
        expect(useD)

        fun switchTo(newUse: Int) {
            val previous = use
            use = newUse
            a++
            expectChanges()
            revalidate()
            expect(newUse, previous)
        }

        switchTo(useD or useE)
        switchTo(useD or useF)

        val states = listOf(
            useE,
            useF,
            useD or useE,
            useD or useF,
            useD or useE or useF,
            useE or useF,
            useNone
        )
        for (newUse in states) {
            switchTo(newUse)
        }
    }

    @Test
    fun ensureCalculateIsNotCalledTooSoon() = compositionTest {
        var a by mutableStateOf(11)
        var dCalculated = 0
        var dChanged = false
        val d = "d" to derivedStateOf {
            dCalculated++
            a + 10
        }

        compose {
            Text("a = $a")
            val oldDCalculated = dCalculated
            Display(d)
            dChanged = oldDCalculated != dCalculated
        }

        validate {
            Text("a = $a")
            Text("d = ${a + 10}")
        }

        assertTrue(dChanged, "Expected d to recalculate")

        a++
        expectChanges()
        revalidate()
        assertTrue(dChanged, "Expected d to recalculate")
    }

    @Test
    fun writingToADerviedStateDependencyTriggersAForwardInvalidate() = compositionTest {
        var a by mutableStateOf(12)
        var b by mutableStateOf(30)
        val d = derivedStateOf { a + b }
        compose {
            DisplayIndirect("d", d)
            var c by remember { mutableStateOf(0) }
            c = a + b
            val e = remember { derivedStateOf { a + b + c } }
            DisplayIndirect("e", e)
        }

        validate {
            Text("d = ${a + b}")
            Text("e = ${a + b + a + b}")
        }

        a++
        expectChanges()
        revalidate()

        b--
        expectChanges()
        revalidate()

        Snapshot.withMutableSnapshot {
            a += 1
            b -= 1
        }
        advance()
        revalidate()
    }

    @Test // Regression test for 215402574
    fun observingBothNormalAndDerivedInSameScope() = compositionTest {
        val a = mutableStateOf(0)
        val b = derivedStateOf { a.value > 0 }
        val c = mutableStateOf(false)

        compose {
            Linear {
                if (b.value) Text("B is true")
                if (c.value) Text("C is true")
            }
        }

        validate {
            Linear {
                if (b.value) Text("B is true")
                if (c.value) Text("C is true")
            }
        }

        a.value++
        expectChanges()
        revalidate()

        a.value++
        advance()
        revalidate()

        a.value++
        Snapshot.sendApplyNotifications()

        c.value = true
        advance()
        revalidate()
    }

    @Test
    fun changingTheDerivedStateInstanceShouldRelease() = compositionTest {
        var reload by mutableStateOf(0)

        compose {
            val items = remember(reload) {
                derivedStateOf {
                    List(10) { it }
                }
            }

            Text("List of size ${items.value.size}")
        }

        validate {
            Text("List of size 10")
        }

        repeat(10) {
            reload++
            advance()
        }

        revalidate()

        // Validate there are only 2 observed objectt which should be `reload` and the last
        // created derivedStateOf instance
        val observed = (composition as? CompositionImpl)?.observedObjects ?: emptyList()
        assertEquals(2, observed.count())
    }

    @Test
    fun observingDerivedStateInMultipleScopes() = compositionTest {
        var observeInFirstScope by mutableStateOf(true)
        var count by mutableStateOf(0)

        compose {
            val items by remember {
                derivedStateOf {
                    List(count) { it }
                }
            }

            Linear {
                if (observeInFirstScope) {
                    Text("List of size ${items.size}")
                }
            }

            Linear {
                Text("List of size ${items.size}")
            }
        }

        validate {
            Linear {
                Text("List of size 0")
            }

            Linear {
                Text("List of size 0")
            }
        }

        observeInFirstScope = false
        advance()
        count++
        advance()

        validate {
            Linear {
            }

            Linear {
                Text("List of size 1")
            }
        }
    }

    @Test
    fun changingTheDerivedStateInstanceShouldClearDependencies() = compositionTest {
        var reload by mutableStateOf(0)

        compose {
            val itemValue = remember(reload) {
                derivedStateOf {
                    reload
                }
            }

            val items = remember(reload) {
                derivedStateOf {
                    List(10) { itemValue.value }
                }
            }

            Text("List of size ${items.value.size}")
        }

        validate {
            Text("List of size 10")
        }

        repeat(10) {
            reload++
            advance()
        }

        revalidate()

        // Validate there are only 2 observed dependencies, one per each derived state
        val observed = (composition as? CompositionImpl)?.derivedStateDependencies ?: emptyList()
        assertEquals(2, observed.count())
    }

    @Test
    fun changingDerivedStateDependenciesShouldClearThem() = compositionTest {
        var reload by mutableStateOf(0)

        compose {
            val itemValue = remember(reload) {
                derivedStateOf { 1 }
            }

            val intermediateState = rememberUpdatedState(itemValue)

            val snapshot = remember {
                derivedStateOf {
                    List(10) { intermediateState.value.value }
                }
            }

            Text("List of size ${snapshot.value.size}")
        }

        validate {
            Text("List of size 10")
        }

        repeat(10) {
            reload++
            advance()
        }

        revalidate()

        // Validate there are only 2 observed dependencies, one for intermediateState, one for itemValue
        val observed = (composition as? CompositionImpl)?.derivedStateDependencies ?: emptyList()
        assertEquals(2, observed.count())
    }

    @Test
    fun changingDerivedStateShouldNotAccumulateConditionalScopes() = compositionTest {

        var reload by mutableStateOf(0)

        compose {
            val derivedState = remember {
                derivedStateOf {
                    List(reload) { it }
                }
            }

            if (reload % 2 == 0) {
                Wrap {
                    Text("${derivedState.value.size}")
                }
            }
        }

        reload++

        advance()

        val conditionalScopes = (composition as? CompositionImpl)?.conditionalScopes ?: emptyList()

        assertEquals(0, conditionalScopes.count { it.isConditional })
    }

    @Test
    fun derivedStateOfNestedChangesInvalidate() = compositionTest {
        var a by mutableStateOf(31)
        var b by mutableStateOf(10)
        val transient by derivedStateOf { a + b }
        val answer by derivedStateOf { transient - 1 }

        compose {
            Text("The answer is $answer")
        }

        validate {
            Text("The answer is ${a + b - 1}")
        }

        a++
        expectChanges()

        b++
        expectChanges()

        revalidate()
    }

    @Test
    fun derivedStateOfMutationPolicyDoesNotInvalidateNestedStates() = compositionTest {
        var a by mutableStateOf(30)
        var b by mutableStateOf(10)
        val transient by derivedStateOf(structuralEqualityPolicy()) { (a + b) / 10 }
        var invalidateCount = 0
        val answer by derivedStateOf {
            invalidateCount++
            transient
        }

        compose {
            Text("The answer is $answer")
        }

        validate {
            Text("The answer is ${(a + b) / 10}")
        }

        assertEquals(1, invalidateCount)

        a += 10
        expectChanges()
        assertEquals(2, invalidateCount)

        b++
        expectNoChanges()
        assertEquals(2, invalidateCount)

        revalidate()
    }

    @Test
    fun derivedStateOfStructuralMutationPolicyDoesntRecompose() = compositionTest {
        var a by mutableStateOf(30)
        var b by mutableStateOf(10)
        val answer by derivedStateOf(structuralEqualityPolicy()) {
            listOf(a >= 30, b >= 10)
        }
        var compositionCount = 0

        compose {
            val remembered = rememberUpdatedState(answer)
            Linear {
                compositionCount++
                Text("The answer is ${remembered.value}")
            }
        }

        validate {
            Linear {
                Text("The answer is ${listOf(true, true)}")
            }
        }

        assertEquals(1, compositionCount)

        a++
        expectNoChanges() // the equivalent list doesn't cause changes
        assertEquals(1, compositionCount)

        b++
        expectNoChanges() // the equivalent list doesn't cause changes
        assertEquals(1, compositionCount)

        revalidate()
    }

    @Test
    fun derivedStateOfReferencialMutationPolicyRecomposes() = compositionTest {
        var a by mutableStateOf(30)
        var b by mutableStateOf(10)
        val answer by derivedStateOf(referentialEqualityPolicy()) {
            listOf(a >= 30, b >= 10)
        }
        var compositionCount = 0

        compose {
            val remembered = rememberUpdatedState(answer)
            Linear {
                compositionCount++
                Text("The answer is ${remembered.value}")
            }
        }

        validate {
            Linear {
                Text("The answer is ${listOf(true, true)}")
            }
        }

        assertEquals(1, compositionCount)

        a++
        expectNoChanges() // the equivalent list doesn't cause changes
        assertEquals(2, compositionCount)

        b++
        expectNoChanges() // the equivalent list doesn't cause changes
        assertEquals(3, compositionCount)

        revalidate()
    }
}

@Composable
private fun DisplayItem(name: String, state: State<Int>) {
    Text("$name = ${state.value}")
}

@Composable
private fun DisplayIndirect(name: String, state: State<Int>) {
    DisplayItem(name, state)
}

@Composable
private fun Display(vararg names: Pair<String, State<Int>>) {
    for ((name, state) in names) {
        DisplayIndirect(name, state)
    }
}
