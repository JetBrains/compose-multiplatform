/*
 * Copyright 2019 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeApi::class, InternalComposeApi::class)
package androidx.compose.runtime

import androidx.compose.runtime.mock.Contact
import androidx.compose.runtime.mock.ContactModel
import androidx.compose.runtime.mock.MockViewValidator
import androidx.compose.runtime.mock.Point
import androidx.compose.runtime.mock.Report
import androidx.compose.runtime.mock.TestMonotonicFrameClock
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.contact
import androidx.compose.runtime.mock.Edit
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Points
import androidx.compose.runtime.mock.Repeated
import androidx.compose.runtime.mock.ReportsReport
import androidx.compose.runtime.mock.ReportsTo
import androidx.compose.runtime.mock.SelectContact
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.skip
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Composable
fun Container(content: @Composable () -> Unit) = content()

@Stable
@OptIn(ExperimentalComposeApi::class, InternalComposeApi::class)
@Suppress("unused")
class CompositionTests {

    @AfterTest
    fun teardown() {
        clearRoots()
    }

    @Test
    fun testComposeAModel() = compositionTest {
        val model = testModel()
        compose {
            SelectContact(model)
        }

        validate {
            Linear {
                Linear {
                    Text("Filter:")
                    Edit("")
                }
                Linear {
                    Text(value = "Contacts:")
                    Linear {
                        contact(bob)
                        contact(jon)
                        contact(steve)
                    }
                }
            }
        }
    }

    @Test
    fun testRecomposeWithoutChanges() = compositionTest {
        val model = testModel()
        compose {
            SelectContact(model)
        }

        expectNoChanges()

        validate {
            SelectContact(model)
        }
    }

    @Test
    fun testInsertAContact() = compositionTest {
        val model =
            testModel(mutableListOf(bob, jon))
        var changed = {}

        compose {
            changed = invalidate
            SelectContact(model)
        }

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(bob)
                        contact(jon)
                    }
                }
            }
        }

        model.add(steve, after = bob)
        changed()
        expectChanges()

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(bob)
                        contact(steve)
                        contact(jon)
                    }
                }
            }
        }
    }

    @Test
    fun testMoveAContact() = compositionTest {
        val model = testModel(
            mutableListOf(
                bob,
                steve,
                jon
            )
        )
        var changed = {}

        compose {
            changed = invalidate
            SelectContact(model)
        }

        model.move(steve, after = jon)
        changed()
        expectChanges()

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(bob)
                        contact(jon)
                        contact(steve)
                    }
                }
            }
        }
    }

    @Test
    fun testChangeTheFilter() = compositionTest {
        val model = testModel(
            mutableListOf(
                bob,
                steve,
                jon
            )
        )
        var changed = {}

        compose {
            changed = invalidate
            SelectContact(model)
        }

        model.filter = "Jon"
        changed()
        expectChanges()

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(jon)
                    }
                }
            }
        }
    }

    @Test
    fun testComposeCompositionWithMultipleRoots() = compositionTest {
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )

        compose {
            ReportsReport(reports)
        }

        validate {
            ReportsReport(reports)
        }
    }

    @Test
    fun testMoveCompositionWithMultipleRoots() = compositionTest {
        var reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )
        var changed = { }
        compose {
            changed = invalidate
            ReportsReport(reports)
        }

        reports = listOf(
            jim_reports_to_sally,
            clark_reports_to_lois,
            rob_reports_to_alice
        )
        changed()
        expectChanges()

        validate {
            ReportsReport(reports)
        }
    }

    @Test
    fun testReplace() = compositionTest {
        var includeA = true
        var changed = { }

        @Composable
        fun Composition() {
            changed = invalidate
            Text("Before")
            if (includeA) {
                Linear {
                    Text("A")
                }
            } else {
                Edit("B")
            }
            Text("After")
        }

        fun MockViewValidator.Composition() {
            Text("Before")
            if (includeA) {
                Linear {
                    Text("A")
                }
            } else {
                Edit("B")
            }
            Text("After")
        }

        compose {
            Composition()
        }

        validate {
            this.Composition()
        }

        includeA = false
        changed()
        expectChanges()
        validate {
            this.Composition()
        }
        includeA = true
        changed()
        expectChanges()
        validate {
            this.Composition()
        }
        changed()
        expectNoChanges()
    }

    @Test
    fun testInsertWithMultipleRoots() = compositionTest {
        var chars = listOf('a', 'b', 'c')
        var changed = { }

        @Composable
        fun TextOf(c: Char) {
            Text(c.toString())
        }

        fun MockViewValidator.TextOf(c: Char) {
            Text(c.toString())
        }

        @Composable
        fun Chars(chars: Iterable<Char>) {
            Repeated(of = chars) { c -> TextOf(c) }
        }

        fun MockViewValidator.validateChars(chars: Iterable<Char>) {
            Repeated(of = chars) { c -> this.TextOf(c) }
        }

        compose {
            changed = invalidate
            Chars(chars)
            Chars(chars)
            Chars(chars)
        }

        validate {
            validateChars(chars)
            validateChars(chars)
            validateChars(chars)
        }

        chars = listOf('a', 'b', 'x', 'c')
        changed()
        expectChanges()

        validate {
            validateChars(chars)
            validateChars(chars)
            validateChars(chars)
        }
    }

    @Test
    fun testSimpleSkipping() = compositionTest {
        val points = listOf(Point(1, 2), Point(2, 3))
        var changed = {}
        compose {
            changed = invalidate
            Points(points)
        }

        validate { Points(points) }

        changed()
        expectNoChanges()
    }

    @Test
    fun testMovingMemoization() = compositionTest {
        var points = listOf(
            Point(1, 2),
            Point(2, 3),
            Point(4, 5),
            Point(6, 7)
        )
        var changed = { }
        compose {
            changed = invalidate
            Points(points)
        }

        validate { Points(points) }

        points = listOf(
            Point(1, 2),
            Point(4, 5),
            Point(2, 3),
            Point(6, 7)
        )
        changed()
        expectChanges()

        validate { Points(points) }
    }

    @Test
    fun testComponent() = compositionTest {
        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                Text(report.from)
                Text("reports to")
                Text(report.to)
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(reports: Iterable<Report>) {
            Linear {
                Repeated(of = reports) { report ->
                    Reporter(report)
                }
            }
        }

        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )
        compose {
            ReportsReport(reports)
        }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
            }
        }

        expectNoChanges()
    }

    @Test
    fun testComposeTwoAttributeComponent() = compositionTest {
        @Composable
        fun Two2(first: Int = 1, second: Int = 2) {
            Linear {
                Text("$first $second")
            }
        }

        fun MockViewValidator.two(first: Int, second: Int) {
            Linear {
                Text("$first $second")
            }
        }

        compose {
            Two2(41, 42)
        }

        validate {
            two(41, 42)
        }
    }

    @Test
    fun testComposeThreeAttributeComponent() = compositionTest {
        @Composable
        fun Three3(first: Int = 1, second: Int = 2, third: Int = 3) {
            Linear {
                Text("$first $second $third")
            }
        }

        fun MockViewValidator.Three(first: Int, second: Int, third: Int) {
            Linear {
                Text("$first $second $third")
            }
        }

        compose {
            Three3(41, 42, 43)
        }

        validate {
            Three(41, 42, 43)
        }
    }

    @Test
    fun testComposeFourOrMoreAttributeComponent() = compositionTest {
        @Composable
        fun Four4(
            first: Int = 1,
            second: Int = 2,
            third: Int = 3,
            fourth: Int = 4
        ) {
            Linear {
                Text("$first $second $third $fourth")
            }
        }

        fun MockViewValidator.Four(first: Int, second: Int, third: Int, fourth: Int) {
            Linear {
                Text("$first $second $third $fourth")
            }
        }

        compose {
            Four4(41, 42, 43, 44)
        }

        validate {
            Four(41, 42, 43, 44)
        }
    }

    @Test
    fun testSkippingACall() = compositionTest {

        @Composable
        fun Show(value: Int) {
            Linear {
                Text("$value")
            }
            Linear {
                Text("value")
            }
        }

        fun MockViewValidator.Show(value: Int) {
            Linear {
                Text("$value")
            }
            Linear {
                Text("value")
            }
        }

        @Composable
        fun Test(showThree: Boolean) {
            Show(1)
            Show(2)
            if (showThree) {
                Show(3)
            }
        }

        var showThree = false

        var changed = { }

        @Composable
        fun Test() {
            changed = invalidate
            Test(showThree)
        }

        fun MockViewValidator.Test(showThree: Boolean) {
            this.Show(1)
            this.Show(2)
            if (showThree) {
                this.Show(3)
            }
        }

        fun validate() {
            validate {
                this.Test(showThree)
            }
        }

        compose {
            Test()
        }

        validate()

        showThree = true
        changed()
        expectChanges()
        validate()
    }

    @Test
    fun testComponentWithVarConstructorParameter() = compositionTest {
        @Composable
        fun One(first: Int) {
            Text("$first")
        }

        fun MockViewValidator.One(first: Int) {
            Text("$first")
        }

        @Composable
        fun CallOne(value: Int) {
            One(first = value)
        }

        var value = 42
        var changed = { }
        compose {
            changed = invalidate
            CallOne(value)
        }

        validate {
            this.One(42)
        }

        value = 43
        changed()
        expectChanges()

        validate {
            this.One(43)
        }
    }

    @Test
    fun testComponentWithValConstructorParameter() = compositionTest {
        @Composable
        fun One(first: Int) {
            Text("$first")
        }

        fun MockViewValidator.One(first: Int) {
            Text("$first")
        }

        @Composable
        fun CallOne(value: Int) {
            One(first = value)
        }

        var value = 42
        var changed = { }
        compose {
            changed = invalidate
            CallOne(value)
        }

        validate {
            this.One(42)
        }

        value = 43
        changed()
        expectChanges()

        validate {
            this.One(43)
        }

        changed()
        expectNoChanges()
    }

    @Test
    fun testComposePartOfTree() = compositionTest {
        var recomposeLois = { }

        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                if (report.from == "Lois" || report.to == "Lois") recomposeLois = invalidate
                Text(report.from)
                Text("reports to")
                Text(report.to)
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(reports: Iterable<Report>) {
            Linear {
                Repeated(of = reports) { report ->
                    Reporter(report)
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois, r
        )
        compose {
            ReportsReport(reports)
        }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }

        expectNoChanges()

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Compose only the Lois report
        recomposeLois()

        expectChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }
    }

    @Test
    fun testRecomposeWithReplace() = compositionTest {
        var recomposeLois = { }
        var key = 0

        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                if (report.from == "Lois" || report.to == "Lois") recomposeLois = invalidate
                key(key) {
                    Text(report.from)
                    Text("reports to")
                    Text(report.to)
                }
            } else {
                Text("no report to report")
            }
        }

        @Composable fun ReportsReport(reports: Iterable<Report>) {
            Linear {
                Repeated(of = reports) { report ->
                    Reporter(report)
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois, r
        )
        compose {
            ReportsReport(reports)
        }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }

        expectNoChanges()

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Cause a new group to be generated in the component
        key = 2

        // Compose only the Lois report
        recomposeLois()

        expectChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }
    }

    @Test
    fun testInvalidationAfterRemoval() = compositionTest {
        var recomposeLois = { }
        val key = 0

        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                val callback = invalidate
                if (report.from == "Lois" || report.to == "Lois") recomposeLois = callback
                key(key) {
                    Text(report.from)
                    Text("reports to")
                    Text(report.to)
                }
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(
            reports: Iterable<Report>,
            include: (report: Report) -> Boolean
        ) {
            Linear {
                Repeated(of = reports) { report ->
                    if (include(report)) {
                        Reporter(report)
                    }
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois,
            r
        )
        val all: (report: Report) -> Boolean = { true }
        val notLois: (report: Report) -> Boolean = { it.from != "Lois" && it.to != "Lois" }

        var filter = all
        var changed = { }
        compose {
            changed = invalidate
            ReportsReport(reports, filter)
        }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }

        filter = notLois
        changed()
        expectChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
            }
        }

        // Invalidate Lois which is now removed.
        recomposeLois()
        expectNoChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
            }
        }
    }

    // remember()

    @Test
    fun testSimpleRemember() = compositionTest {
        var count = 0
        var changed = { }

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(value: Int) {
            changed = invalidate
            val w = remember { Wrapper(value) }
            Text("value = ${w.value}")
        }

        fun MockViewValidator.Test(value: Int) {
            Text("value = $value")
        }

        compose {
            Test(1)
        }

        validate { this.Test(1) }

        assertEquals(1, count)

        changed()
        expectNoChanges()

        // Expect the previous instance to be remembered
        assertEquals(1, count)
    }

    @Test
    fun testRememberOneParameter() = compositionTest {
        var count = 0

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(value: Int) {
            val w = remember(value) { Wrapper(value) }
            Text("value = ${w.value}")
        }

        fun MockViewValidator.Test(value: Int) {
            Text("value = $value")
        }

        var value = 1
        var changed = { }
        compose {
            changed = invalidate
            Test(value)
        }

        validate { this.Test(1) }

        value = 2
        changed()
        expectChanges()

        validate { this.Test(2) }

        changed()
        expectNoChanges()

        validate { this.Test(2) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberTwoParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int) {
            val w = remember(a, b) { Wrapper(a, b) }
            Text("a = ${w.a} b = ${w.b}")
        }

        fun MockViewValidator.Test(a: Int, b: Int) {
            Text("a = $a b = $b")
        }

        var p1 = 1
        var p2 = 2
        var changed = { }

        compose {
            changed = invalidate
            Test(p1, p2)
        }

        validate { this.Test(1, 2) }

        p1 = 2
        p2 = 3
        changed()
        expectChanges()

        validate { this.Test(2, 3) }

        changed()
        expectNoChanges()

        validate { this.Test(2, 3) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberThreeParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int, c: Int) {
            val w = remember(a, b, c) { Wrapper(a, b, c) }
            Text("a = ${w.a} b = ${w.b} c = ${w.c}")
        }

        fun MockViewValidator.Test(a: Int, b: Int, c: Int) {
            Text("a = $a b = $b c = $c")
        }

        var p3 = 3
        var changed = { }
        compose {
            changed = invalidate
            Test(1, 2, p3)
        }

        validate { this.Test(1, 2, 3) }

        p3 = 4
        changed()
        expectChanges()

        validate { this.Test(1, 2, 4) }

        changed()
        expectNoChanges()

        validate { this.Test(1, 2, 4) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberFourParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int, val d: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int, c: Int, d: Int) {
            val w = remember(a, b, c, d) { Wrapper(a, b, c, d) }
            Text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d}")
        }

        fun MockViewValidator.Test(a: Int, b: Int, c: Int, d: Int) {
            Text("a = $a b = $b c = $c d = $d")
        }

        var p3 = 3
        var p4 = 4
        var changed = { }

        compose {
            changed = invalidate
            Test(1, 2, p3, p4)
        }

        validate { this.Test(1, 2, 3, 4) }

        p3 = 4
        p4 = 5
        changed()
        expectChanges()

        validate { this.Test(1, 2, 4, 5) }

        changed()
        expectNoChanges()

        validate { this.Test(1, 2, 4, 5) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberFiveParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            val w = remember(a, b, c, d, e) { Wrapper(a, b, c, d, e) }
            Text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d} e = ${w.e}")
        }

        fun MockViewValidator.Test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            Text("a = $a b = $b c = $c d = $d e = $e")
        }

        var lastParameter = 5
        var changed = { }
        compose {
            changed = invalidate
            Test(1, 2, 3, 4, lastParameter)
        }

        validate { this.Test(1, 2, 3, 4, 5) }

        lastParameter = 6
        changed()

        expectChanges()

        validate { this.Test(1, 2, 3, 4, 6) }

        expectNoChanges()

        validate { this.Test(1, 2, 3, 4, 6) }

        assertEquals(2, count)
    }

    @Test
    fun testInsertGroupInContainer() = compositionTest {
        val values = mutableStateListOf(0)

        @Composable
        fun Composition() {
            Linear {
                for (value in values) {
                    Text("$value")
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                for (value in values)
                    Text("$value")
            }
        }

        compose { Composition() }

        validate { this.Composition() }

        for (i in 1..10) {
            values.add(i)
            expectChanges()
            validate { this.Composition() }
        }
    }

    // b/148273328
    @Test
    fun testInsertInGroups() = compositionTest {
        var threeVisible by mutableStateOf(false)

        @Composable fun Composition() {
            Linear {
                Text("one")
                Text("two")
                if (threeVisible) {
                    Text("three")
                    Text("four")
                }
                Linear {
                    Text("five")
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text("one")
                Text("two")
                if (threeVisible) {
                    Text("three")
                    Text("four")
                }
                Linear {
                    Text("five")
                }
            }
        }

        compose { Composition() }
        validate { this.Composition() }

        threeVisible = true
        expectChanges()

        validate { this.Composition() }
    }

    @Test
    fun testStartJoin() = compositionTest {
        var text by mutableStateOf("Starting")

        @Composable
        fun Composition() {
            Linear {
                Text(text)
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text(text)
            }
        }

        compose { Composition() }

        validate { this.Composition() }

        text = "Ending"

        expectChanges()

        validate { this.Composition() }
    }

    @Test
    fun testInvalidateJoin_End() = compositionTest {
        var text by mutableStateOf("Starting")
        var includeNested by mutableStateOf(true)

        @Composable
        fun Composition() {
            Linear {
                Text(text)
                if (includeNested) {
                    Text("Nested in $text")
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text(text)
                if (includeNested) {
                    Text("Nested in $text")
                }
            }
        }

        compose { Composition() }

        validate { this.Composition() }

        text = "Ending"
        includeNested = false

        expectChanges()

        validate { this.Composition() }

        expectNoChanges()

        validate { this.Composition() }
    }

    @Test
    fun testInvalidateJoin_Start() = compositionTest {
        var text by mutableStateOf("Starting")
        var includeNested by mutableStateOf(true)

        @Composable
        fun Composition() {
            Linear {
                if (includeNested) {
                    Text("Nested in $text")
                }
                Text(text)
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                if (includeNested) {
                    Text("Nested in $text")
                }
                Text(text)
            }
        }

        compose { Composition() }

        validate { this.Composition() }

        text = "Ending"
        includeNested = false

        expectChanges()

        validate { this.Composition() }

        expectNoChanges()

        validate { this.Composition() }
    }

    // b/132638679
    @Test
    fun testJoinInvalidate() = compositionTest {
        var texts = 5
        var invalidateOuter = { }
        var invalidateInner = { }
        var forceInvalidate = false

        @Composable fun Composition() {
            Linear {
                invalidateOuter = invalidate
                for (i in 1..texts) {
                    Text("Some text")
                }

                Container {
                    Text("Some text")

                    // Force the invalidation to survive the compose
                    val innerInvalidate = invalidate
                    invalidateInner = innerInvalidate
                    if (forceInvalidate) {
                        innerInvalidate()
                        forceInvalidate = false
                    }
                }
            }
        }

        compose { Composition() }

        texts = 4
        invalidateOuter()
        invalidateInner()
        forceInvalidate = true
        expectChanges()

        texts = 3
        invalidateOuter()
        forceInvalidate = true
        expectChanges()

        expectNoChanges()
    }

    @Test
    fun testLifecycle_Enter_Simple() = compositionTest {
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        var changed = { }

        @Composable
        fun Composition() {
            Linear {
                changed = invalidate
                remember { lifecycleObject }
                Text("Some text")
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text("Some text")
            }
        }

        compose {
            Composition()
        }
        validate { this.Composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        changed()
        expectNoChanges()
        validate { this.Composition() }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")
    }

    @Test
    fun testLifecycle_Enter_SingleNotification() = compositionTest {
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        var value by mutableStateOf(0)
        @Composable
        fun Composition() {
            Linear {
                val l = remember { lifecycleObject }
                assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                Text("Some text $value")
            }
            Linear {
                val l = remember { lifecycleObject }
                assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                Text("Some other text $value")
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text("Some text $value")
            }
            Linear {
                Text("Some other text $value")
            }
        }

        compose {
            Composition()
        }
        validate { this.Composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        value++
        expectChanges()
        validate { this.Composition() }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")
    }

    @Test
    fun testLifecycle_Leave_Simple() = compositionTest {
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        @Composable
        fun Composition(includeLifecycleObject: Boolean) {
            Linear {
                if (includeLifecycleObject) {
                    Linear {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        Text("Some text")
                    }
                }
            }
        }

        fun MockViewValidator.Composition(includeLifecycleObject: Boolean) {
            Linear {
                if (includeLifecycleObject) {
                    Linear {
                        Text("Some text")
                    }
                }
            }
        }

        var changed = { }
        var value = true
        compose {
            changed = invalidate
            Composition(value)
        }
        validate { this.Composition(true) }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        changed()
        expectNoChanges()
        validate { this.Composition(true) }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")

        value = false
        changed()
        expectChanges()
        validate { this.Composition(false) }

        assertEquals(0, lifecycleObject.count, "Object should have been notified of a leave")
    }

    @Test
    fun testLifecycle_Leave_NoLeaveOnReenter() = compositionTest {
        var expectedEnter = true
        var expectedLeave = true
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
                assertTrue(expectedEnter, "No enter expected")
            }

            override fun onLeave() {
                count--
                assertTrue(expectedLeave, "No leave expected")
            }
        }

        @Composable
        fun Composition(a: Boolean, b: Boolean, c: Boolean) {
            Linear {
                if (a) {
                    key(1) {
                        Linear {
                            val l = remember { lifecycleObject }
                            assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                            Text("a")
                        }
                    }
                }
                if (b) {
                    key(2) {
                        Linear {
                            val l = remember { lifecycleObject }
                            assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                            Text("b")
                        }
                    }
                }
                if (c) {
                    key(3) {
                        Linear {
                            val l = remember { lifecycleObject }
                            assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                            Text("c")
                        }
                    }
                }
            }
        }

        fun MockViewValidator.Composition(a: Boolean, b: Boolean, c: Boolean) {
            Linear {
                if (a) {
                    Linear {
                        Text("a")
                    }
                }
                if (b) {
                    Linear {
                        Text("b")
                    }
                }
                if (c) {
                    Linear {
                        Text("c")
                    }
                }
            }
        }

        expectedEnter = true
        expectedLeave = false

        var a = true
        var b = false
        var c = false
        var changed = { }
        compose {
            changed = invalidate
            Composition(a = a, b = b, c = c)
        }
        validate {
            this.Composition(
                a = true,
                b = false,
                c = false
            )
        }

        assertEquals(
            1,
            lifecycleObject.count,
            "object should have been notified of an enter"
        )

        expectedEnter = false
        expectedLeave = false
        changed()
        expectNoChanges()
        validate {
            this.Composition(
                a = true,
                b = false,
                c = false
            )
        }
        assertEquals(
            1,
            lifecycleObject.count,
            "Object should have only been notified once"
        )

        expectedEnter = false
        expectedLeave = false
        a = false
        b = true
        c = false
        changed()
        expectChanges()
        validate {
            this.Composition(
                a = false,
                b = true,
                c = false
            )
        }
        assertEquals(1, lifecycleObject.count, "No enter or leaves")

        expectedEnter = false
        expectedLeave = false
        a = false
        b = false
        c = true
        changed()
        expectChanges()
        validate {
            this.Composition(
                a = false,
                b = false,
                c = true
            )
        }
        assertEquals(1, lifecycleObject.count, "No enter or leaves")

        expectedEnter = false
        expectedLeave = false
        a = true
        b = false
        c = false
        changed()
        expectChanges()
        validate {
            this.Composition(
                a = true,
                b = false,
                c = false
            )
        }
        assertEquals(1, lifecycleObject.count, "No enter or leaves")

        expectedEnter = false
        expectedLeave = true
        a = false
        b = false
        c = false
        changed()
        expectChanges()
        validate {
            this.Composition(
                a = false,
                b = false,
                c = false
            )
        }
        assertEquals(0, lifecycleObject.count, "A leave")
    }

    @Test
    fun testLifecycle_Leave_LeaveOnReplace() = compositionTest {
        val lifecycleObject1 = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        val lifecycleObject2 = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        var lifecycleObject: Any = lifecycleObject1
        var changed = {}

        @Composable
        fun Composition(obj: Any) {
            Linear {
                key(1) {
                    Linear {
                        remember(obj) { obj }
                        Text("Some value")
                    }
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Linear {
                    Text("Some value")
                }
            }
        }

        compose {
            changed = invalidate
            Composition(obj = lifecycleObject)
        }
        validate { this.Composition() }
        assertEquals(1, lifecycleObject1.count, "first object should enter")
        assertEquals(0, lifecycleObject2.count, "second object should not have entered")

        lifecycleObject = lifecycleObject2
        changed()
        expectChanges()
        validate { Composition() }
        assertEquals(0, lifecycleObject1.count, "first object should have left")
        assertEquals(1, lifecycleObject2.count, "second object should have entered")

        lifecycleObject = object {}
        changed()
        expectChanges()
        validate { Composition() }
        assertEquals(0, lifecycleObject1.count, "first object should have left")
        assertEquals(0, lifecycleObject2.count, "second object should have left")
    }

    @Test
    fun testLifecycle_EnterLeaveOrder() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newLifecycleObject = { name: String ->
            object :
                CompositionLifecycleObserver,
                Counted,
                Ordered,
                Named {
                override var name = name
                override var count = 0
                override var enterOrder = -1
                override var leaveOrder = -1
                override fun onEnter() {
                    assertEquals(-1, enterOrder, "Only one call to onEnter expected")
                    enterOrder = order++
                    count++
                }

                override fun onLeave() {
                    assertEquals(-1, leaveOrder, "Only one call to onLeave expected")
                    leaveOrder = order++
                    count--
                }
            }.also { objects.add(it) }
        }

        @Composable
        fun LifecycleUser(name: String) {
            Linear {
                remember(name) { newLifecycleObject(name) }
                Text(value = name)
            }
        }

        /*
        A
        |- B
        |  |- C
        |  |- D
        |- E
        |- F
        |  |- G
        |  |- H
        |     |-I
        |- J

        Should enter as: A, B, C, D, E, F, G, H, I, J
        Should leave as: J, I, H, G, F, E, D, C, B, A
        */

        @Composable
        fun Tree() {
            Linear {
                LifecycleUser("A")
                Linear {
                    LifecycleUser("B")
                    Linear {
                        LifecycleUser("C")
                        LifecycleUser("D")
                    }
                    LifecycleUser("E")
                    LifecycleUser("F")
                    Linear {
                        LifecycleUser("G")
                        LifecycleUser("H")
                        Linear {
                            LifecycleUser("I")
                        }
                    }
                    LifecycleUser("J")
                }
            }
        }

        @Composable
        fun Composition(includeTree: Boolean) {
            Linear {
                if (includeTree) Tree()
            }
        }

        var value by mutableStateOf(true)

        compose {
            Composition(value)
        }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered"
        )

        value = false
        expectChanges()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left"
        )

        assertArrayEquals(
            "Expected enter order",
            arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
            objects.mapNotNull { it as? Ordered }.sortedBy { it.enterOrder }.map {
                (it as Named).name
            }.toTypedArray()
        )

        assertArrayEquals(
            "Expected leave order",
            arrayOf("J", "I", "H", "G", "F", "E", "D", "C", "B", "A"),
            objects.mapNotNull { it as? Ordered }.sortedBy { it.leaveOrder }.map {
                (it as Named).name
            }.toTypedArray()
        )
    }

    @Test
    fun testCompoundKeyHashStaysTheSameAfterRecompositions() = compositionTest {
        val outerKeys = mutableListOf<Int>()
        val innerKeys = mutableListOf<Int>()
        var previousOuterKeysSize = 0
        var previousInnerKeysSize = 0
        var outerInvalidate: (() -> Unit) = {}
        var innerInvalidate: (() -> Unit) = {}

        @Composable
        fun Test() {
            outerInvalidate = invalidate
            outerKeys.add(currentComposer.currentCompoundKeyHash)
            Container {
                Linear {
                    innerInvalidate = invalidate
                    innerKeys.add(currentComposer.currentCompoundKeyHash)
                }
            }
            // asserts that the key is correctly rolled back after start and end of Observe
            assertEquals(outerKeys.last(), currentComposer.currentCompoundKeyHash)
        }

        compose {
            Test()
        }

        assertNotEquals(previousOuterKeysSize, outerKeys.size)
        assertNotEquals(previousInnerKeysSize, innerKeys.size)

        previousOuterKeysSize = outerKeys.size
        outerInvalidate()
        expectNoChanges()
        assertNotEquals(previousOuterKeysSize, outerKeys.size)

        previousInnerKeysSize = innerKeys.size
        innerInvalidate()
        expectNoChanges()
        assertNotEquals(previousInnerKeysSize, innerKeys.size)

        assertNotEquals(innerKeys[0], outerKeys[0])
        innerKeys.forEach {
            assertEquals(innerKeys[0], it)
        }
        outerKeys.forEach {
            assertEquals(outerKeys[0], it)
        }
    }

    @Test // b/152753046
    fun testSwappingGroups() = compositionTest {
        val items = mutableListOf(0, 1, 2, 3, 4)
        var invalidateComposition = {}

        @Composable
        fun NoNodes() { }

        @Composable
        fun Test() {
            invalidateComposition = invalidate
            for (item in items) {
                key(item) {
                    NoNodes()
                }
            }
        }

        compose {
            Test()
        }

        // Swap 2 and 3
        items[2] = 3
        items[3] = 2
        invalidateComposition()

        expectChanges()
    }

    @Test // b/154650546
    fun testInsertOnMultipleLevels() = compositionTest {
        val items = mutableListOf(
            1 to mutableListOf(
                0, 1, 2, 3, 4
            ),
            3 to mutableListOf(
                0, 1, 2, 3, 4
            )
        )

        val invalidates = mutableListOf<() -> Unit>()
        fun invalidateComposition() {
            invalidates.forEach { it() }
            invalidates.clear()
        }

        @Composable
        fun Numbers(numbers: List<Int>) {
            Linear {
                Linear {
                    invalidates.add(invalidate)
                    for (number in numbers) {
                        Text("$number")
                    }
                }
            }
        }

        @Composable
        fun Item(number: Int, numbers: List<Int>) {
            Linear {
                invalidates.add(invalidate)
                Text("$number")
                Numbers(numbers)
            }
        }

        @Composable
        fun Test() {
            invalidates.add(invalidate)

            Linear {
                invalidates.add(invalidate)
                for ((number, numbers) in items) {
                    Item(number, numbers)
                }
            }
        }

        fun MockViewValidator.numbers(numbers: List<Int>) {
            Linear {
                Linear {
                    for (number in numbers) {
                        Text("$number")
                    }
                }
            }
        }

        fun MockViewValidator.item(number: Int, numbers: List<Int>) {
            Linear {
                Text("$number")
                numbers(numbers)
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                for ((number, numbers) in items) {
                    item(number, numbers)
                }
            }
        }

        compose {
            Test()
        }

        fun validate() {
            validate {
                this.Test()
            }
        }

        validate()

        // Add numbers to the list at 0 and 1
        items[0].second.add(2, 100)
        items[1].second.add(3, 200)

        // Add a list to the root.
        items.add(1, 2 to mutableListOf(0, 1, 2))

        invalidateComposition()

        expectChanges()
        validate()
    }

    @Test
    fun testInsertingAfterSkipping() = compositionTest {
        val items = mutableListOf(
            1 to listOf(0, 1, 2, 3, 4)
        )

        val invalidates = mutableListOf<() -> Unit>()
        fun invalidateComposition() {
            invalidates.forEach { it() }
            invalidates.clear()
        }

        @Composable
        fun Test() {
            invalidates.add(invalidate)

            Linear {
                for ((item, numbers) in items) {
                    Text(item.toString())
                    Linear {
                        invalidates.add(invalidate)
                        for (number in numbers) {
                            Text(number.toString())
                        }
                    }
                }
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                for ((item, numbers) in items) {
                    Text(item.toString())
                    Linear {
                        for (number in numbers) {
                            Text(number.toString())
                        }
                    }
                }
            }
        }

        compose {
            Test()
        }

        validate {
            this.Test()
        }

        items.add(2 to listOf(3, 4, 5, 6))
        invalidateComposition()

        expectChanges()
        validate {
            this.Test()
        }
    }

    @Test
    fun evenOddRecomposeGroup() = compositionTest {
        var includeEven = true
        var includeOdd = true
        val invalidates = mutableListOf<() -> Unit>()

        fun invalidateComposition() {
            for (invalidate in invalidates) {
                invalidate()
            }
            invalidates.clear()
        }

        @Composable
        fun Wrapper(content: @Composable () -> Unit) {
            content()
        }

        @Composable
        fun EmitText() {
            invalidates.add(invalidate)
            if (includeOdd) {
                key(1) {
                    Text("odd 1")
                }
            }
            if (includeEven) {
                key(2) {
                    Text("even 2")
                }
            }
            if (includeOdd) {
                key(3) {
                    Text("odd 3")
                }
            }
            if (includeEven) {
                key(4) {
                    Text("even 4")
                }
            }
        }

        @Composable
        fun Test() {
            Linear {
                Wrapper {
                    EmitText()
                }
                EmitText()
                Wrapper {
                    EmitText()
                }
                EmitText()
            }
        }

        fun MockViewValidator.Wrapper(children: () -> Unit) {
            children()
        }

        fun MockViewValidator.EmitText() {
            if (includeOdd) {
                Text("odd 1")
            }
            if (includeEven) {
                Text("even 2")
            }
            if (includeOdd) {
                Text("odd 3")
            }
            if (includeEven) {
                Text("even 4")
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                this.Wrapper {
                    this.EmitText()
                }
                this.EmitText()
                this.Wrapper {
                    this.EmitText()
                }
                this.EmitText()
            }
        }

        compose {
            Test()
        }

        fun validate() {
            validate {
                this.Test()
            }
        }
        validate()

        includeEven = false
        invalidateComposition()
        expectChanges()
        validate()

        includeEven = true
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        includeEven = false
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        includeEven = true
        invalidateComposition()
        expectChanges()
        validate()

        includeOdd = true
        invalidateComposition()
        expectChanges()
        validate()
    }

    @Test
    fun evenOddWithMovement() = compositionTest {
        var includeEven = true
        var includeOdd = true
        var order = listOf(1, 2, 3, 4)
        val invalidates = mutableListOf<() -> Unit>()

        fun invalidateComposition() {
            for (invalidate in invalidates) {
                invalidate()
            }
            invalidates.clear()
        }

        @Composable
        fun EmitText(all: Boolean) {
            invalidates.add(invalidate)
            for (i in order) {
                if (i % 2 == 1 && (all || includeOdd)) {
                    key(i) {
                        Text("odd $i")
                    }
                }
                if (i % 2 == 0 && (all || includeEven)) {
                    key(i) {
                        Text("even $i")
                    }
                }
            }
        }

        @Composable
        fun Test() {
            Linear {
                invalidates.add(invalidate)
                for (i in order) {
                    key(i) {
                        Text("group $i")
                        if (i == 2 || (includeEven && includeOdd)) {
                            Text("including everything")
                        } else {
                            if (includeEven) {
                                Text("including evens")
                            }
                            if (includeOdd) {
                                Text("including odds")
                            }
                        }
                        EmitText(i == 2)
                    }
                }
                EmitText(false)
            }
        }

        fun MockViewValidator.EmitText(all: Boolean) {
            for (i in order) {
                if (i % 2 == 1 && (includeOdd || all)) {
                    Text("odd $i")
                }
                if (i % 2 == 0 && (includeEven || all)) {
                    Text("even $i")
                }
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                for (i in order) {
                    Text("group $i")
                    if (i == 2 || (includeEven && includeOdd)) {
                        Text("including everything")
                    } else {
                        if (includeEven) {
                            Text("including evens")
                        }
                        if (includeOdd) {
                            Text("including odds")
                        }
                    }
                    this.EmitText(i == 2)
                }
                this.EmitText(false)
            }
        }

        compose {
            Test()
        }

        fun validate() {
            validate {
                this.Test()
            }
        }
        validate()

        order = listOf(1, 2, 4, 3)
        includeEven = false
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(1, 4, 2, 3)
        includeEven = true
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(3, 4, 2, 1)
        includeEven = false
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(4, 3, 2, 1)
        includeEven = true
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(1, 2, 3, 4)
        includeOdd = true
        invalidateComposition()
        expectChanges()
        validate()
    }

    @Test
    fun testObservationScopes() = compositionTest {
        val states = mutableListOf<MutableState<Int>>()
        var iterations = 0

        @Composable
        fun Test() {
            val s1 = mutableStateOf(iterations++)
            Text("s1 ${s1.value}")
            states.add(s1)
            val s2 = mutableStateOf(iterations++)
            Text("s2 ${s2.value}")
            states.add(s2)
        }

        compose {
            Test()
        }

        fun invalidateFirst() {
            states.first().value++
        }

        fun invalidateLast() {
            states.last().value++
        }

        repeat(10) {
            invalidateLast()
            expectChanges()
        }

        invalidateFirst()
        expectNoChanges()
    }

    @OptIn(ComposeCompilerApi::class)
    @Test
    fun testApplierBeginEndCallbacks() = compositionTest {
        val checks = mutableListOf<String>()
        compose {
            val myComposer = currentComposer
            val myApplier = myComposer.applier as ViewApplier
            assertEquals(0, myApplier.onBeginChangesCalled, "onBeginChanges during composition")
            assertEquals(0, myApplier.onEndChangesCalled, "onEndChanges during composition")
            checks += "composition"

            SideEffect {
                assertEquals(1, myApplier.onBeginChangesCalled, "onBeginChanges during side effect")
                assertEquals(1, myApplier.onEndChangesCalled, "onEndChanges during side effect")
                checks += "SideEffect"
            }

            // Memo to future self:
            // Without the explicit generic definition of CompositionLifecycleObserver here,
            // the type of this remember call is inferred to be `Unit` thanks to the call's position
            // as the last expression in a unit lambda (the argument to `compose {}`). The remember
            // lambda is in turn interpreted as returning Unit, the object expression is dropped
            // on the floor for the gc, and Unit is written into the slot table.
            remember<CompositionLifecycleObserver> {
                object : CompositionLifecycleObserver {
                    override fun onEnter() {
                        assertEquals(
                            1,
                            myApplier.onBeginChangesCalled,
                            "onBeginChanges during lifecycle observer"
                        )
                        assertEquals(
                            1,
                            myApplier.onEndChangesCalled,
                            "onEndChanges during lifecycle observer"
                        )
                        checks += "CompositionLifecycleObserver"
                    }
                }
            }
        }
        assertEquals(
            listOf(
                "composition",
                "CompositionLifecycleObserver",
                "SideEffect"
            ),
            checks,
            "expected order of calls"
        )
    }

    @Test // regression test for b/172660922
    fun testInvalidationOfRemovedContent() = compositionTest {
        var markS1Invalid: () -> Unit = {}
        var viewS1 by mutableStateOf(true)
        var performBackwardsWrite = true
        @Composable
        fun S1() {
            markS1Invalid = invalidate
            Text("In s1")
        }

        @Composable
        fun S2() {
            Text("In s2")
        }

        @Composable
        fun Test() {
            Text("$viewS1")
            Wrap {
                if (viewS1) {
                    S1()
                }
                S2()
            }

            if (performBackwardsWrite) {
                // This forces the equivalent of a backwards write.
                markS1Invalid()
                performBackwardsWrite = false
            }
        }

        fun MockViewValidator.S1() {
            Text("In s1")
        }

        fun MockViewValidator.S2() {
            Text("In s2")
        }

        fun MockViewValidator.Test() {
            Text("$viewS1")
            if (viewS1) {
                this.S1()
            }
            this.S2()
        }

        compose {
            Test()
        }

        fun validate() = validate { this.Test() }

        validate()

        markS1Invalid()
        performBackwardsWrite = true
        expectNoChanges()

        validate()

        viewS1 = false
        performBackwardsWrite = true
        expectChanges()
        validate()
    }

    /**
     * This test checks that an updated ComposableLambda capture used in a subcomposition
     * correctly invalidates that subcomposition and schedules recomposition of that subcomposition.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testComposableLambdaSubcompositionInvalidation() = runBlockingTest {
        localRecomposerTest { recomposer ->
            val composer = Composer(EmptyApplier(), recomposer)
            try {
                var rootState by mutableStateOf(false)
                val composedResults = mutableListOf<Boolean>()
                Snapshot.notifyObjectsInitialized()
                recomposer.composeInitial(composer) {
                    // Read into local variable, local will be captured below
                    val capturedValue = rootState
                    TestSubcomposition {
                        composedResults.add(capturedValue)
                    }
                }
                composer.applyChanges()
                assertEquals(listOf(false), composedResults)
                rootState = true
                Snapshot.sendApplyNotifications()
                advanceUntilIdle()
                assertEquals(listOf(false, true), composedResults)
            } finally {
                composer.dispose()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCompositionReferenceIsRemembered() = runBlockingTest {
        localRecomposerTest { recomposer ->
            val composer = Composer(EmptyApplier(), recomposer)
            try {
                lateinit var invalidator: () -> Unit
                val parentReferences = mutableListOf<CompositionReference>()
                recomposer.composeInitial(composer) {
                    invalidator = invalidate
                    parentReferences += compositionReference()
                }
                composer.applyChanges()
                invalidator()
                advanceUntilIdle()
                assert(parentReferences.size > 1) { "expected to be composed more than once" }
                assert(parentReferences.toSet().size == 1) {
                    "expected all parentReferences to be the same; saw $parentReferences"
                }
            } finally {
                composer.dispose()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testParentCompositionRecomposesFirst() = runBlockingTest {
        localRecomposerTest { recomposer ->
            val composer = Composer(EmptyApplier(), recomposer)
            val results = mutableListOf<String>()
            try {
                var firstState by mutableStateOf("firstInitial")
                var secondState by mutableStateOf("secondInitial")
                Snapshot.notifyObjectsInitialized()
                recomposer.composeInitial(composer) {
                    results += firstState
                    TestSubcomposition {
                        results += secondState
                    }
                }
                secondState = "secondSet"
                Snapshot.sendApplyNotifications()
                firstState = "firstSet"
                Snapshot.sendApplyNotifications()
                advanceUntilIdle()
                assertEquals(
                    listOf("firstInitial", "secondInitial", "firstSet", "secondSet"),
                    results,
                    "Expected call ordering during recomposition of subcompositions"
                )
            } finally {
                composer.dispose()
            }
        }
    }

    /**
     * An [Applier] may inadvertently (or on purpose) run arbitrary user code as a side effect
     * of performing tree manipulations as a [Composer] is applying changes. This can happen
     * if the tree type dispatches event callbacks when nodes are added or removed from a tree.
     * These callbacks may cause snapshot state writes, which can in turn invalidate scopes in the
     * composition that produced the tree in the first place. Ensure that the recomposition
     * machinery is robust to this, and that these invalidations are processed on a subsequent
     * recomposition.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testStateWriteInApplier() = runBlockingTest {

        class MutateOnRemoveApplier(
            private val removeCounter: MutableState<Int>
        ) : AbstractApplier<Unit>(Unit) {
            var insertCount: Int = 0
                private set

            override fun remove(index: Int, count: Int) {
                removeCounter.value += count
            }

            override fun onClear() {
                // do nothing
            }

            override fun insertTopDown(index: Int, instance: Unit) {
                insertCount++
            }

            override fun insertBottomUp(index: Int, instance: Unit) {
                // do nothing
            }

            override fun move(from: Int, to: Int, count: Int) {
                // do nothing
            }
        }

        localRecomposerTest { recomposer ->
            val stateMutatedOnRemove = mutableStateOf(0)
            var shouldEmitNode by mutableStateOf(true)
            var compositionCount = 0
            Snapshot.notifyObjectsInitialized()
            val applier = MutateOnRemoveApplier(stateMutatedOnRemove)
            val composer = Composer(applier, recomposer)
            try {
                recomposer.composeInitial(composer) {
                    compositionCount++
                    // Read the state here so that the emit removal will invalidate it
                    stateMutatedOnRemove.value
                    if (shouldEmitNode) {
                        emit<Unit, MutateOnRemoveApplier>({ Unit }) {}
                    }
                }
                // Initial composition should not contain the node we will remove. We want to test
                // recomposition for this case in particular.
                assertEquals(1, applier.insertCount, "expected setup node not inserted")
                shouldEmitNode = false
                Snapshot.sendApplyNotifications()
                advanceUntilIdle()
                assertEquals(1, stateMutatedOnRemove.value, "observable removals performed")
                // Only two composition passes should have been performed by this point; a state
                // invalidation in the applier should not be picked up or acted upon until after
                // this frame is complete.
                assertEquals(2, compositionCount, "expected number of (re)compositions performed")
                // After sending apply notifications we expect the snapshot state change made by
                // the applier to trigger one final recomposition.
                Snapshot.sendApplyNotifications()
                advanceUntilIdle()
                assertEquals(3, compositionCount, "expected number of (re)compositions performed")
            } finally {
                composer.dispose()
            }
        }
    }
}

@OptIn(InternalComposeApi::class, ExperimentalComposeApi::class)
@Composable
private fun TestSubcomposition(
    content: @Composable () -> Unit
) {
    val parentRef = compositionReference()
    val currentContent by rememberUpdatedState(content)
    DisposableEffect(parentRef) {
        val subcomposer = Composer(EmptyApplier(), parentRef)
        parentRef.composeInitial(subcomposer) {
            currentContent()
        }
        subcomposer.applyChanges()
        onDispose {
            subcomposer.dispose()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <R> localRecomposerTest(
    block: CoroutineScope.(Recomposer) -> R
) = coroutineScope {
    val contextWithClock = coroutineContext + TestMonotonicFrameClock(this)
    val recomposer = Recomposer(contextWithClock)
    launch(contextWithClock) {
        recomposer.runRecomposeAndApplyChanges()
    }
    block(recomposer)
    // This call doesn't need to be in a finally; everything it does will be torn down
    // in exceptional cases by the coroutineScope failure
    recomposer.shutDown()
}

@Composable fun Wrap(content: @Composable () -> Unit) {
    content()
}

private fun <T> assertArrayEquals(message: String, expected: Array<T>, received: Array<T>) {
    fun Array<T>.getString() = this.joinToString(", ") { it.toString() }
    fun err(msg: String): Nothing = error(
        "$message: $msg, expected: [${
        expected.getString()}], received: [${received.getString()}]"
    )
    if (expected.size != received.size) err("sizes are different")
    expected.indices.forEach { index ->
        if (expected[index] != received[index])
            err(
                "item at index $index was different (expected [${
                expected[index]}], received: [${received[index]}]"
            )
    }
}

// Contact test data
private val bob = Contact("Bob Smith", email = "bob@smith.com")
private val jon = Contact(name = "Jon Alberton", email = "jon@alberton.com")
private val steve = Contact("Steve Roberson", email = "steverob@somemail.com")

private fun testModel(
    contacts: MutableList<Contact> = mutableListOf(
        bob,
        jon,
        steve
    )
) = ContactModel(filter = "", contacts = contacts)

// Report test data
private val jim_reports_to_sally = Report("Jim", "Sally")
private val rob_reports_to_alice = Report("Rob", "Alice")
private val clark_reports_to_lois = Report("Clark", "Lois")

private interface Counted {
    val count: Int
}

private interface Ordered {
    val enterOrder: Int
    val leaveOrder: Int
}

private interface Named {
    val name: String
}

@OptIn(ExperimentalComposeApi::class)
private class EmptyApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) {}
    override fun up() {}
    override fun insertTopDown(index: Int, instance: Unit) {
        error("Unexpected")
    }
    override fun insertBottomUp(index: Int, instance: Unit) {
        error("Unexpected")
    }
    override fun remove(index: Int, count: Int) {
        error("Unexpected")
    }
    override fun move(from: Int, to: Int, count: Int) {
        error("Unexpected")
    }
    override fun clear() {}
}
