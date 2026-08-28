/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies target-specific constructor emission and inert JVM behavior.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConstructorEmissionTest {
    /** Preserves browser constructor placement and defaults in common declarations. */
    @Test
    fun commonDeclarationsRepeatTheBrowserConstructor() {
        val common = commonFile(EVENT, TEXT, PATH_2D, DOM_POINT)

        assertContains(
            common,
            """
            |public expect open class Event(
            |  type: String,
            |  eventInitDict: EventInit = definedExternally,
            |) : JsAny {
            """.trimMargin(),
        )
        assertContains(common, "public expect open class Path2D() : JsAny {\n  public constructor(d: String)")
        // No primary at all upstream, so the facade declares none either.
        assertContains(common, "public expect open class DOMPoint : JsAny {\n  public constructor(")
    }

    /** Emits inert JVM defaults and delegates secondary constructors to the primary. */
    @Test
    fun jvmActualsCarryInertDefaultsAndDelegate() {
        val jvm = jvmFile(EVENT_INIT, EVENT, PATH_2D)

        assertContains(
            jvm,
            """
            |public actual open class Event actual constructor(
            |  type: String,
            |  eventInitDict: EventInit = EventInit(),
            |) : JsAny {
            """.trimMargin(),
        )
        assertContains(jvm, "public actual constructor(d: String) : this()")
        // The expect declares the defaults, so the actual repeating them needs the suppression.
        assertContains(jvm, """@file:Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")""")
    }

    /** Promotes one secondary constructor when the browser declares no primary. */
    @Test
    fun aClassWithoutAPrimaryConstructorGetsOneSynthesizedOnJvm() {
        val jvm = jvmFile(DOM_POINT_INIT, DOM_POINT)

        assertContains(
            jvm,
            """
            |public actual open class DOMPoint actual constructor(
            |  point: DOMPointInit,
            |) : JsAny {
            |  public actual constructor(x: Double = 0.0) : this(DOMPointInit())
            """.trimMargin(),
        )
    }

    /** Adds a protected no-argument constructor only when JVM subclasses need one. */
    @Test
    fun theProtectedFallbackConstructorIsAddedOnlyWhereItIsNeeded() {
        assertTrue(EVENT.needsJvmFallbackConstructor, "Event(type) cannot be called without arguments")
        assertFalse(TEXT.needsJvmFallbackConstructor, "Text() can, because data defaults")
        assertFalse(PATH_2D.needsJvmFallbackConstructor, "Path2D() is declared")
        assertFalse(NODE.needsJvmFallbackConstructor, "Node declares no constructor and kept the synthesized one")

        val jvm = jvmFile(EVENT_INIT, EVENT, UI_EVENT, TEXT)

        assertContains(jvm, "protected constructor() : this(\"\", EventInit())")
        // The subclass reaches its parent's, and needs one of its own for the same reason.
        assertContains(jvm, "public actual open class UIEvent actual constructor(\n  type: String,\n) : Event()")
        assertContains(jvm, "protected constructor() : this(\"\")")
        assertFalse("protected constructor() : this(\"hello\")" in jvm)
        assertEquals(2, Regex("protected constructor").findAll(jvm).count())
    }

    /** Assigns JVM constructor parameters only to matching mutable properties. */
    @Test
    fun aJvmConstructorStoresOnlyWhatMatchesAMutableProperty() {
        val jvm = jvmFile(EVENT_INIT, EVENT, CHARACTER_DATA, TEXT)

        assertContains(
            jvm,
            """
            |  init {
            |    this.`data` = `data`
            |  }
            """.trimMargin(),
        )
        assertEquals(1, Regex("init \\{").findAll(jvm).count(), "only Text stores its argument")
        assertFalse("this.type = type" in jvm)
    }

    /** The web actual is a typealias, so the browser's own constructor is the one common code reaches. */
    @Test
    fun webActualsEmitNoConstructorOfTheirOwn() {
        val web = webFile(EVENT, TEXT, PATH_2D)

        // The browser classifier of the same simple name, reached through the file's own import.
        assertContains(web, "import org.w3c.dom.events.Event")
        assertContains(web, "public actual typealias Event = Event")
        assertFalse("constructor" in web)
        // A constructor default is a default on the expect side too, and this is what allows it.
        assertContains(web, """"DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS"""")
    }

    /** Keeps concrete implementations of common interface members explicit on common and JVM. */
    @Test
    fun interfaceImplementationsAreEmittedAsOverrides() {
        val contract = commonClass(
            DOM_EVENTS_PACKAGE,
            "PathContract",
            shape = ClassShape.INTERFACE,
            functions = listOf(function("closePath")),
        )
        val implementation = commonClass(
            DOM_EVENTS_PACKAGE,
            "ConcretePath",
            superinterfaces = listOf(contract.commonName),
            functions = listOf(function("closePath", overrides = true)),
        )

        val common = commonFile(contract, implementation)
        val jvm = jvmFile(contract, implementation)

        assertContains(common, "override fun closePath()")
        assertContains(jvm, "actual override fun closePath()")
        assertEquals(2, Regex("fun closePath").findAll(jvm).count(), "one interface and one class member")
    }

    /** Compiles the freshly generated common facade before this Gradle test task can run. */
    @Test
    fun generatedCommonFacadeCompilesInterfaceOverrides() {
        val common = requiredGradleTestFile("commonDomSource").readText()
        val metadata = requiredGradleTestFile("commonDomMetadata")

        assertContains(
            common,
            """
            |public expect open class Path2D() : CanvasPath, JsAny {
            |  public constructor(path: Path2D)
            """.trimMargin(),
        )
        assertContains(common, "override fun closePath()")
        assertContains(common, "override var contentEditable: String")
        assertTrue(metadata.isFile, "compiled common metadata is missing: $metadata")
    }

    /** Uses a singleton when required constructor arguments prevent direct JVM instantiation. */
    @Test
    fun inertValuesFallBackToTheSingletonWhereAConstructorDemandsArguments() {
        val classes = listOf(EVENT, TEXT, NODE).associateBy(CommonClass::commonName)

        assertNull(inertLiteral(EVENT.commonName, classes))
        assertEquals(
            "kotlinx.browser.dom.events.EmptyEvent",
            JvmStubValues(classes).value(EVENT.commonName).toString(),
        )
        // Both of these can still be built directly.
        assertEquals("kotlinx.browser.dom.Text()", inertLiteral(TEXT.commonName, classes).toString())
        assertEquals("kotlinx.browser.dom.Node()", inertLiteral(NODE.commonName, classes).toString())
    }
}

private val EVENTS = CommonPackageMapping(COMMON_EVENTS_PACKAGE, "Events", "EventDictionaries")

private fun requiredGradleTestFile(property: String): File {
    val path = System.getProperty(property)
        ?: error("Missing -D$property; run this test with the Gradle test runner")
    return File(path).also { check(it.isFile) { "Missing test input: $it" } }
}

private fun commonFile(vararg declarations: CommonClass): String =
    commonDeclarationsFile(EVENTS, declarations.toList(), emptyList()).toString()

private fun webFile(vararg declarations: CommonClass): String =
    browserLeafDeclarationsFile(EVENTS, declarations.toList(), emptyList()).toString()

private fun jvmFile(vararg declarations: CommonClass): String {
    val values = JvmStubValues(declarations.associateBy(CommonClass::commonName))
    val constants = JvmConstantValues(declarations.flatMap { it.companion?.properties.orEmpty() })
    return jvmDeclarationsFile(EVENTS, declarations.toList(), emptyList(), values, constants).toString()
}

private fun parameter(name: String, type: TypeName, hasDefault: Boolean = false): CommonParameter =
    CommonParameter(name = name, type = type, isVararg = false, hasDefault = hasDefault)

private fun property(name: String, type: TypeName, mutable: Boolean): CommonProperty =
    CommonProperty(name = name, type = type, mutable = mutable, open = true, abstractInBrowser = false)

private fun function(name: String, overrides: Boolean = false): CommonFunction = CommonFunction(
    name = name,
    parameters = emptyList(),
    returnType = UNIT,
    open = true,
    abstractInBrowser = false,
    overrides = overrides,
)

private fun commonClass(
    packageName: String,
    name: String,
    parent: CommonClass? = null,
    shape: ClassShape = ClassShape.OPEN,
    isDictionary: Boolean = false,
    properties: List<CommonProperty> = emptyList(),
    functions: List<CommonFunction> = emptyList(),
    superinterfaces: List<ClassName> = emptyList(),
    constructors: List<CommonConstructor> = emptyList(),
    factory: CommonFactory? = null,
): CommonClass = CommonClass(
    browserName = ClassName(packageName, name),
    parentBrowserName = parent?.browserName,
    superinterfaces = superinterfaces,
    ancestors = parent?.let { listOf(it.commonName) + it.ancestors }.orEmpty(),
    shape = shape,
    isDictionary = isDictionary,
    isJsAny = true,
    properties = properties,
    functions = functions,
    constructors = constructors,
    companion = null,
    factory = factory,
    sourceFile = null,
)

/** `EventInit`, the option dictionary `Event` defaults its second parameter to. */
private val EVENT_INIT = commonClass(
    DOM_EVENTS_PACKAGE,
    "EventInit",
    shape = ClassShape.INTERFACE,
    isDictionary = true,
    factory = CommonFactory(listOf(parameter("bubbles", com.squareup.kotlinpoet.BOOLEAN, hasDefault = true))),
)

private val EVENT = commonClass(
    DOM_EVENTS_PACKAGE,
    "Event",
    properties = listOf(property("type", STRING, mutable = false)),
    constructors = listOf(
        CommonConstructor(
            parameters = listOf(
                parameter("type", STRING),
                parameter("eventInitDict", EVENT_INIT.commonName, hasDefault = true),
            ),
            primary = true,
        ),
    ),
)

private val UI_EVENT = commonClass(
    DOM_EVENTS_PACKAGE,
    "UIEvent",
    parent = EVENT,
    constructors = listOf(CommonConstructor(listOf(parameter("type", STRING)), primary = true)),
)

private val CHARACTER_DATA = commonClass(
    DOM_PACKAGE,
    "CharacterData",
    shape = ClassShape.ABSTRACT,
    properties = listOf(property("data", STRING, mutable = true)),
)

private val TEXT = commonClass(
    DOM_PACKAGE,
    "Text",
    parent = CHARACTER_DATA,
    constructors = listOf(
        CommonConstructor(listOf(parameter("data", STRING, hasDefault = true)), primary = true),
    ),
)

private val PATH_2D = commonClass(
    DOM_PACKAGE,
    "Path2D",
    constructors = listOf(
        CommonConstructor(emptyList(), primary = true),
        CommonConstructor(listOf(parameter("d", STRING)), primary = false),
    ),
)

private val DOM_POINT_INIT = commonClass(
    DOM_PACKAGE,
    "DOMPointInit",
    shape = ClassShape.INTERFACE,
    isDictionary = true,
    factory = CommonFactory(listOf(parameter("x", DOUBLE, hasDefault = true))),
)

/** `DOMPoint`, which the browser declares with two secondary constructors and no primary. */
private val DOM_POINT = commonClass(
    DOM_PACKAGE,
    "DOMPoint",
    constructors = listOf(
        CommonConstructor(listOf(parameter("point", DOM_POINT_INIT.commonName)), primary = false),
        CommonConstructor(listOf(parameter("x", DOUBLE, hasDefault = true)), primary = false),
    ),
)

/** A classifier with no constructor at all, which keeps the one Kotlin synthesizes. */
private val NODE = commonClass(DOM_PACKAGE, "Node")
