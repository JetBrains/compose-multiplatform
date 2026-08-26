/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies the generated classifier and signature closure.
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Checks classifier selection, ordering, and signature closure against the generator ledger.
class GeneratedClosureTest {
    private val report = GeneratedModelReport.read()

    private val nameBySimpleName: Map<String, String> =
        report.declarations.associate { it.simpleName to it.name }

    @Test
    fun ledgerCountsMatchTheEmittedClosure() {
        assertEquals(report.declarations.size, report.counts.getValue("closure"))
        assertEquals(report.declarations.sumOf { it.memberCount }, report.counts.getValue("members"))
        assertEquals(
            report.declarations.sumOf { it.constructors.size },
            report.counts.getValue("constructors"),
        )
        assertEquals(report.counts.getValue("inputs"), report.declarations.count { it.origin == "input" })
        assertEquals(
            report.counts.getValue("dependencies"),
            report.declarations.count { it.origin == "dependency" },
        )
        assertEquals(
            report.counts.getValue("identityOnly"),
            report.declarations.count { it.origin == "identity-only" },
        )
        // Simple names are how the ledger records superinterfaces, and how the two facade packages
        // are told apart nowhere else; a collision would make the rest of this class ambiguous.
        assertEquals(report.declarations.size, nameBySimpleName.size)
    }

    // Requires supertypes to be emitted before their dependents.
    @Test
    fun closureIsOrderedSupertypesFirst() {
        val positions = report.order.withIndex().associate { (index, name) -> name to index }

        report.declarations.forEach { declaration ->
            val position = positions.getValue(declaration.name)
            declaration.parent?.let { parent ->
                assertTrue(
                    positions.getValue(parent) < position,
                    "${declaration.name} is emitted before its superclass $parent",
                )
            }
            declaration.superinterfaces.forEach { simpleName ->
                val superinterface = nameBySimpleName.getValue(simpleName)
                assertTrue(
                    positions.getValue(superinterface) < position,
                    "${declaration.name} is emitted before its superinterface $superinterface",
                )
            }
        }
    }

    // Pins the deterministic supertypes-first order for alphabetically visited roots.
    @Test
    fun closureOrderIsTheDeterministicSupertypesFirstWalk() {
        val expected = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(name: String) {
            if (!visited.add(name)) return
            val declaration = report.byName.getValue(name)
            declaration.parent?.let(::visit)
            declaration.superinterfaces.forEach { visit(nameBySimpleName.getValue(it)) }
            expected += name
        }

        report.selected.map { it.name }.sorted().forEach(::visit)

        assertEquals(expected, report.order)
    }

    // Ensures signatures name only emitted facade or generated interop types.
    @Test
    fun everySignatureIsClosedOverTheEmittedClosure() {
        val facadeNames = report.declarations.mapTo(mutableSetOf()) { it.portableName }

        report.declarations.forEach { declaration ->
            report.signatureTypes(declaration).forEach { type ->
                assertTrue(
                    type.startsWith("kotlin.") ||
                        type in GeneratedModelReport.INTEROP_TYPES ||
                        type in facadeNames,
                    "${declaration.name} names $type, which is not in the closure",
                )
            }
        }
    }

    /** Every interop type is reachable, so none of them is generated for nothing. */
    @Test
    fun everyInteropTypeIsNamedByTheFacade() {
        val named = report.declarations.flatMapTo(mutableSetOf(), report::signatureTypes)

        GeneratedModelReport.INTEROP_TYPES.forEach { type ->
            assertContains(named, type, "$type is emitted but no signature names it")
        }
    }

    /** Only numbers nested directly in JavaScript arrays use the target-normalizing scalar. */
    @Test
    fun numericArrayElementsUseJsDoubleWithoutChangingScalarJsNumber() {
        val canvas = report.byName.getValue("org.w3c.dom.CanvasPathDrawingStyles")
        val event = report.byName.getValue("org.w3c.dom.events.Event")

        assertContains(canvas.members, "fun getLineDash(): kotlinx.browser.JsArray<kotlinx.browser.JsDouble>")
        assertContains(
            canvas.members,
            "fun setLineDash(kotlinx.browser.JsArray<kotlinx.browser.JsDouble>): kotlin.Unit",
        )
        assertContains(event.members, "val timeStamp: kotlinx.browser.JsNumber")
    }

    // Keeps explicit input roots selected independently of signature reachability.
    @Test
    fun inputClassifiersDoNotDependOnAccidentalSignatureReachability() {
        listOf("org.w3c.dom.Navigator", "org.w3c.dom.MimeTypeArray").forEach { name ->
            val declaration = report.byName.getValue(name)
            assertEquals("input", declaration.origin, "$name was not selected from its input file")

            val referrers = report.declarations
                .filter { it.name != name && declaration.portableName in report.signatureTypes(it) }
            assertTrue(referrers.isNotEmpty(), "$name is named by nothing, so it should not be selected")
        }

        // The chain those referrers form, spelled out: each link is named by the previous one.
        listOf(
            "org.w3c.dom.Window" to "org.w3c.dom.Navigator",
            "org.w3c.dom.NavigatorPlugins" to "org.w3c.dom.MimeTypeArray",
        ).forEach { (referrer, referenced) ->
            assertContains(
                report.signatureTypes(report.byName.getValue(referrer)),
                report.byName.getValue(referenced).portableName,
            )
        }
    }

    @Test
    fun coreDomSurfaceKeepsItsArrayDictionaryAndEnumShapes() {
        val rects = report.byName.getValue("org.w3c.dom.DOMRectList")
        val arrayLike = report.byName.getValue("org.w3c.dom.ItemArrayLike")
        val options = report.byName.getValue("org.w3c.dom.ScrollIntoViewOptions")
        val positions = report.byName.getValue("org.w3c.dom.ScrollLogicalPosition")

        assertEquals(
            listOf(
                "val length: kotlin.Int",
                "fun item(kotlin.Int): T?",
            ),
            arrayLike.members,
        )
        assertEquals(emptyList(), rects.members)
        assertEquals(listOf("ItemArrayLike"), rects.superinterfaces)
        assertEquals(
            listOf(
                "var block: kotlinx.browser.dom.ScrollLogicalPosition?",
                "var inline: kotlinx.browser.dom.ScrollLogicalPosition?",
            ),
            options.members,
        )
        assertEquals(
            listOf("var behavior: kotlinx.browser.dom.ScrollBehavior?"),
            report.byName.getValue("org.w3c.dom.ScrollOptions").members,
        )
        assertEquals(
            listOf(
                "ScrollLogicalPosition.Companion.START",
                "ScrollLogicalPosition.Companion.CENTER",
                "ScrollLogicalPosition.Companion.END",
                "ScrollLogicalPosition.Companion.NEAREST",
            ),
            positions.values,
        )
    }

    /** The worker promotion also makes HTMLLinkElement's WorkerType member portable. */
    @Test
    fun workerTypesAreSelectedAndAbstractWorkerKeepsItsIdentity() {
        assertEquals(
            listOf("WorkerType.Companion.CLASSIC", "WorkerType.Companion.MODULE"),
            report.byName.getValue("org.w3c.dom.WorkerType").values,
        )
        assertContains(
            report.byName.getValue("org.w3c.dom.HTMLLinkElement").members,
            "var workerType: kotlinx.browser.dom.WorkerType",
        )
        assertContains(
            report.byName.getValue("org.w3c.dom.AbstractWorker").members,
            "var onerror: ((kotlinx.browser.dom.events.Event) -> kotlin.Unit)?",
        )
        assertContains(report.byName.getValue("org.w3c.dom.Worker").superinterfaces, "AbstractWorker")
        assertContains(report.byName.getValue("org.w3c.dom.SharedWorker").superinterfaces, "AbstractWorker")
    }

    /** Signature-only packages supply callback identities without importing their APIs. */
    @Test
    fun signatureOnlyCallbacksJoinTheClosure() {
        assertEquals("input", report.byName.getValue("org.w3c.dom.MessageEvent").origin)
        assertContains(
            report.byName.getValue("org.w3c.dom.WindowEventHandlers").members,
            "var onmessage: ((kotlinx.browser.dom.MessageEvent) -> kotlin.Unit)?",
        )
        assertContains(
            report.byName.getValue("org.w3c.dom.GlobalEventHandlers").members,
            "var onprogress: ((kotlinx.browser.xhr.ProgressEvent) -> kotlin.Unit)?",
        )
        assertEquals("identity-only", report.byName.getValue("org.w3c.xhr.ProgressEvent").origin)
    }

    // Verifies callbacks preserve every nested parameter and return type.
    @Test
    fun callbacksArePortedWithEveryTypeInside() {
        val window = report.byName.getValue("org.w3c.dom.Window").members
        val globalScope = report.byName.getValue("org.w3c.dom.WindowOrWorkerGlobalScope").members
        val handlers = report.byName.getValue("org.w3c.dom.GlobalEventHandlers").members
        val document = report.byName.getValue("org.w3c.dom.Document").members

        // A facade classifier as the callback's parameter.
        assertContains(handlers, "var onclick: ((kotlinx.browser.dom.events.MouseEvent) -> kotlin.Unit)?")
        // A builtin as its result, rather than the `Unit` almost every handler returns.
        assertContains(
            document,
            "fun createTreeWalker(kotlinx.browser.dom.Node, kotlin.Int, " +
                "((kotlinx.browser.dom.Node) -> kotlin.Short)?): kotlinx.browser.dom.TreeWalker",
        )
        // An interop type as the result, next to a vararg of the same type outside the callback.
        assertContains(
            globalScope,
            "fun setTimeout(() -> kotlinx.browser.JsAny?, kotlin.Int, vararg kotlinx.browser.JsAny?): kotlin.Int",
        )
        assertContains(window, "fun requestAnimationFrame((kotlin.Double) -> kotlin.Unit): kotlin.Int")
        // The widest one the browser declares: interop types in parameter and result position both.
        assertContains(
            handlers,
            "var onerror: ((kotlinx.browser.JsAny?,kotlin.String,kotlin.Int,kotlin.Int," +
                "kotlinx.browser.JsAny?) -> kotlinx.browser.JsAny?)?",
        )
    }

    // Verifies selected signatures retain nested generic types around `MutationObserver`.
    @Test
    fun genericsNestInsideSelectedSignatures() {
        val observer = report.byName.getValue("org.w3c.dom.MutationObserver")

        assertEquals("input", observer.origin)
        assertContains(
            observer.members,
            "fun takeRecords(): kotlinx.browser.JsArray<kotlinx.browser.dom.MutationRecord>",
        )
        listOf("org.w3c.dom.MutationRecord", "org.w3c.dom.MutationObserverInit").forEach { name ->
            assertEquals("input", report.byName.getValue(name).origin, "$name was not selected")
        }
        assertEquals("dictionary", report.byName.getValue("org.w3c.dom.MutationObserverInit").kind)
    }

    /** Keeps target-inconsistent promise results opaque even when their classifier is selected. */
    @Test
    fun promiseResultTypesRemainOpaqueWhenTheClassifierIsSelectedIndependently() {
        val globalScope = report.byName.getValue("org.w3c.dom.WindowOrWorkerGlobalScope").members

        assertTrue(
            globalScope.any {
                it.startsWith("fun createImageBitmap(") && it.endsWith("kotlinx.browser.Promise<*>")
            },
            "createImageBitmap should be ported with an opaque promise result",
        )
        assertEquals("input", report.byName.getValue("org.w3c.dom.ImageBitmap").origin)
    }

    /** Every mixin keeps its classifier identity; `definedExternally` members become expect `open`. */
    @Test
    fun mixinsKeepTheirClassifierIdentities() {
        val emitted = listOf(
            "AbstractWorker",
            "DocumentAndElementEventHandlers",
            "DocumentOrShadowRoot",
            "GlobalEventHandlers",
            "NonDocumentTypeChildNode",
            "ParentNode",
            "Slotable",
            "WindowEventHandlers",
            "ChildNode",
            "GeometryUtils",
            "HTMLOrSVGImageElement",
            "NavigatorConcurrentHardware",
            "NavigatorContentUtils",
            "NavigatorCookies",
            "NavigatorID",
            "NavigatorLanguage",
            "NavigatorOnLine",
            "NavigatorPlugins",
            "NonElementParentNode",
            "ScrollOptions",
            "WindowLocalStorage",
            "WindowOrWorkerGlobalScope",
            "WindowSessionStorage",
        )

        emitted.forEach { simpleName ->
            assertEquals("input", report.byName.getValue("org.w3c.dom.$simpleName").origin)
            Class.forName("kotlinx.browser.dom.$simpleName")
        }

        val parentNode = report.byName.getValue("org.w3c.dom.ParentNode").members
        assertTrue(parentNode.any { it.startsWith("fun querySelector(") }, "ParentNode lost querySelector")
        assertContains(report.byName.getValue("org.w3c.dom.Element").superinterfaces, "ParentNode")
        assertContains(report.byName.getValue("org.w3c.dom.Element").superinterfaces, "GeometryUtils")
        assertContains(
            report.byName.getValue("org.w3c.dom.Window").superinterfaces,
            "WindowOrWorkerGlobalScope",
        )
    }

    // Pins the full listener surface, including callback and option-dictionary overloads.
    @Test
    fun eventTargetPortsItsCompleteListenerSurface() {
        val members = report.byName.getValue("org.w3c.dom.events.EventTarget").members
        val callback = "((kotlinx.browser.dom.events.Event) -> kotlin.Unit)?"

        listOf(
            "fun addEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?, " +
                "kotlinx.browser.dom.AddEventListenerOptions): kotlin.Unit",
            "fun addEventListener(kotlin.String, $callback, " +
                "kotlinx.browser.dom.AddEventListenerOptions): kotlin.Unit",
            "fun addEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?, " +
                "kotlin.Boolean): kotlin.Unit",
            "fun addEventListener(kotlin.String, $callback, kotlin.Boolean): kotlin.Unit",
            "fun addEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?): kotlin.Unit",
            "fun addEventListener(kotlin.String, $callback): kotlin.Unit",
            "fun removeEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?, " +
                "kotlinx.browser.dom.EventListenerOptions): kotlin.Unit",
            "fun removeEventListener(kotlin.String, $callback, " +
                "kotlinx.browser.dom.EventListenerOptions): kotlin.Unit",
            "fun removeEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?, " +
                "kotlin.Boolean): kotlin.Unit",
            "fun removeEventListener(kotlin.String, $callback, kotlin.Boolean): kotlin.Unit",
            "fun removeEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?): kotlin.Unit",
            "fun removeEventListener(kotlin.String, $callback): kotlin.Unit",
            "fun dispatchEvent(kotlinx.browser.dom.events.Event): kotlin.Boolean",
        ).forEach { assertContains(members, it) }
        assertEquals(13, members.size)

        // The dictionaries are selected from the input file, not incidentally by these overloads.
        listOf("org.w3c.dom.AddEventListenerOptions", "org.w3c.dom.EventListenerOptions").forEach { name ->
            val dictionary = report.byName.getValue(name)
            assertEquals("input", dictionary.origin)
            assertEquals("dictionary", dictionary.kind)
        }
    }
}
