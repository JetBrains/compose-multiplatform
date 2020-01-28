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
@file:Suppress("PLUGIN_ERROR")
package androidx.compose

import androidx.compose.mock.Compose
import androidx.compose.mock.Contact
import androidx.compose.mock.ContactModel
import androidx.compose.mock.MockViewComposer
import androidx.compose.mock.MockViewComposition
import androidx.compose.mock.MockViewValidator
import androidx.compose.mock.Point
import androidx.compose.mock.Report
import androidx.compose.mock.View
import androidx.compose.mock.ViewComponent
import androidx.compose.mock.call
import androidx.compose.mock.contact
import androidx.compose.mock.edit
import androidx.compose.mock.join
import androidx.compose.mock.linear
import androidx.compose.mock.memoize
import androidx.compose.mock.points
import androidx.compose.mock.repeat
import androidx.compose.mock.reportsReport
import androidx.compose.mock.reportsTo
import androidx.compose.mock.selectContact
import androidx.compose.mock.set
import androidx.compose.mock.skip
import androidx.compose.mock.text
import androidx.compose.mock.update
import androidx.compose.mock.validate
import org.junit.After
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CompositionTests {

    @After
    fun teardown() {
        androidx.compose.Compose.clearRoots()
    }

    @Test
    fun testComposeAModel() {
        val model = testModel()
        val composer = compose(model)

        validate(composer.root) {
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
        val composer = compose(model)

        compose(model, composer, expectChanges = false)

        validate(composer.root) {
            selectContact(model)
        }
    }

    @Test
    fun testInsertAContact() {
        val model =
            testModel(mutableListOf(bob, jon))
        val composer = compose(model)

        validate(composer.root) {
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
        compose(model, composer)

        validate(composer.root) {
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
        val composer = compose(model)

        model.move(steve, after = jon)
        compose(model, composer)

        validate(composer.root) {
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
        val composer = compose(model)

        model.filter = "Jon"
        compose(model, composer)

        validate(composer.root) {
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

        val composer = compose {
            reportsReport(reports)
        }

        validate(composer.root) {
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
        val composer = compose {
            reportsReport(reports)
        }

        val newReports = listOf(
            jim_reports_to_sally,
            clark_reports_to_lois,
            rob_reports_to_alice
        )
        compose(composer) {
            reportsReport(newReports)
        }

        validate(composer.root) {
            reportsReport(newReports)
        }
    }

    @Test
    fun testReplace() {
        var includeA = true
        fun MockViewComposition.composition() {
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
        val composer = compose {
            composition()
        }
        validate(composer.root) {
            composition()
        }
        includeA = false
        compose(composer) {
            composition()
        }
        validate(composer.root) {
            composition()
        }
        includeA = true
        compose(composer) {
            composition()
        }
        validate(composer.root) {
            composition()
        }
    }

    @Test
    fun testInsertWithMultipleRoots() {
        val chars = listOf('a', 'b', 'c')

        fun MockViewComposition.textOf(c: Char) {
            text(c.toString())
        }

        fun MockViewValidator.textOf(c: Char) {
            text(c.toString())
        }

        fun MockViewComposition.chars(chars: Iterable<Char>) {
            repeat(of = chars) { c -> textOf(c) }
        }

        fun MockViewValidator.chars(chars: Iterable<Char>) {
            repeat(of = chars) { c -> textOf(c) }
        }

        val composer = compose {
            chars(chars)
            chars(chars)
            chars(chars)
        }.apply { applyChanges() }

        validate(composer.root) {
            chars(chars)
            chars(chars)
            chars(chars)
        }

        val newChars = listOf('a', 'b', 'x', 'c')

        compose(composer) {
            chars(newChars)
            chars(newChars)
            chars(newChars)
        }.apply { applyChanges() }

        validate(composer.root) {
            chars(newChars)
            chars(newChars)
            chars(newChars)
        }
    }

    @Test
    fun testSimpleMemoize() {
        val points = listOf(Point(1, 2), Point(2, 3))
        val composer = compose {
            points(points)
        }.apply { applyChanges() }

        validate(composer.root) { points(points) }

        compose(composer, expectChanges = false) {
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
        val composer = compose {
            points(points)
        }

        validate(composer.root) { points(points) }

        val modifiedPoints = listOf(
            Point(1, 2),
            Point(4, 5),
            Point(2, 3),
            Point(6, 7)
        )
        compose(composer) {
            points(modifiedPoints)
        }

        validate(composer.root) { points(modifiedPoints) }
    }

    @Test
    fun testComponent() {
        val slReportReports = object {}

        class Reporter : ViewComponent() {
            var report: Report? = null

            override fun compose() {
                val r = report
                if (r != null) {
                    text(r.from)
                    text("reports to")
                    text(r.to)
                } else {
                    text("no report to report")
                }
            }
        }

        fun MockViewComposition.reportsReport(reports: Iterable<Report>) {
            linear {
                repeat(of = reports) { report ->
                    call(
                        slReportReports,
                        { Reporter() },
                        { set(report) { this.report = it } },
                        { it() }
                    )
                }
            }
        }

        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois
        )
        val composer = compose {
            reportsReport(reports)
        }

        validate(composer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
            }
        }

        compose(composer, expectChanges = false) {
            reportsReport(reports)
        }
    }

    @Test
    fun testComposeTwoAttributeComponent() {
        class Two : ViewComponent() {
            var first: Int = 1
            var second: Int = 2

            override fun compose() {
                linear {
                    text("$first $second")
                }
            }
        }

        fun MockViewValidator.two(first: Int, second: Int) {
            linear {
                text("$first $second")
            }
        }

        val two = object {}
        val composer = compose {
            call(
                two,
                { Two() },
                {
                    set(41) { this.first = it }
                    set(42) { this.second = it }
                },
                { it() }
            )
        }

        validate(composer.root) {
            two(41, 42)
        }
    }

    @Test
    fun testComposeThreeAttributeComponent() {
        class Three : ViewComponent() {
            var first: Int = 1
            var second: Int = 2
            var third: Int = 3

            override fun compose() {
                linear {
                    text("$first $second $third")
                }
            }
        }

        fun MockViewValidator.three(first: Int, second: Int, third: Int) {
            linear {
                text("$first $second $third")
            }
        }

        val three = object {}
        val composer = compose {
            call(
                three,
                { Three() },
                {
                    set(41) { this.first = it }
                    set(42) { this.second = it }
                    set(43) { this.third = it }
                },
                { it() }
            )
        }

        validate(composer.root) {
            three(41, 42, 43)
        }
    }

    @Test
    fun testComposeFourOrMoreAttributeComponent() {
        class Four : ViewComponent() {
            var first: Int = 1
            var second: Int = 2
            var third: Int = 3
            var fourth: Int = 4

            override fun compose() {
                linear {
                    text("$first $second $third $fourth")
                }
            }
        }

        fun MockViewValidator.four(first: Int, second: Int, third: Int, fourth: Int) {
            linear {
                text("$first $second $third $fourth")
            }
        }

        val four = object {}
        val composer = compose {
            call(
                four,
                { Four() },
                {
                    set(41) { this.first = it }
                    set(42) { this.second = it }
                    set(43) { this.third = it }
                    set(44) { this.fourth = it }
                },
                { it() }
            )
        }

        validate(composer.root) {
            four(41, 42, 43, 44)
        }
    }

    @Test
    fun testSkippingACall() {

        fun MockViewComposition.show(value: Int) {
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

        fun MockViewComposition.test(showThree: Boolean) {
            call(537, { false }) {
                show(1)
            }
            call(540, { false }) {
                show(2)
            }
            if (showThree) {
                call(544, { false }) {
                    show(3)
                }
            }
        }

        var showThree = false

        var recomposeTest: () -> Unit = { }

        class Test : ViewComponent() {
            override fun compose() {
                recomposeTest = { recompose() }
                test(showThree)
            }
        }

        fun MockViewValidator.test(showThree: Boolean) {
            show(1)
            show(2)
            if (showThree) {
                show(3)
            }
        }

        val composition: MockViewComposition.() -> Unit = {
            call(579, { Test() }, { }, {
                it()
            })
        }
        val validation: MockViewValidator.() -> Unit = {
            test(showThree)
        }

        val composer = compose(block = composition)
        validate(composer.root, block = validation)

        showThree = true
        recomposeTest()
        composer.recomposeWithCurrent()
        composer.applyChanges()
        validate(composer.root, block = validation)
    }

    @Test
    fun testComponentWithVarCtorParameter() {
        class One(var first: Int) : ViewComponent() {
            override fun compose() {
                text("$first")
            }
        }

        fun MockViewValidator.one(first: Int) {
            text("$first")
        }

        val key = object {}
        fun MockViewComposition.callOne(value: Int) {
            call(
                key,
                { One(first = value) },
                {
                    update(value) { this.first = it }
                },
                { it() }
            )
        }

        var value = 42
        val composer = compose {
            callOne(value)
        }

        validate(composer.root) {
            one(42)
        }

        value = 43

        compose(composer) {
            callOne(value)
        }

        validate(composer.root) {
            one(43)
        }
    }

    @Test
    fun testComponentWithValCtorParameter() {
        class One(val first: Int) : ViewComponent() {
            override fun compose() {
                text("$first")
            }
        }

        fun MockViewValidator.one(first: Int) {
            text("$first")
        }

        val key = object {}
        fun MockViewComposition.callOne(value: Int) {
            call(
                cc.joinKey(key, value),
                { One(first = value) },
                { },
                { it() }
            )
        }

        var value = 42
        val composer = compose {
            callOne(value)
        }

        validate(composer.root) {
            one(42)
        }

        value = 43

        compose(composer) {
            callOne(value)
        }

        validate(composer.root) {
            one(43)
        }

        compose(composer, expectChanges = false) {
            callOne(value)
        }
    }

    @Test
    fun testComposePartOfTree() {
        val slReportReports = object {}
        var recomposeLois: (() -> Unit)? = null

        class Reporter : ViewComponent() {
            var report: Report? = null

            override fun compose() {
                val r = report
                if (r != null) {
                    if (r.from == "Lois" || r.to == "Lois") recomposeLois = { recompose() }
                    text(r.from)
                    text("reports to")
                    text(r.to)
                } else {
                    text("no report to report")
                }
            }
        }

        fun MockViewComposition.reportsReport(reports: Iterable<Report>) {
            linear {
                repeat(of = reports) { report ->
                    call(
                        slReportReports,
                        { Reporter() },
                        { set(report) { this.report = it } },
                        { it() }
                    )
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois, r)
        val composer = compose {
            reportsReport(reports)
        }.apply { applyChanges() }

        validate(composer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        compose(composer, expectChanges = false) {
            reportsReport(reports)
        }

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Compose only the Lois report
        recomposeLois?.let { it() }

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) {
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
        val slReportReports = object {}
        var recomposeLois: (() -> Unit)? = null
        var key = 0

        class Reporter : ViewComponent() {
            var report: Report? = null

            override fun compose() {
                val r = report
                if (r != null) {
                    if (r.from == "Lois" || r.to == "Lois") recomposeLois = { recompose() }
                    cc.startGroup(key)
                    text(r.from)
                    text("reports to")
                    text(r.to)
                    cc.endGroup()
                } else {
                    text("no report to report")
                }
            }
        }

        fun MockViewComposition.reportsReport(reports: Iterable<Report>) {
            linear {
                repeat(of = reports) { report ->
                    call(
                        slReportReports,
                        { Reporter() },
                        { set(report) { this.report = it } },
                        { it() }
                    )
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(
            jim_reports_to_sally,
            rob_reports_to_alice,
            clark_reports_to_lois, r)
        val composer = compose {
            reportsReport(reports)
        }.apply { applyChanges() }

        validate(composer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        compose(composer, expectChanges = false) {
            reportsReport(reports)
        }

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Cause a new group to be generated in the component
        key = 2

        // Compose only the Lois report
        recomposeLois?.let { it() }

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) {
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
        val slReportReports = object {}
        var recomposeLois: (() -> Unit)? = null
        val key = 0

        class Reporter : ViewComponent() {
            var report: Report? = null

            override fun compose() {
                val r = report
                if (r != null) {
                    if (r.from == "Lois" || r.to == "Lois") recomposeLois = { recompose() }
                    cc.startGroup(key)
                    text(r.from)
                    text("reports to")
                    text(r.to)
                    cc.endGroup()
                } else {
                    text("no report to report")
                }
            }
        }

        fun MockViewComposition.reportsReport(
            reports: Iterable<Report>,
            include: (report: Report) -> Boolean
        ) {
            linear {
                repeat(of = reports) { report ->
                    if (include(report)) {
                        call(
                            slReportReports,
                            { Reporter() },
                            { set(report) { this.report = it } },
                            { it() }
                        )
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
        val composer = compose {
            reportsReport(reports, all)
        }.apply { applyChanges() }

        validate(composer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
                reportsTo(clark_reports_to_lois)
                reportsTo(r)
            }
        }

        compose(composer, expectChanges = true) {
            reportsReport(reports, notLois)
        }

        validate(composer.root) {
            linear {
                reportsTo(jim_reports_to_sally)
                reportsTo(rob_reports_to_alice)
            }
        }

        // Invalidate Lois which is now removed.
        recomposeLois?.let { it() }

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) {
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

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        fun MockViewComposition.test(value: Int) {
            val w = remember { Wrapper(value) }
            text("value = ${w.value}")
        }

        fun MockViewValidator.test(value: Int) {
            text("value = $value")
        }

        val composer = compose {
            test(1)
        }

        validate(composer.root) { test(1) }

        assertEquals(1, count)

        compose(composer, expectChanges = false) {
            test(1)
        }

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

        fun MockViewComposition.test(value: Int) {
            val w = remember(value) { Wrapper(value) }
            text("value = ${w.value}")
        }

        fun MockViewValidator.test(value: Int) {
            text("value = $value")
        }

        val composer = compose {
            test(1)
        }

        validate(composer.root) { test(1) }

        compose(composer) {
            test(2)
        }

        validate(composer.root) { test(2) }

        compose(composer, expectChanges = false) {
            test(2)
        }

        validate(composer.root) { test(2) }

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

        fun MockViewComposition.test(a: Int, b: Int) {
            val w = remember(a, b) { Wrapper(a, b) }
            text("a = ${w.a} b = ${w.b}")
        }

        fun MockViewValidator.test(a: Int, b: Int) {
            text("a = $a b = $b")
        }

        val composer = compose {
            test(1, 2)
        }

        validate(composer.root) { test(1, 2) }

        compose(composer) {
            test(2, 3)
        }

        validate(composer.root) { test(2, 3) }

        compose(composer, expectChanges = false) {
            test(2, 3)
        }

        validate(composer.root) { test(2, 3) }

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

        fun MockViewComposition.test(a: Int, b: Int, c: Int) {
            val w = remember(a, b, c) { Wrapper(a, b, c) }
            text("a = ${w.a} b = ${w.b} c = ${w.c}")
        }

        fun MockViewValidator.test(a: Int, b: Int, c: Int) {
            text("a = $a b = $b c = $c")
        }

        val composer = compose {
            test(1, 2, 3)
        }

        validate(composer.root) { test(1, 2, 3) }

        compose(composer) {
            test(1, 2, 4)
        }

        validate(composer.root) { test(1, 2, 4) }

        compose(composer, expectChanges = false) {
            test(1, 2, 4)
        }

        validate(composer.root) { test(1, 2, 4) }

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

        fun MockViewComposition.test(a: Int, b: Int, c: Int, d: Int) {
            val w = remember(a, b, c, d) { Wrapper(a, b, c, d) }
            text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d}")
        }

        fun MockViewValidator.test(a: Int, b: Int, c: Int, d: Int) {
            text("a = $a b = $b c = $c d = $d")
        }

        val composer = compose {
            test(1, 2, 3, 4)
        }

        validate(composer.root) { test(1, 2, 3, 4) }

        compose(composer) {
            test(1, 2, 4, 5)
        }

        validate(composer.root) { test(1, 2, 4, 5) }

        compose(composer, expectChanges = false) {
            test(1, 2, 4, 5)
        }

        validate(composer.root) { test(1, 2, 4, 5) }

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

        fun MockViewComposition.test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            val w = remember(a, b, c, d, e) { Wrapper(a, b, c, d, e) }
            text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d} e = ${w.e}")
        }

        fun MockViewValidator.test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            text("a = $a b = $b c = $c d = $d e = $e")
        }

        val composer = compose {
            test(1, 2, 3, 4, 5)
        }

        validate(composer.root) { test(1, 2, 3, 4, 5) }

        compose(composer) {
            test(1, 2, 4, 5, 6)
        }

        validate(composer.root) { test(1, 2, 4, 5, 6) }

        compose(composer, expectChanges = false) {
            test(1, 2, 4, 5, 6)
        }

        validate(composer.root) { test(1, 2, 4, 5, 6) }

        assertEquals(2, count)
    }

    @Test
    fun testInsertGroupInContainer() {
        val values = mutableListOf(0)

        fun MockViewComposition.composition() {
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

        val composer = compose { composition() }

        validate(composer.root) { composition() }

        for (i in 1..10) {
            values.add(i)
            compose(composer) { composition() }
            validate(composer.root) { composition() }
        }
    }

    // b/148273328
    @Test
    fun testInsertInGroups() {

        var threeVisible = false

        fun MockViewComposition.composition() {
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

        val composer = compose { composition() }
        validate(composer.root) { composition() }

        threeVisible = true

        compose(composer) { composition() }

        validate(composer.root) { composition() }
    }

    @Test
    fun testStartJoin() {
        var text = "Starting"
        var invalidate: (() -> Unit)? = null
        fun MockViewComposition.composition() {
            linear {
                join(860) { myInvalidate ->
                    invalidate = { myInvalidate() }
                    text(text)
                }
            }
        }

        fun MockViewValidator.composition() {
            linear {
                text(text)
            }
        }

        val composer = compose { composition() }

        validate(composer.root) { composition() }

        text = "Ending"
        invalidate?.let { it() }

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) { composition() }
    }

    @Test
    fun testInvalidateJoin_End() {
        var text = "Starting"
        var includeNested = true
        var invalidate1: (() -> Unit)? = null
        var invalidate2: (() -> Unit)? = null

        fun MockViewComposition.composition() {
            linear {
                join(860) { myInvalidate ->
                    invalidate1 = { myInvalidate() }
                    text(text)
                    if (includeNested) {
                        join(899) { joinInvalidate ->
                            invalidate2 = { joinInvalidate() }
                            text("Nested in $text")
                        }
                    }
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

        val composer = compose { composition() }

        validate(composer.root) { composition() }

        text = "Ending"
        includeNested = false
        invalidate1?.invoke()
        invalidate2?.invoke()

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) { composition() }

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) { composition() }
    }

    @Test
    fun testInvalidateJoin_Start() {
        var text = "Starting"
        var includeNested = true
        var invalidate1: (() -> Unit)? = null
        var invalidate2: (() -> Unit)? = null

        fun MockViewComposition.composition() {
            linear {
                join(860) { myInvalidate ->
                    invalidate1 = { myInvalidate() }
                    if (includeNested) {
                        join(899) { joinInvalidate ->
                            invalidate2 = { joinInvalidate() }
                            text("Nested in $text")
                        }
                    }
                    text(text)
                }
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

        val composer = compose { composition() }

        validate(composer.root) { composition() }

        text = "Ending"
        includeNested = false
        invalidate1?.invoke()
        invalidate2?.invoke()

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) { composition() }

        composer.recomposeWithCurrent()
        composer.applyChanges()

        validate(composer.root) { composition() }
    }

    // b/132638679
    @Test
    fun testJoinInvalidate() {
        var texts = 5
        var invalidateOuter: (() -> Unit)? = null
        var invalidateInner: (() -> Unit)? = null

        fun MockViewComposition.composition() {
            linear {
                join(1106) { outerInvalidate ->
                    invalidateOuter = { outerInvalidate() }
                    for (i in 1..texts) {
                        text("Some text")
                    }

                    skip(1114) {
                        join(1116) { innerInvalidate ->
                            text("Some text")

                            // Force the invalidation to survive the compose
                            innerInvalidate()
                            invalidateInner = { innerInvalidate() }
                        }
                    }
                }
            }
        }

        val composer = compose { composition() }

        texts = 4
        invalidateOuter?.invoke()
        invalidateInner?.invoke()
        composer.recomposeWithCurrent()
        composer.applyChanges()

        texts = 3
        invalidateOuter?.invoke()
        composer.recomposeWithCurrent()
        composer.applyChanges()
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

        fun MockViewComposition.composition() {
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

        val composer = compose { composition() }
        validate(composer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        compose(composer, expectChanges = false) {
            composition()
        }
        validate(composer.root) { composition() }

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

        fun MockViewComposition.composition() {
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

        val composer = compose { composition() }
        validate(composer.root) { composition() }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        compose(composer, expectChanges = false) {
            composition()
        }
        validate(composer.root) { composition() }

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

        fun MockViewComposition.composition(includeLifecycleObject: Boolean) {
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

        val composer = compose { composition(true) }
        validate(composer.root) { composition(true) }

        assertEquals(1, lifecycleObject.count, "object should have been notified of an enter")

        compose(composer, expectChanges = false) {
            composition(true)
        }
        validate(composer.root) { composition(true) }

        assertEquals(1, lifecycleObject.count, "Object should have only been notified once")

        compose(composer, expectChanges = true) {
            composition(false)
        }
        validate(composer.root) { composition(false) }

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

        fun MockViewComposition.composition(a: Boolean, b: Boolean, c: Boolean) {
            linear {
                if (a) {
                    linear(1) {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("a")
                    }
                }
                if (b) {
                    linear(2) {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("b")
                    }
                }
                if (c) {
                    linear(3) {
                        val l = remember { lifecycleObject }
                        assertEquals(lifecycleObject, l, "Lifecycle object should be returned")
                        text("c")
                    }
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
        val composer = compose { composition(a = true, b = false, c = false) }
        validate(composer.root) {
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
        compose(composer, expectChanges = false) {
            composition(a = true, b = false, c = false)
        }
        validate(composer.root) {
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
        compose(composer, expectChanges = true) {
            composition(a = false, b = true, c = false)
        }
        validate(composer.root) {
            composition(
                a = false,
                b = true,
                c = false
            )
        }
        assertEquals(1, lifecycleObject.count, "No enter or leaves")

        expectedEnter = false
        expectedLeave = false
        compose(composer, expectChanges = true) {
            composition(a = false, b = false, c = true)
        }
        validate(composer.root) {
            composition(
                a = false,
                b = false,
                c = true
            )
        }
        assertEquals(1, lifecycleObject.count, "No enter or leaves")

        expectedEnter = false
        expectedLeave = false
        compose(composer, expectChanges = true) {
            composition(a = true, b = false, c = false)
        }
        validate(composer.root) {
            composition(
                a = true,
                b = false,
                c = false
            )
        }
        assertEquals(1, lifecycleObject.count, "No enter or leaves")

        expectedEnter = false
        expectedLeave = true
        compose(composer, expectChanges = true) {
            composition(a = false, b = false, c = false)
        }
        validate(composer.root) {
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

        fun MockViewComposition.composition(obj: Any) {
            linear {
                linear(1) {
                    remember(obj) { obj }
                    text("Some value")
                }
            }
        }

        fun MockViewValidator.composition() {
            linear {
                linear {
                    text("Some value")
                }
            }
        }

        val composer = compose { composition(obj = lifecycleObject1) }
        validate(composer.root) { composition() }
        assertEquals(1, lifecycleObject1.count, "first object should enter")
        assertEquals(0, lifecycleObject2.count, "second object should not have entered")

        compose(composer, expectChanges = true) {
            composition(lifecycleObject2)
        }
        validate(composer.root) { composition() }
        assertEquals(0, lifecycleObject1.count, "first object should have left")
        assertEquals(1, lifecycleObject2.count, "second object should have entered")

        compose(composer, expectChanges = true) {
            composition(object {})
        }
        validate(composer.root) { composition() }
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

        fun MockViewComposition.lifecycleUser(name: String) {
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

        fun MockViewComposition.tree() {
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

        fun MockViewComposition.composition(includeTree: Boolean) {
            linear {
                if (includeTree) tree()
            }
        }

        val composer = compose { composition(true) }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered"
        )

        compose(composer) {
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
    composer: MockViewComposer? = null,
    expectChanges: Boolean = true,
    block: Compose
): MockViewComposer {
    val myComposer = composer ?: run {
        val root = View().apply { name = "root" }
        MockViewComposer(root)
    }

    myComposer.runWithCurrent {
        myComposer.compose {
            block()
        }
    }

    if (expectChanges) {
        assertNotEquals(0, myComposer.changeCount, "changes were expected")
        myComposer.applyChanges()
    } else {
        assertEquals(0, myComposer.changeCount, "no changes were expected")
    }

    return myComposer
}

private fun compose(
    model: ContactModel,
    composer: MockViewComposer? = null,
    expectChanges: Boolean = true
): MockViewComposer =
    compose(composer = composer, expectChanges = expectChanges) {
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
