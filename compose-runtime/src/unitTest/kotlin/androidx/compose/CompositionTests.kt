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
package androidx.compose

import androidx.compose.mock.Contact
import androidx.compose.mock.ContactModel
import androidx.compose.mock.MockComposeScope
import androidx.compose.mock.MockViewComposer
import androidx.compose.mock.MockViewValidator
import androidx.compose.mock.Point
import androidx.compose.mock.Report
import androidx.compose.mock.View
import androidx.compose.mock.contact
import androidx.compose.mock.edit
import androidx.compose.mock.linear
import androidx.compose.mock.memoize
import androidx.compose.mock.points
import androidx.compose.mock.repeat
import androidx.compose.mock.reportsReport
import androidx.compose.mock.reportsTo
import androidx.compose.mock.selectContact
import androidx.compose.mock.skip
import androidx.compose.mock.text
import androidx.compose.mock.validate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Composable fun Container(body: @Composable () -> Unit) = body()

@Stable
class CompositionTests {

    @After
    fun teardown() {
        clearRoots()
    }

    @Test
    fun testComposeAModel() {
        val model = testModel()
        val myComposer = compose {
            selectContact(model)
        }

        validate(myComposer.root) {
            linear {
                linear {
                    text("Filter:")
                    edit("")
                }
                linear {
                    text(value = "Contacts:")
                    linear {
                        contact(bob)
                        contact(jon)
                        contact(steve)
                    }
                }
            }
        }
    }

    @Test
    fun testRecomposeWithoutChanges() {
        val model = testModel()
        val myComposer = compose {
            selectContact(model)
        }

        myComposer.expectNoChanges()

        validate(myComposer.root) {
            selectContact(model)
        }
    }

    @Test
    fun testInsertAContact() {
        val model =
            testModel(mutableListOf(bob, jon))
        var changed = {}
        val myComposer = compose {
            changed = invalidate
            selectContact(model)
        }

        validate(myComposer.root) {
            linear {
                skip()
                linear {
                    skip()
                    linear {
                        contact(bob)
                        contact(jon)
                    }
                }
            }
        }

        model.add(steve, after = bob)
        changed()
        myComposer.expectChanges()

        validate(myComposer.root) {
            linear {
                skip()
                linear {
                    skip()
                    linear {
                        contact(bob)
                        contact(steve)
                        contact(jon)
                    }
                }
            }
        }
    }

    @Test
    fun testMoveAContact() {
        val model = testModel(
            mutableListOf(
                bob,
                steve,
                jon
            )
        )
        var changed = {}
        val myComposer = compose {
            changed = invalidate
            selectContact(model)
        }

        model.move(steve, after = jon)
        changed()
        myComposer.expectChanges()

        validate(myComposer.root) {
            linear {
                skip()
                linear {
                    skip()
                    linear {
                        contact(bob)
                        contact(jon)
                        contact(steve)
                    }
                }
            }
        }
    }

    @Test
    fun testChangeTheFilter() {
        val model = testModel(
            mutableListOf(
                bob,
                steve,
                jon
            )
        )
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            selectContact(model)
        }

        model.filter = "Jon"
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) {
            linear {
                skip()
                linear {
                    skip()
                    linear {
                        contact(jon)
                    }
                }
            }
        }
    }

    @Test
    fun testComposeCompositionWithMultipleRoots() {
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )

        val myComposer = compose {
            reportsReport(reports)
        }

        validate(myComposer.root) {
            reportsReport(reports)
        }
    }

    @Test
    fun testMoveCompositionWithMultipleRoots() {
        var reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            reportsReport(reports)
        }

        reports = listOf(
            jim_reports_to_sally,
            clark_reports_to_lois,
            rob_reports_to_alice
        )
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) {
            reportsReport(reports)
        }
    }

    @Test
    fun testReplace() {
        var includeA = true
        var changed: (() -> Unit)? = null
        @Composable fun MockComposeScope.composition() {
            changed = invalidate
            text("Before")
            if (includeA) {
                linear {
                    text("A")
                }
            } else {
                edit("B")
            }
            text("After")
        }
        fun MockViewValidator.composition() {
            text("Before")
            if (includeA) {
                linear {
                    text("A")
                }
            } else {
                edit("B")
            }
            text("After")
        }
        val myComposer = compose {
            composition()
        }
        validate(myComposer.root) {
            composition()
        }
        includeA = false
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) {
            composition()
        }
        includeA = true
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) {
            composition()
        }
    }

    @Test
    fun testInsertWithMultipleRoots() {
        var chars = listOf('a', 'b', 'c')
        var changed: (() -> Unit)? = null

        @Composable fun MockComposeScope.textOf(c: Char) {
            text(c.toString())
        }

        fun MockViewValidator.textOf(c: Char) {
            text(c.toString())
        }

        @Composable fun MockComposeScope.chars(chars: Iterable<Char>) {
            repeat(of = chars) { c -> textOf(c) }
        }

        fun MockViewValidator.validatechars(chars: Iterable<Char>) {
            repeat(of = chars) { c -> textOf(c) }
        }

        val myComposer = compose {
            changed = invalidate
            chars(chars)
            chars(chars)
            chars(chars)
        }

        validate(myComposer.root) {
            validatechars(chars)
            validatechars(chars)
            validatechars(chars)
        }

        chars = listOf('a', 'b', 'x', 'c')
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) {
            validatechars(chars)
            validatechars(chars)
            validatechars(chars)
        }
    }

    @Test
    fun testSimpleMemoize() {
        val points = listOf(Point(1, 2), Point(2, 3))
        val myComposer = compose {
            points(points)
        }

        validate(myComposer.root) { points(points) }

        val changes = myComposer.recompose()
        assertFalse(changes)
    }

    @Test
    fun testMovingMemoization() {
        var points = listOf(
            Point(1, 2),
            Point(2, 3),
            Point(4, 5),
            Point(6, 7)
        )
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            points(points)
        }

        validate(myComposer.root) { points(points) }

        points = listOf(
            Point(1, 2),
            Point(4, 5),
            Point(2, 3),
            Point(6, 7)
        )
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) { points(points) }
    }

    @Test
    fun testComponent() {
        @Composable fun MockComposeScope.Reporter(report: Report? = null) {
            if (report != null) {
                text(report.from)
                text("reports to")
                text(report.to)
            } else {
                text("no report to report")
            }
        }

        @Composable fun MockComposeScope.reportsReport(reports: Iterable<Report>) {
            linear {
                repeat(of = reports) { report ->
                    Reporter(report)
                }
            }
        }

        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )
        val myComposer = compose {
            reportsReport(reports)
        }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
            }
        }

        myComposer.expectNoChanges()
    }

    @Test
    fun testComposeTwoAttributeComponent() {
        @Composable fun MockComposeScope.Two(first: Int = 1, second: Int = 2) {
            linear {
                text("$first $second")
            }
        }

        fun MockViewValidator.two(first: Int, second: Int) {
            linear {
                text("$first $second")
            }
        }

        val myComposer = compose {
            Two(41, 42)
        }

        validate(myComposer.root) {
            two(41, 42)
        }
    }

    @Test
    fun testComposeThreeAttributeComponent() {
        @Composable fun MockComposeScope.Three(first: Int = 1, second: Int = 2, third: Int = 3) {
            linear {
                text("$first $second $third")
            }
        }

        fun MockViewValidator.three(first: Int, second: Int, third: Int) {
            linear {
                text("$first $second $third")
            }
        }

        val myComposer = compose {
            Three(41, 42, 43)
        }

        validate(myComposer.root) {
            three(41, 42, 43)
        }
    }

    @Test
    fun testComposeFourOrMoreAttributeComponent() {
        @Composable fun MockComposeScope.Four(
            first: Int = 1,
            second: Int = 2,
            third: Int = 3,
            fourth: Int = 4
        ) {
            linear {
                text("$first $second $third $fourth")
            }
        }

        fun MockViewValidator.four(first: Int, second: Int, third: Int, fourth: Int) {
            linear {
                text("$first $second $third $fourth")
            }
        }

        val myComposer = compose {
            Four(41, 42, 43, 44)
        }

        validate(myComposer.root) {
            four(41, 42, 43, 44)
        }
    }

    @Test
    fun testSkippingACall() {

        @Composable fun MockComposeScope.show(value: Int) {
            linear {
                text("$value")
            }
            linear {
                text("value")
            }
        }

        fun MockViewValidator.show(value: Int) {
            linear {
                text("$value")
            }
            linear {
                text("value")
            }
        }

        @Composable fun MockComposeScope.test(showThree: Boolean) {
            show(1)
            show(2)
            if (showThree) {
                show(3)
            }
        }

        var showThree = false

        var recomposeTest: () -> Unit = { }

        @Composable fun MockComposeScope.Test() {
            recomposeTest = invalidate
            test(showThree)
        }

        fun MockViewValidator.test(showThree: Boolean) {
            show(1)
            show(2)
            if (showThree) {
                show(3)
            }
        }

        val composition: @Composable MockComposeScope.() -> Unit = {
            Test()
        }
        val validation: MockViewValidator.() -> Unit = {
            test(showThree)
        }

        val myComposer = compose(block = composition)
        validate(myComposer.root, block = validation)

        showThree = true
        recomposeTest()
        myComposer.expectChanges()
        validate(myComposer.root, block = validation)
    }

    @Test
    fun testComponentWithVarCtorParameter() {
        @Composable fun MockComposeScope.One(first: Int) {
            text("$first")
        }

        fun MockViewValidator.one(first: Int) {
            text("$first")
        }

        @Composable fun MockComposeScope.callOne(value: Int) {
            One(first = value)
        }

        var value = 42
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            callOne(value)
        }

        validate(myComposer.root) {
            one(42)
        }

        value = 43
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) {
            one(43)
        }
    }

    @Test
    fun testComponentWithValCtorParameter() {
        @Composable fun MockComposeScope.One(first: Int) {
            text("$first")
        }

        fun MockViewValidator.one(first: Int) {
            text("$first")
        }

        @Composable fun MockComposeScope.callOne(value: Int) {
            One(first = value)
        }

        var value = 42
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            callOne(value)
        }

        validate(myComposer.root) {
            one(42)
        }

        value = 43
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) {
            one(43)
        }

        changed!!()
        myComposer.expectNoChanges()
    }

    @Test
    fun testComposePartOfTree() {
        var recomposeLois: (() -> Unit)? = null

        @Composable fun MockComposeScope.Reporter(report: Report? = null) {
            if (report != null) {
                if (report.from == "Lois" || report.to == "Lois") recomposeLois = invalidate
                text(report.from)
                text("reports to")
                text(report.to)
            } else {
                text("no report to report")
            }
        }

        @Composable fun MockComposeScope.reportsReport(reports: Iterable<Report>) {
            linear {
                repeat(of = reports) { report ->
                    Reporter(report)
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois, r)
        val myComposer = compose {
            reportsReport(reports)
        }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        myComposer.expectNoChanges()

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Compose only the Lois report
        recomposeLois?.let { it() }

        myComposer.expectChanges()

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }
    }

    @Test
    fun testRecomposeWithReplace() {
        var recomposeLois: (() -> Unit)? = null
        var key = 0

        @Composable fun MockComposeScope.Reporter(report: Report? = null) {
            if (report != null) {
                if (report.from == "Lois" || report.to == "Lois") recomposeLois = invalidate
                key(key) {
                    text(report.from)
                    text("reports to")
                    text(report.to)
                }
            } else {
                text("no report to report")
            }
        }

        @Composable fun MockComposeScope.reportsReport(reports: Iterable<Report>) {
            linear {
                repeat(of = reports) { report ->
                    Reporter(report)
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois, r)
        val myComposer = compose {
            reportsReport(reports)
        }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        myComposer.expectNoChanges()

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Cause a new group to be generated in the component
        key = 2

        // Compose only the Lois report
        recomposeLois?.let { it() }

        myComposer.expectChanges()

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }
    }

    @Test
    fun testInvalidationAfterRemoval() {
        var recomposeLois = {}
        val key = 0

        @Composable fun MockComposeScope.Reporter(report: Report? = null) {
            if (report != null) {
                val callback = invalidate
                if (report.from == "Lois" || report.to == "Lois") recomposeLois = callback
                key(key) {
                    text(report.from)
                    text("reports to")
                    text(report.to)
                }
            } else {
                text("no report to report")
            }
        }

        @Composable fun MockComposeScope.reportsReport(
            reports: Iterable<Report>,
            include: (report: Report) -> Boolean
        ) {
            linear {
                repeat(of = reports) { report ->
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
        var changed = {}
        val myComposer = compose {
            changed = invalidate
            reportsReport(reports, filter)
        }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        filter = notLois
        changed()
        myComposer.expectChanges()

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
            }
        }

        // Invalidate Lois which is now removed.
        recomposeLois()
        myComposer.expectNoChanges()

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
            }
        }
    }

    // remember()

    @Test
    fun testSimpleRemember() {
        var count = 0
        var changed: (() -> Unit)? = null

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(value: Int) {
            changed = invalidate
            val w = remember { Wrapper(value) }
            text("value = ${w.value}")
        }

        fun MockViewValidator.test(value: Int) {
            text("value = $value")
        }

        val myComposer = compose {
            test(1)
        }

        validate(myComposer.root) { test(1) }

        assertEquals(1, count)

        changed!!()
        myComposer.expectNoChanges()

        // Expect the previous instance to be remembered
        assertEquals(1, count)
    }

    @Test
    fun testRememberOneParameter() {
        var count = 0

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(value: Int) {
            val w = remember(value) { Wrapper(value) }
            text("value = ${w.value}")
        }

        fun MockViewValidator.test(value: Int) {
            text("value = $value")
        }

        var value = 1
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            test(value)
        }

        validate(myComposer.root) { test(1) }

        value = 2
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) { test(2) }

        changed!!()
        myComposer.expectNoChanges()

        validate(myComposer.root) { test(2) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberTwoParameters() {
        var count = 0

        class Wrapper(val a: Int, val b: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(a: Int, b: Int) {
            val w = remember(a, b) { Wrapper(a, b) }
            text("a = ${w.a} b = ${w.b}")
        }

        fun MockViewValidator.test(a: Int, b: Int) {
            text("a = $a b = $b")
        }

        var p1 = 1
        var p2 = 2
        var changed: (() -> Unit)? = null

        val myComposer = compose {
            changed = invalidate
            test(p1, p2)
        }

        validate(myComposer.root) { test(1, 2) }

        p1 = 2
        p2 = 3
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) { test(2, 3) }

        changed!!()
        myComposer.expectNoChanges()

        validate(myComposer.root) { test(2, 3) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberThreeParameters() {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(a: Int, b: Int, c: Int) {
            val w = remember(a, b, c) { Wrapper(a, b, c) }
            text("a = ${w.a} b = ${w.b} c = ${w.c}")
        }

        fun MockViewValidator.test(a: Int, b: Int, c: Int) {
            text("a = $a b = $b c = $c")
        }

        var p3 = 3
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            test(1, 2, p3)
        }

        validate(myComposer.root) { test(1, 2, 3) }

        p3 = 4
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) { test(1, 2, 4) }

        changed!!()
        myComposer.expectNoChanges()

        validate(myComposer.root) { test(1, 2, 4) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberFourParameters() {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int, val d: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(a: Int, b: Int, c: Int, d: Int) {
            val w = remember(a, b, c, d) { Wrapper(a, b, c, d) }
            text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d}")
        }

        fun MockViewValidator.test(a: Int, b: Int, c: Int, d: Int) {
            text("a = $a b = $b c = $c d = $d")
        }

        var p3 = 3
        var p4 = 4
        var changed: (() -> Unit)? = null

        val myComposer = compose {
            changed = invalidate
            test(1, 2, p3, p4)
        }

        validate(myComposer.root) { test(1, 2, 3, 4) }

        p3 = 4
        p4 = 5
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) { test(1, 2, 4, 5) }

        changed!!()
        myComposer.expectNoChanges()

        validate(myComposer.root) { test(1, 2, 4, 5) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberFiveParameters() {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            val w = remember(a, b, c, d, e) { Wrapper(a, b, c, d, e) }
            text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d} e = ${w.e}")
        }

        fun MockViewValidator.test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            text("a = $a b = $b c = $c d = $d e = $e")
        }

        var lastParameter = 5
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            test(1, 2, 3, 4, lastParameter)
        }

        validate(myComposer.root) { test(1, 2, 3, 4, 5) }

        lastParameter = 6
        changed!!()

        myComposer.expectChanges()

        validate(myComposer.root) { test(1, 2, 3, 4, 6) }

        myComposer.expectNoChanges()

        validate(myComposer.root) { test(1, 2, 3, 4, 6) }

        assertEquals(2, count)
    }

    @Test
    fun testInsertGroupInContainer() {
        val values = mutableListOf(0)
        var changed: (() -> Unit)? = null

        @Composable fun MockComposeScope.composition() {
            linear {
                changed = invalidate
                for (value in values) {
                    memoize(value, value) {
                        text("$value")
                    }
                }
            }
        }

        fun MockViewValidator.composition() {
            linear {
                for (value in values)
                    text("$value")
            }
        }

        val myComposer = compose { composition() }

        validate(myComposer.root) { composition() }

        for (i in 1..10) {
            values.add(i)
            changed!!()
            myComposer.expectChanges()
            validate(myComposer.root) { composition() }
        }
    }

    // b/148273328
    @Test
    fun testInsertInGroups() {

        var threeVisible = false
        var changed: (() -> Unit)? = null

        @Composable fun MockComposeScope.composition() {
            linear {
                text("one")
                text("two")
                changed = invalidate
                if (threeVisible) {
                    text("three")
                    text("four")
                }
                linear {
                    text("five")
                }
            }
        }

        fun MockViewValidator.composition() {
            linear {
                text("one")
                text("two")
                if (threeVisible) {
                    text("three")
                    text("four")
                }
                linear {
                    text("five")
                }
            }
        }

        val myComposer = compose { composition() }
        validate(myComposer.root) { composition() }

        threeVisible = true
        changed!!()
        myComposer.expectChanges()

        validate(myComposer.root) { composition() }
    }

    @Test
    fun testStartJoin() {
        var text = "Starting"
        var myInvalidate: (() -> Unit)? = null
        @Composable fun MockComposeScope.composition() {
            linear {
                myInvalidate = invalidate
                text(text)
            }
        }

        fun MockViewValidator.composition() {
            linear {
                text(text)
            }
        }

        val myComposer = compose { composition() }

        validate(myComposer.root) { composition() }

        text = "Ending"
        myInvalidate?.let { it() }

        myComposer.expectChanges()

        validate(myComposer.root) { composition() }
    }

    @Test
    fun testInvalidateJoin_End() {
        var text = "Starting"
        var includeNested = true
        var invalidate1 = {}
        var invalidate2 = {}

        @Composable fun MockComposeScope.composition() {
            linear {
                invalidate1 = invalidate
                text(text)
                if (includeNested) {
                    invalidate2 = invalidate
                    text("Nested in $text")
                }
            }
        }

        fun MockViewValidator.composition() {
            linear {
                text(text)
                if (includeNested) {
                    text("Nested in $text")
                }
            }
        }

        val myComposer = compose { composition() }

        validate(myComposer.root) { composition() }

        text = "Ending"
        includeNested = false
        invalidate1()
        invalidate2()

        myComposer.expectChanges()

        validate(myComposer.root) { composition() }

        myComposer.expectNoChanges()

        validate(myComposer.root) { composition() }
    }

    @Test
    fun testInvalidateJoin_Start() {
        var text = "Starting"
        var includeNested = true
        var invalidate1: (() -> Unit)? = null
        var invalidate2: (() -> Unit)? = null

        @Composable fun MockComposeScope.composition() {
            linear {
                invalidate1 = invalidate
                if (includeNested) {
                    invalidate2 = invalidate
                    text("Nested in $text")
                }
                text(text)
            }
        }

        fun MockViewValidator.composition() {
            linear {
                if (includeNested) {
                    text("Nested in $text")
                }
                text(text)
            }
        }

        val myComposer = compose { composition() }

        validate(myComposer.root) { composition() }

        text = "Ending"
        includeNested = false
        invalidate1?.invoke()
        invalidate2?.invoke()

        myComposer.expectChanges()

        validate(myComposer.root) { composition() }

        myComposer.expectNoChanges()

        validate(myComposer.root) { composition() }
    }

    // b/132638679
    @Test
    fun testJoinInvalidate() {
        var texts = 5
        var invalidateOuter: (() -> Unit)? = null
        var invalidateInner: (() -> Unit)? = null

        @Composable fun MockComposeScope.composition() {
            linear {
                invalidateOuter = invalidate
                for (i in 1..texts) {
                    text("Some text")
                }

                Container {
                    text("Some text")

                    // Force the invalidation to survive the compose
                    val innerInvalidate = invalidate
                    innerInvalidate()
                    invalidateInner = innerInvalidate
                }
            }
        }

        val myComposer = compose { composition() }

        texts = 4
        invalidateOuter?.invoke()
        invalidateInner?.invoke()
        myComposer.expectChanges()

        texts = 3
        invalidateOuter?.invoke()
        myComposer.expectChanges()
    }

    @Test
    fun testLifecycle_Enter_Simple() {
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        @Composable fun MockComposeScope.composition() {
            linear {
                remember { lifecycleObject }
                text("Some text")
            }
        }

        fun MockViewValidator.composition() {
            linear {
                text("Some text")
            }
        }

        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            composition()
        }
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        changed!!()
        myComposer.expectNoChanges()
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")
    }

    @Test
    fun testLifecycle_Enter_SingleNotification() {
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        @Composable fun MockComposeScope.composition() {
            linear {
                val l = remember { lifecycleObject }
                assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                text("Some text")
            }
            linear {
                val l = remember { lifecycleObject }
                assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                text("Some other text")
            }
        }

        fun MockViewValidator.composition() {
            linear {
                text("Some text")
            }
            linear {
                text("Some other text")
            }
        }

        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            composition()
        }
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        changed!!()
        myComposer.expectNoChanges()
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")
    }

    @Test
    fun testLifecycle_Leave_Simple() {
        val lifecycleObject = object : CompositionLifecycleObserver {
            var count = 0
            override fun onEnter() {
                count++
            }

            override fun onLeave() {
                count--
            }
        }

        @Composable fun MockComposeScope.composition(includeLifecycleObject: Boolean) {
            linear {
                if (includeLifecycleObject) {
                    linear {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("Some text")
                    }
                }
            }
        }

        fun MockViewValidator.composition(includeLifecycleObject: Boolean) {
            linear {
                if (includeLifecycleObject) {
                    linear {
                        text("Some text")
                    }
                }
            }
        }

        var changed: (() -> Unit)? = null
        var value = true
        val myComposer = compose {
            changed = invalidate
            composition(value)
        }
        validate(myComposer.root) { composition(true) }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        changed!!()
        myComposer.expectNoChanges()
        validate(myComposer.root) { composition(true) }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")

        value = false
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) { composition(false) }

        assertEquals(0, lifecycleObject.count, "Object should have been notified of a leave")
    }

    @Test
    fun testLifecycle_Leave_NoLeaveOnReenter() {
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

        @Composable fun MockComposeScope.composition(a: Boolean, b: Boolean, c: Boolean) {
            linear {
                if (a) {
                    key(1) { linear {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("a")
                    } }
                }
                if (b) {
                    key(2) { linear {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("b")
                    } }
                }
                if (c) {
                    key(3) { linear {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("c")
                    } }
                }
            }
        }

        fun MockViewValidator.composition(a: Boolean, b: Boolean, c: Boolean) {
            linear {
                if (a) {
                    linear {
                        text("a")
                    }
                }
                if (b) {
                    linear {
                        text("b")
                    }
                }
                if (c) {
                    linear {
                        text("c")
                    }
                }
            }
        }

        expectedEnter = true
        expectedLeave = false

        var a = true
        var b = false
        var c = false
        var changed: (() -> Unit)? = null
        val myComposer = compose {
            changed = invalidate
            composition(a = a, b = b, c = c)
        }
        validate(myComposer.root) {
            composition(
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
        changed!!()
        myComposer.expectNoChanges()
        validate(myComposer.root) {
            composition(
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
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) {
            composition(
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
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) {
            composition(
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
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) {
            composition(
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
        changed!!()
        myComposer.expectChanges()
        validate(myComposer.root) {
            composition(
                a = false,
                b = false,
                c = false
            )
        }
        assertEquals(0, lifecycleObject.count, "A leave")
    }

    @Test
    fun testLifecycle_Leave_LeaveOnReplace() {
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

        @Composable fun MockComposeScope.composition(obj: Any) {
            linear {
                key(1) { linear {
                    remember(obj) { obj }
                    text("Some value")
                } }
            }
        }

        fun MockViewValidator.composition() {
            linear {
                linear {
                    text("Some value")
                }
            }
        }

        val myComposer = compose {
            changed = invalidate
            composition(obj = lifecycleObject)
        }
        validate(myComposer.root) { composition() }
        assertEquals(1, lifecycleObject1.count, "first object should enter")
        assertEquals(0, lifecycleObject2.count, "second object should not have entered")

        lifecycleObject = lifecycleObject2
        changed()
        myComposer.expectChanges()
        validate(myComposer.root) { composition() }
        assertEquals(0, lifecycleObject1.count, "first object should have left")
        assertEquals(1, lifecycleObject2.count, "second object should have entered")

        lifecycleObject = object {}
        changed()
        myComposer.expectChanges()
        validate(myComposer.root) { composition() }
        assertEquals(0, lifecycleObject1.count, "first object should have left")
        assertEquals(0, lifecycleObject2.count, "second object should have left")
    }

    @Test
    fun testLifecycle_EnterLeaveOrder() {
        var order = 0
        val objects = mutableListOf<Any>()
        val newLifecycleObject = { name: String ->
            object : CompositionLifecycleObserver, Counted,
                Ordered, Named {
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

        @Composable fun MockComposeScope.lifecycleUser(name: String) {
            linear {
                remember(name) { newLifecycleObject(name) }
                text(value = name)
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

        @Composable fun MockComposeScope.tree() {
            linear {
                lifecycleUser("A")
                linear {
                    lifecycleUser("B")
                    linear {
                        lifecycleUser("C")
                        lifecycleUser("D")
                    }
                    lifecycleUser("E")
                    lifecycleUser("F")
                    linear {
                        lifecycleUser("G")
                        lifecycleUser("H")
                        linear {
                            lifecycleUser("I")
                        }
                    }
                    lifecycleUser("J")
                }
            }
        }

        @Composable fun MockComposeScope.composition(includeTree: Boolean) {
            linear {
                if (includeTree) tree()
            }
        }

        var value = true
        var changed: (() -> Unit)? = null

        val myComposer = compose {
            changed = invalidate
            composition(value)
        }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered"
        )

        value = false
        changed!!()
        myComposer.expectChanges()

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
    fun testCompoundKeyHashStaysTheSameAfterRecompositions() {
        val outerKeys = mutableListOf<Int>()
        val innerKeys = mutableListOf<Int>()
        var previousOuterKeysSize = 0
        var previousInnerKeysSize = 0
        var outerInvalidate: (() -> Unit) = {}
        var innerInvalidate: (() -> Unit) = {}

        @Composable
        fun MockComposeScope.test() {
            outerInvalidate = invalidate
            outerKeys.add(composer.currentCompoundKeyHash)
            Container {
                innerInvalidate = invalidate
                innerKeys.add(composer.currentCompoundKeyHash)
            }
            // asserts that the key is correctly rolled back after start and end of Observe
            assertEquals(outerKeys.last(), composer.currentCompoundKeyHash)
        }

        val myComposer = compose {
            test()
        }

        assertNotEquals(previousOuterKeysSize, outerKeys.size)
        assertNotEquals(previousInnerKeysSize, innerKeys.size)

        previousOuterKeysSize = outerKeys.size
        outerInvalidate()
        myComposer.expectNoChanges()
        assertNotEquals(previousOuterKeysSize, outerKeys.size)

        previousInnerKeysSize = innerKeys.size
        innerInvalidate()
        myComposer.expectNoChanges()
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
    fun testSwappingGroups() {
        val items = mutableListOf(0, 1, 2, 3, 4)
        var invalidateComposition = {}

        @Composable
        fun MockComposeScope.noNodes() { }

        @Composable
        fun MockComposeScope.test() {
            invalidateComposition = invalidate
            for (item in items) {
                key(item) {
                    noNodes()
                }
            }
        }

        val myComposer = compose {
            test()
        }

        // Swap 2 and 3
        items[2] = 3
        items[3] = 2
        invalidateComposition()

        myComposer.expectChanges()
    }

    @Test // b/154650546
    fun testInsertOnMultipleLevels() {
        val items = mutableListOf(
            1 to mutableListOf(
                0, 1, 2, 3, 4),
            3 to mutableListOf(
                0, 1, 2, 3, 4)
        )

        val invalidates = mutableListOf<() -> Unit>()
        fun invalidateComposition() {
            invalidates.forEach { it() }
            invalidates.clear()
        }

        @Composable
        fun MockComposeScope.numbers(numbers: List<Int>) {
            linear {
                linear {
                    invalidates.add(invalidate)
                    for (number in numbers) {
                        text("$number")
                    }
                }
            }
        }

        @Composable
        fun MockComposeScope.item(number: Int, numbers: List<Int>) {
            linear {
                invalidates.add(invalidate)
                text("$number")
                numbers(numbers)
            }
        }

        @Composable
        fun MockComposeScope.test() {
            invalidates.add(invalidate)

            linear {
                invalidates.add(invalidate)
                for ((number, numbers) in items) {
                    item(number, numbers)
                }
            }
        }

        fun MockViewValidator.numbers(numbers: List<Int>) {
            linear {
                linear {
                    for (number in numbers) {
                        text("$number")
                    }
                }
            }
        }

        fun MockViewValidator.item(number: Int, numbers: List<Int>) {
            linear {
                text("$number")
                numbers(numbers)
            }
        }

        fun MockViewValidator.test() {
            linear {
                for ((number, numbers) in items) {
                    item(number, numbers)
                }
            }
        }

        val myComposition = compose {
            test()
        }

        fun validate() {
            validate(myComposition.root) {
                test()
            }
        }

        validate()

        // Add numbers to the list at 0 and 1
        items[0].second.add(2, 100)
        items[1].second.add(3, 200)

        // Add a list to the root.
        items.add(1, 2 to mutableListOf(0, 1, 2))

        invalidateComposition()

        myComposition.expectChanges()
        validate()
    }

    @Test
    fun testInsertingAfterSkipping() {
        val items = mutableListOf(
            1 to listOf(0, 1, 2, 3, 4)
        )

        val invalidates = mutableListOf<() -> Unit>()
        fun invalidateComposition() {
            invalidates.forEach { it() }
            invalidates.clear()
        }

        @Composable
        fun MockComposeScope.test() {
            invalidates.add(invalidate)

            linear {
                for ((item, numbers) in items) {
                    text(item.toString())
                    linear {
                        invalidates.add(invalidate)
                        for (number in numbers) {
                            text(number.toString())
                        }
                    }
                }
            }
        }

        fun MockViewValidator.test() {
            linear {
                for ((item, numbers) in items) {
                    text(item.toString())
                    linear {
                        for (number in numbers) {
                            text(number.toString())
                        }
                    }
                }
            }
        }

        val myComposition = compose {
            test()
        }

        validate(myComposition.root) {
            test()
        }

        items.add(2 to listOf(3, 4, 5, 6))
        invalidateComposition()

        myComposition.expectChanges()
        validate(myComposition.root) {
            test()
        }
    }

    @Test
    fun evenOddRecomposeGroup() {
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
        fun MockComposeScope.wrapper(children: @Composable () -> Unit) {
            children()
        }

        @Composable
        fun MockComposeScope.emitText() {
            invalidates.add(invalidate)
            if (includeOdd) {
                key(1) {
                    text("odd 1")
                }
            }
            if (includeEven) {
                key(2) {
                    text("even 2")
                }
            }
            if (includeOdd) {
                key(3) {
                    text("odd 3")
                }
            }
            if (includeEven) {
                key(4) {
                    text("even 4")
                }
            }
        }

        @Composable
        fun MockComposeScope.test() {
            linear {
                wrapper {
                    emitText()
                }
                emitText()
                wrapper {
                    emitText()
                }
                emitText()
            }
        }

        fun MockViewValidator.wrapper(children: () -> Unit) {
            children()
        }

        fun MockViewValidator.emitText() {
            if (includeOdd) {
                text("odd 1")
            }
            if (includeEven) {
                text("even 2")
            }
            if (includeOdd) {
                text("odd 3")
            }
            if (includeEven) {
                text("even 4")
            }
        }

        fun MockViewValidator.test() {
            linear {
                wrapper {
                    emitText()
                }
                emitText()
                wrapper {
                    emitText()
                }
                emitText()
            }
        }

        val myComposition = compose {
            test()
        }

        fun validate() {
            validate(myComposition.root) {
                test()
            }
        }
        validate()

        includeEven = false
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        includeEven = true
        includeOdd = false
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        includeEven = false
        includeOdd = false
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        includeEven = true
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        includeOdd = true
        invalidateComposition()
        myComposition.expectChanges()
        validate()
    }

    @Test
    fun evenOddWithMovement() {
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
        fun MockComposeScope.wrapper(children: @Composable () -> Unit) {
            children()
        }

        @Composable
        fun MockComposeScope.emitText(all: Boolean) {
            invalidates.add(invalidate)
            for (i in order) {
                if (i % 2 == 1 && (all || includeOdd)) {
                    key(i) {
                        text("odd $i")
                    }
                }
                if (i % 2 == 0 && (all || includeEven)) {
                    key(i) {
                        text("even $i")
                    }
                }
            }
        }

        @Composable
        fun MockComposeScope.test() {
            linear {
                invalidates.add(invalidate)
                for (i in order) {
                    key(i) {
                        text("group $i")
                        if (i == 2 || (includeEven && includeOdd)) {
                            text("including everything")
                        } else {
                            if (includeEven) {
                                text("including evens")
                            }
                            if (includeOdd) {
                                text("including odds")
                            }
                        }
                        emitText(i == 2)
                    }
                }
                emitText(false)
            }
        }

        fun MockViewValidator.emitText(all: Boolean) {
            for (i in order) {
                if (i % 2 == 1 && (includeOdd || all)) {
                    text("odd $i")
                }
                if (i % 2 == 0 && (includeEven || all)) {
                    text("even $i")
                }
            }
        }

        fun MockViewValidator.test() {
            linear {
                for (i in order) {
                    text("group $i")
                    if (i == 2 || (includeEven && includeOdd)) {
                        text("including everything")
                    } else {
                        if (includeEven) {
                            text("including evens")
                        }
                        if (includeOdd) {
                            text("including odds")
                        }
                    }
                    emitText(i == 2)
                }
                emitText(false)
            }
        }

        val myComposition = compose {
            test()
        }

        fun validate() {
            validate(myComposition.root) {
                test()
            }
        }
        validate()

        order = listOf(1, 2, 4, 3)
        includeEven = false
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        order = listOf(1, 4, 2, 3)
        includeEven = true
        includeOdd = false
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        order = listOf(3, 4, 2, 1)
        includeEven = false
        includeOdd = false
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        order = listOf(4, 3, 2, 1)
        includeEven = true
        invalidateComposition()
        myComposition.expectChanges()
        validate()

        order = listOf(1, 2, 3, 4)
        includeOdd = true
        invalidateComposition()
        myComposition.expectChanges()
        validate()
    }
}

private fun <T> assertArrayEquals(message: String, expected: Array<T>, received: Array<T>) {
    fun Array<T>.getString() = this.joinToString(", ") { it.toString() }
    fun err(msg: String): Nothing = error("$message: $msg, expected: [${
    expected.getString()}], received: [${received.getString()}]")
    if (expected.size != received.size) err("sizes are different")
    expected.indices.forEach { index ->
        if (expected[index] != received[index])
            err("item at index $index was different (expected [${
            expected[index]}], received: [${received[index]}]")
    }
}

private fun compose(
    block: @Composable MockComposeScope.() -> Unit
): MockViewComposer {
    val composer = run {
        val root = View().apply { name = "root" }

        val scope = CoroutineScope(Job())
        val clock = object : CompositionFrameClock {
            override suspend fun <R> awaitFrameNanos(onFrame: (Long) -> R): R {
                // The original version of this test used a mock Recomposer
                // that never successfully scheduled a frame.
                suspendCancellableCoroutine<Unit> {}
                return onFrame(0)
            }
        }
        val recomposer = Recomposer().apply {
            scope.launch { runRecomposeAndApplyChanges(clock) }
        }
        MockViewComposer(root, recomposer)
    }

    composer.compose {
        block()
    }
    composer.applyChanges()
    composer.slotTable.verifyWellFormed()

    return composer
}

private fun MockViewComposer.expectNoChanges() {
    val changes = recompose() && changeCount > 0
    assertFalse(changes)
}

private fun MockViewComposer.expectChanges() {
    val changes = recompose() && changeCount > 0
    assertTrue(changes, "Expected changes")
    applyChanges()
    slotTable.verifyWellFormed()
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
