package com.google.r4a

import com.google.r4a.mock.*

import junit.framework.TestCase
import org.junit.Assert

class CompositionTests : TestCase() {
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

    fun testRecomposeWithoutChanges() {
        val model = testModel()
        val composer = compose(model)

        compose(model, composer, expectChanges = false)

        validate(composer.root) {
            selectContact(model)
        }
    }

    fun testInsertAContact() {
        val model = testModel(mutableListOf(bob, jon))
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

    fun testMoveAContact() {
        val model = testModel(mutableListOf(bob, steve, jon))
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

    fun testChangeTheFilter() {
        val model = testModel(mutableListOf(bob, steve, jon))
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

    fun testComposeCompositionWithMultipleRoots() {
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois)

        val composer = compose {
            reportsReport(reports)
        }

        validate(composer.root) {
            reportsReport(reports)
        }
    }

    fun testMoveCompositionWithMultipleRoots() {
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois)
        val composer = compose {
            reportsReport(reports)
        }

        val newReports = listOf(jim_reports_to_sally, clark_reports_to_lois, rob_reports_to_alice)
        compose(composer) {
            reportsReport(newReports)
        }

        validate(composer.root) {
            reportsReport(newReports)
        }
    }

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


    fun testMovingMemoization() {
        val points = listOf(Point(1, 2), Point(2, 3), Point(4, 5), Point(6, 7))
        val composer = compose {
            points(points)
        }

        validate(composer.root) { points(points) }

        val modifiedPoints = listOf(Point(1, 2), Point(4, 5), Point(2, 3), Point(6, 7))
        compose(composer) {
            points(modifiedPoints)
        }

        validate(composer.root) { points(modifiedPoints) }
    }

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
                    composeComponent(
                        slReportReports,
                        ::Reporter,
                        a1 = report, set1 = { this.report = it })
                }
            }
        }

        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois)
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
            composeComponent(two, ::Two,
                             41, { first = it },
                             42, { second = it })
        }

        validate(composer.root) {
            two(41, 42)
        }


    }

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
            composeComponent(three, ::Three,
                             41, { first = it },
                             42, { second = it },
                             43, { third = it })
        }

        validate(composer.root) {
            three(41, 42, 43)
        }
    }

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
            composeComponent(four, ::Four) {
                set(41) { first = it }
                set(42) { second = it }
                set(43) { third = it }
                set(44) { fourth = it }
            }
        }

        validate(composer.root) {
            four(41, 42, 43, 44)
        }
    }

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
            composeComponent(key, { One(first = value) }) {
                update(value) { first = it }
            }
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
            composeComponent(cc.joinKey(key, value), { One(first = value) }) { }
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
                    composeComponent(
                        slReportReports,
                        ::Reporter,
                        a1 = report, set1 = { this.report = it })
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois, r)
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

        composer.recompose()
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

    fun testRecomposeWithReplace() {
        val slReportReports = object {}
        var recomposeLois: (()->Unit)? = null
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
                    composeComponent(
                        slReportReports,
                        ::Reporter,
                        a1 = report, set1 = { this.report = it})
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois, r)
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

        composer.recompose()
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

    // remember()

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
}


private fun compose(composer: MockViewComposer? = null, expectChanges: Boolean = true, block: Compose): MockViewComposer {
    val myComposer = composer ?: run {
        val root = View().apply { name = "root" }
        MockViewComposer(root)
    }

    myComposer.compose {
        block()
    }
    if (expectChanges) {
        Assert.assertNotEquals(0, myComposer.changeCount)
        myComposer.applyChanges()
    } else {
        Assert.assertEquals(0, myComposer.changeCount)
    }

    return myComposer
}

private fun compose(model: ContactModel, composer: MockViewComposer? = null, expectChanges: Boolean = true): MockViewComposer =
    compose(composer = composer, expectChanges = expectChanges) {
        selectContact(model)
    }

// Contact test data
private val bob = Contact("Bob Smith", email = "bob@smith.com")
private val jon = Contact(name = "Jon Alberton", email = "jon@alberton.com")
private val steve = Contact("Steve Roberson", email = "steverob@somemail.com")

private fun testModel(contacts: MutableList<Contact> = mutableListOf(bob, jon, steve)) = ContactModel(filter = "", contacts = contacts)

// Report test data
private val jim_reports_to_sally = Report("Jim", "Sally")
private val rob_reports_to_alice = Report("Rob", "Alice")
private val clark_reports_to_lois = Report("Clark", "Lois")
private val griffin_reports_to_alice = Report("Griffin", "Alice")
