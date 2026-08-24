/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies identity-only declarations against the generated ledgers.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Checks identity-only dependencies against the generator ledgers.
class GeneratedIdentityOnlyLedgerTest {
    private val coverage = GeneratedCoverageReport.read()
    private val model = GeneratedModelReport.read()

    private val identities = model.declarations.filter { it.origin == "identity-only" }
    private val webGl = identities.filter { it.name.startsWith("org.khronos.webgl.") }

    /** Package eligibility must not let new identities creep into the facade unnoticed. */
    @Test
    fun exactlyTheExpectedClassifiersAreEmitted() {
        assertEquals(
            listOf(
                "org.khronos.webgl.ArrayBuffer",
                "org.khronos.webgl.ArrayBufferView",
                "org.khronos.webgl.Float32Array",
                "org.khronos.webgl.Float64Array",
                "org.khronos.webgl.Uint8ClampedArray",
                "org.w3c.dom.encryptedmedia.MediaKeySystemConfiguration",
                "org.w3c.dom.encryptedmedia.MediaKeys",
                "org.w3c.dom.mediacapture.MediaDevices",
                "org.w3c.dom.mediacapture.MediaStream",
                "org.w3c.dom.mediacapture.MediaStreamConstraints",
                "org.w3c.dom.mediasource.SourceBuffer",
                "org.w3c.dom.pointerevents.PointerEvent",
                "org.w3c.dom.svg.SVGSVGElement",
                "org.w3c.fetch.Request",
                "org.w3c.fetch.RequestCredentials",
                "org.w3c.fetch.RequestDestination",
                "org.w3c.fetch.RequestInit",
                "org.w3c.files.Blob",
                "org.w3c.files.File",
                "org.w3c.files.FileList",
                "org.w3c.performance.Performance",
                "org.w3c.workers.CacheStorage",
                "org.w3c.workers.ServiceWorkerContainer",
                "org.w3c.xhr.ProgressEvent",
            ),
            identities.map { it.name }.sorted(),
        )
        assertEquals(identities.size, model.counts.getValue("identityOnly"))
        // A WebGL classifier used as an actual supertype is emitted normally, outside this category.
        assertEquals(
            (webGl.map { it.name } + "org.khronos.webgl.TexImageSource").sorted(),
            model.byName.keys.filter { it.startsWith("org.khronos.") }.sorted(),
        )
    }

    /** An identity and nothing else: no member, constructor, companion value or factory default. */
    @Test
    fun eachIsEmittedAsABareIdentity() {
        identities.forEach { declaration ->
            assertEquals(0, declaration.memberCount, "${declaration.name} carries members")
            assertEquals(emptyList(), declaration.members, "${declaration.name} carries members")
            assertEquals(emptyList(), declaration.constructors, "${declaration.name} carries constructors")
            assertEquals(emptyList(), declaration.values, "${declaration.name} carries values")
        }
    }

    // Keeps edges between listed types without pulling in unlisted browser supertypes.
    @Test
    fun onlyTheEdgesBetweenListedClassifiersAreKept() {
        val view = model.byName.getValue("org.khronos.webgl.ArrayBufferView")
        assertEquals(emptyList(), view.superinterfaces)
        assertEquals(null, view.parent)

        webGl.filter { it.name.endsWith("Array") }.forEach { array ->
            assertEquals(listOf("ArrayBufferView"), array.superinterfaces, "${array.name} lost its edge")
            assertEquals(null, array.parent)
        }
        assertTrue("org.khronos.webgl.BufferDataSource" !in model.byName)
    }

    // Requires every omitted declaration to have the expected exclusion reason.
    @Test
    fun everyOmittedMemberIsRecordedWithItsReason() {
        val skips = coverage.entries.filter { it.reason == "identity-only-dependency" }

        assertTrue(skips.isNotEmpty())
        assertTrue(skips.all { entry -> identities.any { entry.subject.startsWith(it.name) } })
        // The members these classifiers really do declare, spot-checked against the browser sources.
        listOf(
            "member" to "org.khronos.webgl.ArrayBuffer#val byteLength",
            "member" to "org.khronos.webgl.ArrayBufferView#val buffer",
            "constructor" to "org.khronos.webgl.Uint8ClampedArray#constructor(length: Int)",
            "companion" to "org.khronos.webgl.Uint8ClampedArray <= org.khronos.webgl.Uint8ClampedArray.Companion",
        ).forEach { (kind, subject) ->
            assertEquals("identity-only-dependency", coverage.entry(kind, subject).reason)
        }
    }

    /** Package mappings allow discovery; they never emit an unreferenced sibling. */
    @Test
    fun signatureOnlyPackagesEmitJustWhatInputSignaturesName() {
        val portedTypes = model.declarations
            .filter { it.origin != "identity-only" }
            .flatMapTo(mutableSetOf(), model::signatureTypes)

        assertTrue(identities.isNotEmpty())
        identities.forEach { assertTrue(it.portableName in portedTypes, "${it.name} is unreferenced") }
        listOf(
            "org.khronos.webgl.Int8Array",
            "org.w3c.files.FileReader",
            "org.w3c.xhr.XMLHttpRequest",
        ).forEach {
            assertTrue(it !in model.byName, "$it leaked in with its package")
        }
    }

    // Pins the signatures unlocked by portable typed-array names.
    @Test
    fun thePortableSignaturesTheCategoryUnlockedAreEmitted() {
        val readOnly = model.byName.getValue("org.w3c.dom.DOMMatrixReadOnly")
        val matrix = model.byName.getValue("org.w3c.dom.DOMMatrix")

        assertTrue("fun toFloat32Array(): kotlinx.browser.webgl.Float32Array" in readOnly.members)
        assertTrue("fun toFloat64Array(): kotlinx.browser.webgl.Float64Array" in readOnly.members)
        assertTrue("secondary constructor(kotlinx.browser.webgl.Float32Array)" in matrix.constructors)
        assertTrue("secondary constructor(kotlinx.browser.webgl.Float64Array)" in matrix.constructors)
    }

    /** Actual supertypes use package eligibility to retain identity and API. */
    @Test
    fun mappedSupertypesAreEmittedNormally() {
        assertEquals("supertype", model.byName.getValue("org.khronos.webgl.TexImageSource").origin)
    }
}
