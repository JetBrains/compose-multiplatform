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
import androidx.ui.core.clearRoots
import org.junit.After
import org.junit.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CompositionTests {

    @After
    fun teardown() {
        clearRoots()
    }

    @Test
    fun testComposeAModel() {
        val model = testModel()
        val myComposer = compose(model)

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
        val myComposer = compose(model)

        compose(model, myComposer, expectChanges = false)

        validate(myComposer.root) {
            selectContact(model)
        }
    }

    @Test
    fun testInsertAContact() {
        val model =
            testModel(mutableListOf(bob, jon))
        val myComposer = compose(model)

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
        compose(model, myComposer)

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
        val myComposer = compose(model)

        model.move(steve, after = jon)
        compose(model, myComposer)

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
        val myComposer = compose(model)

        model.filter = "Jon"
        compose(model, myComposer)

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
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )
        val myComposer = compose {
            reportsReport(reports)
        }

        val newReports = listOf(
            jim_reports_to_sally,
            clark_reports_to_lois,
            rob_reports_to_alice
        )
        compose(myComposer) {
            reportsReport(newReports)
        }

        validate(myComposer.root) {
            reportsReport(newReports)
        }
    }

    @Test
    fun testReplace() {
        var includeA = true
        @Composable fun MockComposeScope.composition() {
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
        compose(myComposer) {
            composition()
        }
        validate(myComposer.root) {
            composition()
        }
        includeA = true
        compose(myComposer) {
            composition()
        }
        validate(myComposer.root) {
            composition()
        }
    }

    @Test
    fun testInsertWithMultipleRoots() {
        val chars = listOf('a', 'b', 'c')

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
            chars(chars)
            chars(chars)
            chars(chars)
        }.apply { applyChanges() }

        validate(myComposer.root) {
            validatechars(chars)
            validatechars(chars)
            validatechars(chars)
        }

        val newChars = listOf('a', 'b', 'x', 'c')

        compose(myComposer) {
            chars(newChars)
            chars(newChars)
            chars(newChars)
        }.apply { applyChanges() }

        validate(myComposer.root) {
            validatechars(newChars)
            validatechars(newChars)
            validatechars(newChars)
        }
    }

    @Ignore("b/148896187")
    @Test
    fun testSimpleMemoize() {
        val points = listOf(Point(1, 2), Point(2, 3))
        val myComposer = compose {
            points(points)
        }.apply { applyChanges() }

        validate(myComposer.root) { points(points) }

        compose(myComposer, expectChanges = false) {
            points(points)
        }
    }

    @Test
    fun testMovingMemoization() {
        val points = listOf(
            Point(1, 2),
            Point(2, 3),
            Point(4, 5),
            Point(6, 7)
        )
        val myComposer = compose {
            points(points)
        }

        validate(myComposer.root) { points(points) }

        val modifiedPoints = listOf(
            Point(1, 2),
            Point(4, 5),
            Point(2, 3),
            Point(6, 7)
        )
        compose(myComposer) {
            points(modifiedPoints)
        }

        validate(myComposer.root) { points(modifiedPoints) }
    }

    @Ignore("b/148896187")
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

        compose(myComposer, expectChanges = false) {
            reportsReport(reports)
        }
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
        myComposer.recompose()
        myComposer.applyChanges()
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
        val myComposer = compose {
            callOne(value)
        }

        validate(myComposer.root) {
            one(42)
        }

        value = 43

        compose(myComposer) {
            callOne(value)
        }

        validate(myComposer.root) {
            one(43)
        }
    }

    @Ignore("b/148896187")
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
        val myComposer = compose {
            callOne(value)
        }

        validate(myComposer.root) {
            one(42)
        }

        value = 43

        compose(myComposer) {
            callOne(value)
        }

        validate(myComposer.root) {
            one(43)
        }

        compose(myComposer, expectChanges = false) {
            callOne(value)
        }
    }

    @Ignore("b/148896187")
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
        }.apply { applyChanges() }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        compose(myComposer, expectChanges = false) {
            reportsReport(reports)
        }

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Compose only the Lois report
        recomposeLois?.let { it() }

        myComposer.recompose()
        myComposer.applyChanges()

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }
    }

    @Ignore("b/148896187")
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
        }.apply { applyChanges() }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        compose(myComposer, expectChanges = false) {
            reportsReport(reports)
        }

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Cause a new group to be generated in the component
        key = 2

        // Compose only the Lois report
        recomposeLois?.let { it() }

        myComposer.recompose()
        myComposer.applyChanges()

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
        var recomposeLois: (() -> Unit)? = null
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
        val myComposer = compose {
            reportsReport(reports, all)
        }.apply { applyChanges() }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        compose(myComposer, expectChanges = true) {
            reportsReport(reports, notLois)
        }

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
            }
        }

        // Invalidate Lois which is now removed.
        recomposeLois?.let { it() }

        myComposer.recompose()
        myComposer.applyChanges()

        validate(myComposer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
            }
        }
    }

    // remember()

    @Ignore("b/148896187")
    @Test
    fun testSimpleRemember() {
        var count = 0

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable fun MockComposeScope.test(value: Int) {
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

        compose(myComposer, expectChanges = false) {
            test(1)
        }

        // Expect the previous instance to be remembered
        assertEquals(1, count)
    }

    @Ignore("b/148896187")
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

        val myComposer = compose {
            test(1)
        }

        validate(myComposer.root) { test(1) }

        compose(myComposer) {
            test(2)
        }

        validate(myComposer.root) { test(2) }

        compose(myComposer, expectChanges = false) {
            test(2)
        }

        validate(myComposer.root) { test(2) }

        assertEquals(2, count)
    }

    @Ignore("b/148896187")
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

        val myComposer = compose {
            test(1, 2)
        }

        validate(myComposer.root) { test(1, 2) }

        compose(myComposer) {
            test(2, 3)
        }

        validate(myComposer.root) { test(2, 3) }

        compose(myComposer, expectChanges = false) {
            test(2, 3)
        }

        validate(myComposer.root) { test(2, 3) }

        assertEquals(2, count)
    }

    @Ignore("b/148896187")
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

        val myComposer = compose {
            test(1, 2, 3)
        }

        validate(myComposer.root) { test(1, 2, 3) }

        compose(myComposer) {
            test(1, 2, 4)
        }

        validate(myComposer.root) { test(1, 2, 4) }

        compose(myComposer, expectChanges = false) {
            test(1, 2, 4)
        }

        validate(myComposer.root) { test(1, 2, 4) }

        assertEquals(2, count)
    }

    @Ignore("b/148896187")
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

        val myComposer = compose {
            test(1, 2, 3, 4)
        }

        validate(myComposer.root) { test(1, 2, 3, 4) }

        compose(myComposer) {
            test(1, 2, 4, 5)
        }

        validate(myComposer.root) { test(1, 2, 4, 5) }

        compose(myComposer, expectChanges = false) {
            test(1, 2, 4, 5)
        }

        validate(myComposer.root) { test(1, 2, 4, 5) }

        assertEquals(2, count)
    }

    @Ignore("b/148896187")
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

        val myComposer = compose {
            test(1, 2, 3, 4, 5)
        }

        validate(myComposer.root) { test(1, 2, 3, 4, 5) }

        compose(myComposer) {
            test(1, 2, 4, 5, 6)
        }

        validate(myComposer.root) { test(1, 2, 4, 5, 6) }

        compose(myComposer, expectChanges = false) {
            test(1, 2, 4, 5, 6)
        }

        validate(myComposer.root) { test(1, 2, 4, 5, 6) }

        assertEquals(2, count)
    }

    @Test
    fun testInsertGroupInContainer() {
        val values = mutableListOf(0)

        @Composable fun MockComposeScope.composition() {
            linear {
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
            compose(myComposer) { composition() }
            validate(myComposer.root) { composition() }
        }
    }

    // b/148273328
    @Test
    fun testInsertInGroups() {

        var threeVisible = false

        @Composable fun MockComposeScope.composition() {
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

        compose(myComposer) { composition() }

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

        myComposer.recompose()
        myComposer.applyChanges()

        validate(myComposer.root) { composition() }
    }

    @Test
    fun testInvalidateJoin_End() {
        var text = "Starting"
        var includeNested = true
        var invalidate1: (() -> Unit)? = null
        var invalidate2: (() -> Unit)? = null

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
        invalidate1?.invoke()
        invalidate2?.invoke()

        myComposer.recompose()
        myComposer.applyChanges()

        validate(myComposer.root) { composition() }

        myComposer.recompose()
        myComposer.applyChanges()

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

        myComposer.recompose()
        myComposer.applyChanges()

        validate(myComposer.root) { composition() }

        myComposer.recompose()
        myComposer.applyChanges()

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

                Observe {
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
        myComposer.recompose()
        myComposer.applyChanges()

        texts = 3
        invalidateOuter?.invoke()
        myComposer.recompose()
        myComposer.applyChanges()
    }

    @Ignore("b/148896187")
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

        val myComposer = compose { composition() }
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        compose(myComposer, expectChanges = false) {
            composition()
        }
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")
    }

    @Ignore("b/148896187")
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

        val myComposer = compose { composition() }
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        compose(myComposer, expectChanges = false) {
            composition()
        }
        validate(myComposer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")
    }

    @Ignore("b/148896187")
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

        val myComposer = compose { composition(true) }
        validate(myComposer.root) { composition(true) }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        compose(myComposer, expectChanges = false) {
            composition(true)
        }
        validate(myComposer.root) { composition(true) }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")

        compose(myComposer, expectChanges = true) {
            composition(false)
        }
        validate(myComposer.root) { composition(false) }

        assertEquals(0, lifecycleObject.count, "Object should have been notified of a leave")
    }

    @Ignore("b/148896187")
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
        val myComposer = compose { composition(a = true, b = false, c = false) }
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
        compose(myComposer, expectChanges = false) {
            composition(a = true, b = false, c = false)
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
            "Object should have only been notified once"
        )

        expectedEnter = false
        expectedLeave = false
        compose(myComposer, expectChanges = true) {
            composition(a = false, b = true, c = false)
        }
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
        compose(myComposer, expectChanges = true) {
            composition(a = false, b = false, c = true)
        }
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
        compose(myComposer, expectChanges = true) {
            composition(a = true, b = false, c = false)
        }
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
        compose(myComposer, expectChanges = true) {
            composition(a = false, b = false, c = false)
        }
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

        val myComposer = compose { composition(obj = lifecycleObject1) }
        validate(myComposer.root) { composition() }
        assertEquals(1, lifecycleObject1.count, "first object should enter")
        assertEquals(0, lifecycleObject2.count, "second object should not have entered")

        compose(myComposer, expectChanges = true) {
            composition(lifecycleObject2)
        }
        validate(myComposer.root) { composition() }
        assertEquals(0, lifecycleObject1.count, "first object should have left")
        assertEquals(1, lifecycleObject2.count, "second object should have entered")

        compose(myComposer, expectChanges = true) {
            composition(object {})
        }
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

        val myComposer = compose { composition(true) }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered"
        )

        compose(myComposer) {
            composition(false)
        }

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
            Observe {
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
        myComposer.recompose()
        myComposer.applyChanges()
        assertNotEquals(previousOuterKeysSize, outerKeys.size)

        previousInnerKeysSize = innerKeys.size
        innerInvalidate()
        myComposer.recompose()
        myComposer.applyChanges()
        assertNotEquals(previousInnerKeysSize, innerKeys.size)

        assertNotEquals(innerKeys[0], outerKeys[0])
        innerKeys.forEach {
            assertEquals(innerKeys[0], it)
        }
        outerKeys.forEach {
            assertEquals(outerKeys[0], it)
        }
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
    myComposer: MockViewComposer? = null,
    expectChanges: Boolean = true,
    block: @Composable MockComposeScope.() -> Unit
): MockViewComposer {
    val myRealComposer = myComposer ?: run {
        val root = View().apply { name = "root" }
        MockViewComposer(root)
    }

    myRealComposer.compose {
        block()
    }

    if (expectChanges) {
        assertNotEquals(0, myRealComposer.changeCount, "changes were expected")
        myRealComposer.applyChanges()
    } else {
        assertEquals(0, myRealComposer.changeCount, "no changes were expected")
    }

    return myRealComposer
}

private fun compose(
    model: ContactModel,
    myComposer: MockViewComposer? = null,
    expectChanges: Boolean = true
): MockViewComposer =
    compose(myComposer = myComposer, expectChanges = expectChanges) {
        selectContact(model)
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
