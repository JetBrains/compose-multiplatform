// Guards the generated API manifest and its checked-in baseline.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies that every input declaration has an explicit, reviewable manifest outcome. */
class GeneratedApiManifestTest {
    private val manifest = GeneratedApiManifest.read()

    @Test
    fun theManifestMatchesTheCheckedInBaseline() {
        val generated = GeneratedApiManifest.lines()
        val baseline = GeneratedApiManifest.baselineLines()
        if (generated == baseline) return

        val added = generated - baseline.toSet()
        val removed = baseline - generated.toSet()
        val diff = (removed.map { "- $it" } + added.map { "+ $it" }).take(40).joinToString("\n")
        val elided = added.size + removed.size - 40
        throw AssertionError(
            "The generated API manifest differs from the checked-in one by ${removed.size} removed " +
                "and ${added.size} added rows. Every difference is a change to which part of the " +
                "browser's API the facade covers; review it, then copy the generated manifest over " +
                "api/dom-api-manifest.txt.\n$diff" + if (elided > 0) "\n... and $elided more" else "",
        )
    }

    @Test
    fun everyDeclarationInTheInputFilesIsAccountedFor() {
        val unaccounted = manifest.entries.filter { it.status == "UNACCOUNTED" }

        assertEquals(
            unaccounted.size,
            manifest.count("unaccounted"),
            "the manifest header disagrees with its UNACCOUNTED rows",
        )
        assertEquals(emptyList(), unaccounted.map { it.subject })
        assertEquals(
            manifest.entries.size,
            manifest.count("emitted") + manifest.count("excluded"),
            "the manifest header disagrees with its own rows",
        )
    }

    /** Guards the explicitly selected browser input boundary. */
    @Test
    fun theManifestCoversTheConfiguredInputFiles() {
        assertEquals(
            "org.w3c.dom.css.kt,org.w3c.dom.events.kt,org.w3c.dom.kt",
            manifest.header.getValue("files"),
        )
    }

    /** Requires every declaration in `dom.events.kt` to be emitted. */
    @Test
    fun theEventsFileHasNoOmissionsAtAll() {
        val events = manifest.entries.filter { it.owner.startsWith("org.w3c.dom.events.") }

        assertTrue(events.isNotEmpty(), "the events input contributes no declarations")
        assertEquals(emptyList(), events.filterNot { it.emitted }.map { it.subject })
    }

    /** Requires every exclusion to use a structured, known reason. */
    @Test
    fun everyExclusionCarriesAKnownReason() {
        val excluded = manifest.entries.filterNot(GeneratedApiManifest.Entry::emitted)
        val reasons = excluded.mapTo(mutableSetOf()) { it.reason }
        val knownReasons = setOf("implicit-constructor")

        assertTrue(excluded.all { it.reason.matches(Regex("[a-z0-9-]+")) }, "reasons: $reasons")
        assertEquals(emptySet(), reasons - knownReasons, "unknown exclusion reasons")
    }

    /** The complete CSS input includes inline styles and its static companion function. */
    @Test
    fun theCssFilePromotesInlineStylesWithTheirMembers() {
        val css = manifest.entries.filter { it.owner.startsWith("org.w3c.dom.css.") }
        val style = css.filter { it.owner == "org.w3c.dom.css.CSSStyleDeclaration" }

        assertTrue(css.isNotEmpty(), "the CSS input contributes no declarations")
        assertTrue(css.all(GeneratedApiManifest.Entry::emitted))
        assertTrue(style.isNotEmpty(), "CSSStyleDeclaration contributes no declarations")
        assertTrue(style.all(GeneratedApiManifest.Entry::emitted))
        assertTrue(style.single { it.subject.endsWith("#var color: String") }.emitted)
        assertTrue(style.single { it.subject.contains("#fun setProperty(") }.emitted)
        assertTrue(css.single { it.subject.endsWith("#fun escape(ident: String): String") }.emitted)
    }

    /** Covers the core DOM classifiers and every declaration attached to them. */
    @Test
    fun theCoreDomSurfaceIsEmittedInFull() {
        val owners = setOf(
            "org.w3c.dom.DOMRectList",
            "org.w3c.dom.ScrollIntoViewOptions",
            "org.w3c.dom.ScrollLogicalPosition",
        )
        val surface = manifest.entries.filter { it.owner in owners }

        assertTrue(surface.all(GeneratedApiManifest.Entry::emitted))
        owners.forEach { owner ->
            assertTrue(surface.single { it.owner == owner && it.kind == "classifier" }.emitted, owner)
        }
    }

    /** Covers the HTML element surface including its external signature identities. */
    @Test
    fun theHtmlElementSurfaceIsComplete() {
        val owners = setOf(
            "org.w3c.dom.Audio",
            "org.w3c.dom.HTMLAppletElement",
            "org.w3c.dom.HTMLBaseElement",
            "org.w3c.dom.HTMLBodyElement",
            "org.w3c.dom.HTMLDataElement",
            "org.w3c.dom.HTMLDetailsElement",
            "org.w3c.dom.HTMLDialogElement",
            "org.w3c.dom.HTMLDirectoryElement",
            "org.w3c.dom.HTMLFontElement",
            "org.w3c.dom.HTMLFrameElement",
            "org.w3c.dom.HTMLFrameSetElement",
            "org.w3c.dom.HTMLHtmlElement",
            "org.w3c.dom.HTMLKeygenElement",
            "org.w3c.dom.HTMLLinkElement",
            "org.w3c.dom.HTMLMarqueeElement",
            "org.w3c.dom.HTMLMenuItemElement",
            "org.w3c.dom.HTMLMetaElement",
            "org.w3c.dom.HTMLModElement",
            "org.w3c.dom.HTMLQuoteElement",
            "org.w3c.dom.HTMLScriptElement",
            "org.w3c.dom.HTMLTemplateElement",
            "org.w3c.dom.HTMLTimeElement",
            "org.w3c.dom.HTMLTitleElement",
            "org.w3c.dom.HTMLUnknownElement",
            "org.w3c.dom.Image",
            "org.w3c.dom.Option",
            "org.w3c.dom.RadioNodeList",
        )
        val surface = manifest.entries.filter { it.owner in owners }

        assertTrue(surface.all(GeneratedApiManifest.Entry::emitted))
        owners.forEach { owner ->
            assertTrue(surface.single { it.owner == owner && it.kind == "classifier" }.emitted, owner)
        }
        assertTrue(surface.single { it.subject.endsWith("#var workerType: WorkerType") }.emitted)
    }

    /** Numeric sequence signatures are normalized instead of becoming target mismatches. */
    @Test
    fun numericSequenceDeclarationsAreEmittedOnBothWebTargets() {
        val mismatched = manifest.entries.filter { it.reason == "target-declaration-mismatch" }
        val numericSequences = listOf(
            "org.w3c.dom.CanvasPathDrawingStyles#fun getLineDash(): JsArray<JsNumber>",
            "org.w3c.dom.CanvasPathDrawingStyles#fun setLineDash(segments: JsArray<JsNumber>): Unit",
            "org.w3c.dom.DOMMatrix#constructor(numberSequence: JsArray<JsNumber>)",
            "org.w3c.dom.DOMMatrixReadOnly#constructor(numberSequence: JsArray<JsNumber>)",
        )

        assertEquals(emptyList(), mismatched.map { it.subject })
        numericSequences.forEach { subject ->
            assertTrue(manifest.entries.single { it.subject == subject }.emitted, subject)
        }
    }

    /** Compatible mixins keep both their identity and members. */
    @Test
    fun compatibleMixinsAreFullyEmitted() {
        val childNode = manifest.entries.filter { it.owner == "org.w3c.dom.ChildNode" }

        assertTrue(childNode.all(GeneratedApiManifest.Entry::emitted))
        assertTrue(childNode.size > 1, "ChildNode contributes no members")
    }

    /** AbstractWorker keeps both its classifier identity and its single member. */
    @Test
    fun abstractWorkerKeepsItsClassifierAndMember() {
        val abstractWorker = manifest.entries.filter { it.owner == "org.w3c.dom.AbstractWorker" }

        assertTrue(abstractWorker.single { it.kind == "classifier" }.emitted)
        assertTrue(abstractWorker.single { it.kind == "member" }.emitted)
    }
}
