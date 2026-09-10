/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies complete generated coverage of the browser events input.
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Pins the generated shape of every classifier selected from `org.w3c.dom.events`.
class GeneratedEventsCompletenessTest {
    private val model = GeneratedModelReport.read()
    private val coverage = GeneratedCoverageReport.read()

    /** Instance members plus numeric companion constants, including mutable dictionary properties. */
    private val expectedMemberCounts = linkedMapOf(
        "CompositionEvent" to 5,
        "CompositionEventInit" to 1,
        "Event" to 19,
        "EventListener" to 1,
        "EventModifierInit" to 14,
        "EventTarget" to 13,
        "FocusEvent" to 5,
        "FocusEventInit" to 1,
        "InputEvent" to 6,
        "InputEventInit" to 2,
        "KeyboardEvent" to 21,
        "KeyboardEventInit" to 5,
        "MouseEvent" to 23,
        "MouseEventInit" to 8,
        "UIEvent" to 6,
        "UIEventInit" to 2,
        "WheelEvent" to 11,
        "WheelEventInit" to 4,
    )

    private val expectedDictionaryProperties = linkedMapOf(
        "CompositionEventInit" to setOf("data"),
        "EventModifierInit" to setOf(
            "ctrlKey", "shiftKey", "altKey", "metaKey", "modifierAltGraph", "modifierCapsLock",
            "modifierFn", "modifierFnLock", "modifierHyper", "modifierNumLock",
            "modifierScrollLock", "modifierSuper", "modifierSymbol", "modifierSymbolLock",
        ),
        "FocusEventInit" to setOf("relatedTarget"),
        "InputEventInit" to setOf("data", "isComposing"),
        "KeyboardEventInit" to setOf("key", "code", "location", "repeat", "isComposing"),
        "MouseEventInit" to setOf(
            "screenX", "screenY", "clientX", "clientY", "button", "buttons", "relatedTarget", "region",
        ),
        "UIEventInit" to setOf("view", "detail"),
        "WheelEventInit" to setOf("deltaX", "deltaY", "deltaZ", "deltaMode"),
    )

    private val classesWithConstructors = setOf(
        "CompositionEvent",
        "Event",
        "FocusEvent",
        "InputEvent",
        "KeyboardEvent",
        "MouseEvent",
        "UIEvent",
        "WheelEvent",
    )

    private val factoryParameterCounts = mapOf(
        "CompositionEventInit" to 6,
        "EventModifierInit" to 19,
        "FocusEventInit" to 6,
        "InputEventInit" to 7,
        "KeyboardEventInit" to 24,
        "MouseEventInit" to 27,
        "UIEventInit" to 5,
        "WheelEventInit" to 31,
    )

    @Test
    fun allEighteenClassifiersAreSelectedFromTheInputWithTheirCompleteMemberCounts() {
        val events = model.declarations.filter { it.name.substringBeforeLast('.') == EVENTS_PACKAGE }

        assertEquals(expectedMemberCounts.keys, events.map { it.simpleName }.toSet())
        events.forEach { declaration ->
            assertEquals("input", declaration.origin, "${declaration.name} is not selected from the input")
            assertEquals(
                expectedMemberCounts.getValue(declaration.simpleName),
                declaration.memberCount,
                "${declaration.name} has the wrong generated member surface",
            )
        }
        assertEquals(147, events.sumOf { it.memberCount })
    }

    @Test
    fun allEightEventConstructorsAreEmitted() {
        expectedMemberCounts.keys.forEach { simpleName ->
            val constructors = event(simpleName).constructors
            if (simpleName in classesWithConstructors) {
                assertEquals(1, constructors.size, "$simpleName should have one constructor")
                assertTrue(constructors.single().startsWith("primary constructor(kotlin.String, "))
            } else {
                assertEquals(emptyList(), constructors, "$simpleName should not declare a constructor")
            }
        }
    }

    @Test
    fun eventTargetHasBothListenerFormsAndDispatch() {
        val members = event("EventTarget").members
        val callback = "((kotlinx.browser.dom.events.Event) -> kotlin.Unit)?"

        assertEquals(13, members.size)
        assertEquals(6, members.count { it.startsWith("fun addEventListener(") })
        assertEquals(6, members.count { it.startsWith("fun removeEventListener(") })
        assertTrue(members.any { callback in it }, "the callback overloads are absent")
        assertTrue(
            members.any { "kotlinx.browser.dom.events.EventListener?" in it },
            "the EventListener overloads are absent",
        )
        assertContains(
            members,
            "fun dispatchEvent(kotlinx.browser.dom.events.Event): kotlin.Boolean",
        )
    }

    @Test
    fun mouseEventKeepsItsUnionMarkerSuperinterface() {
        val marker = model.byName.getValue("org.w3c.dom.UnionElementOrMouseEvent")

        assertEquals("input", marker.origin)
        assertEquals(0, marker.memberCount)
        assertContains(event("MouseEvent").superinterfaces, "UnionElementOrMouseEvent")
    }

    @Test
    fun allConstantsAndAllFactoryParametersAreEmitted() {
        val eventCoverage = coverage.entries.filter { it.subject.startsWith("$EVENTS_PACKAGE.") }
        val constants = eventCoverage.filter { it.ported && it.kind == "companion-member" }
        val factories = eventCoverage.filter { it.ported && it.kind == "factory" }
        val parameters = eventCoverage.filter { it.ported && it.kind == "parameter" }

        assertEquals(39, constants.size)
        assertEquals(factoryParameterCounts.keys, factories.map(::ownerSimpleName).toSet())
        assertEquals(125, parameters.size)
        factoryParameterCounts.forEach { (factory, expected) ->
            assertEquals(
                expected,
                parameters.count { ownerSimpleName(it) == factory },
                "$factory has the wrong generated parameter surface",
            )
        }
    }

    @Test
    fun everyEventDictionaryPropertyIsPortedAndThereAreNoEventOmissions() {
        val properties = coverage.entries
            .filter { it.ported && it.kind == "member" && "#var " in it.subject }
            .filter { ownerSimpleName(it) in expectedDictionaryProperties }
            .groupBy(::ownerSimpleName)
            .mapValues { (_, entries) -> entries.map { it.subject.substringAfter("#var ") }.toSet() }

        assertEquals(expectedDictionaryProperties, properties)

        val skipped = coverage.entries.filter {
            !it.ported && it.subject.startsWith("$EVENTS_PACKAGE.")
        }
        assertEquals(emptyList(), skipped, "the manually selected events file has undeclared omissions")
    }

    private fun event(simpleName: String): GeneratedModelReport.Declaration =
        model.byName.getValue("$EVENTS_PACKAGE.$simpleName")

    private fun ownerSimpleName(entry: GeneratedCoverageReport.Entry): String =
        entry.subject.substringBefore('#').substringAfterLast('.')

    private companion object {
        const val EVENTS_PACKAGE = "org.w3c.dom.events"
    }
}
