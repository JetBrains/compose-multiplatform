// Verifies generated constructors against the model and coverage ledgers.
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Checks emitted and intentionally skipped constructors against the generator ledgers.
class GeneratedConstructorLedgerTest {
    private val report = GeneratedModelReport.read()
    private val coverage = GeneratedCoverageReport.read()

    // Checks each emitted constructor set has a valid primary/secondary shape.
    @Test
    fun everyClassWithConstructorsHasExactlyOnePrimaryOrOnlySecondaries() {
        report.declarations.filter { it.constructors.isNotEmpty() }.forEach { declaration ->
            val primaries = declaration.constructors.count { it.startsWith("primary ") }
            assertTrue(
                primaries <= 1,
                "${declaration.name} emits $primaries primary constructors",
            )
        }
        // Both shapes occur, so neither branch above is vacuous.
        assertEquals(1, report.byName.getValue("org.w3c.dom.events.Event").constructors.count {
            it.startsWith("primary ")
        })
        assertTrue(
            report.byName.getValue("org.w3c.dom.DOMPoint").constructors.all { it.startsWith("secondary ") },
            "DOMPoint declares only secondary constructors upstream",
        )
    }

    // Omits redundant synthesized constructors but keeps no-arg constructors needed beside secondaries.
    @Test
    fun aLoneNoArgumentConstructorIsNotEmitted() {
        report.declarations.forEach { declaration ->
            assertTrue(
                declaration.constructors.size != 1 || !declaration.constructors.single().endsWith("()"),
                "${declaration.name} emits a no-argument constructor and nothing else",
            )
        }

        listOf("org.w3c.dom.Document", "org.w3c.dom.DocumentFragment", "org.w3c.dom.Range").forEach { name ->
            assertTrue(report.byName.getValue(name).constructors.isEmpty(), "$name emitted a constructor")
            assertEquals(
                "implicit-constructor",
                coverage.entry("constructor", "$name#constructor()").reason,
            )
        }
        assertContains(report.byName.getValue("org.w3c.dom.DOMMatrix").constructors, "primary constructor()")
    }

    // Keeps input classifiers selected even when only a constructor references them.
    @Test
    fun anInputClassifierNamedOnlyByAConstructorIsSelected() {
        val eventInit = report.byName.getValue("org.w3c.dom.EventInit")

        assertEquals("input", eventInit.origin)
        assertEquals("dictionary", eventInit.kind)

        val referrers = report.declarations.filter { eventInit.portableName in report.signatureTypes(it) }
        assertEquals(listOf("org.w3c.dom.events.Event"), referrers.map { it.name })
        report.declarations.forEach { declaration ->
            assertTrue(
                declaration.members.none { eventInit.portableName in it },
                "${declaration.name} names EventInit from a member, so it is not constructor-only",
            )
        }
    }

    /** Every emitted constructor is closed over the closure, the same way every member signature is. */
    @Test
    fun everyConstructorSignatureIsClosedOverTheEmittedClosure() {
        val facadeNames = report.declarations.mapTo(mutableSetOf()) { it.portableName }

        report.declarations.forEach { declaration ->
            declaration.constructors.forEach { constructor ->
                Regex("""[a-z][\w.]*\.[A-Z]\w*""").findAll(constructor).forEach { match ->
                    assertTrue(
                        match.value.startsWith("kotlin.") ||
                            match.value in GeneratedModelReport.INTEROP_TYPES ||
                            match.value in facadeNames,
                        "${declaration.name} takes ${match.value}, which is not in the closure",
                    )
                }
            }
        }
    }

    /** Numeric sequence element mapping makes both browser constructor shapes one portable API. */
    @Test
    fun numericSequenceConstructorsUseJsDoubleAndAreEmitted() {
        assertEquals(
            listOf("primary constructor(kotlinx.browser.JsArray<kotlinx.browser.JsDouble>)"),
            report.byName.getValue("org.w3c.dom.DOMMatrixReadOnly").constructors,
        )
        assertEquals(
            listOf(
                "primary constructor()",
                "secondary constructor(kotlin.String)",
                "secondary constructor(kotlinx.browser.dom.DOMMatrixReadOnly)",
                "secondary constructor(kotlinx.browser.webgl.Float32Array)",
                "secondary constructor(kotlinx.browser.webgl.Float64Array)",
                "secondary constructor(kotlinx.browser.JsArray<kotlinx.browser.JsDouble>)",
            ),
            report.byName.getValue("org.w3c.dom.DOMMatrix").constructors,
        )

        val numericSequences = coverage.of("constructor").filter {
            "numberSequence" in it.subject &&
                (it.subject.startsWith("org.w3c.dom.DOMMatrix#") ||
                    it.subject.startsWith("org.w3c.dom.DOMMatrixReadOnly#"))
        }

        assertEquals(
            listOf("org.w3c.dom.DOMMatrix", "org.w3c.dom.DOMMatrixReadOnly"),
            numericSequences.map { it.subject.substringBefore('#') }.sorted(),
        )
        assertTrue(numericSequences.all { it.ported })
    }

    // Requires every browser constructor to be emitted or skipped with a reason.
    @Test
    fun everyBrowserConstructorHasADecisionThatMatchesTheModel() {
        val decisions = coverage.of("constructor").filterNot { it.subject.contains(" <= ") }
        val emitted = decisions.filter(GeneratedCoverageReport.Entry::ported)

        assertEquals(report.counts.getValue("constructors"), emitted.size)
        assertTrue(decisions.size > emitted.size, "no constructor was skipped, which cannot be right")
        decisions.filterNot(GeneratedCoverageReport.Entry::ported).forEach {
            assertTrue(it.reason?.isNotEmpty() == true, "${it.subject} was skipped without a reason")
        }

        // The subject names the classifier it was declared on, so the two ledgers can be compared.
        val emittedPerClass = emitted.groupingBy { it.subject.substringBefore('#') }.eachCount()
        report.declarations.forEach { declaration ->
            assertEquals(
                declaration.constructors.size,
                emittedPerClass[declaration.name] ?: 0,
                "${declaration.name} emitted a different number of constructors than coverage records",
            )
        }
    }
}
