/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies nested generic and callback types in signatures, actuals, and JVM stubs.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecursiveTypeTest {
    @Test
    fun signatureFoldsAWrappedCallbackOntoOneLine() {
        // KotlinPoet renders this type across lines, but ledger signatures must remain single-line.
        assertTrue(
            '\n' in ON_ERROR.toString(),
            "KotlinPoet no longer wraps the widest browser callback; signature() may be redundant",
        )
        assertEquals(
            "((kotlinx.browser.JsAny?,kotlin.String,kotlin.Int,kotlin.Int,kotlinx.browser.JsAny?)" +
                " -> kotlinx.browser.JsAny?)?",
            ON_ERROR.signature(),
        )
        assertFalse('\n' in ON_ERROR.signature())
    }

    /** Keeps callback and listener overload keys distinct. */
    @Test
    fun signatureKeysSeparateACallbackOverloadFromItsListenerOverload() {
        val listener = addEventListener(EVENT_LISTENER.copy(nullable = true))
        val callback = addEventListener(EVENT_HANDLER)

        assertEquals(
            "fun addEventListener(kotlin.String, kotlinx.browser.dom.events.EventListener?)",
            listener.key(),
        )
        assertEquals(
            "fun addEventListener(kotlin.String, ((kotlinx.browser.dom.events.Event) -> kotlin.Unit)?)",
            callback.key(),
        )
        assertNotEquals(listener.key(), callback.key())
    }

    /** Finds interop types nested in callbacks before choosing an actual's source set. */
    @Test
    fun interopIsFoundThroughEveryPartOfACallback() {
        assertTrue(MUTATION_CALLBACK.mentionsInterop(), "a generic nested in a callback parameter")
        assertTrue(ON_ERROR.mentionsInterop(), "an interop type as a callback parameter and result")
        assertTrue(
            LambdaTypeName.get(receiver = PORTABLE_JS_STRING, returnType = UNIT).mentionsInterop(),
            "an interop type as a callback receiver",
        )
        assertFalse(EVENT_HANDLER.mentionsInterop(), "a callback over facade classifiers alone")
    }

    /** Finds opaque promises nested within callbacks. */
    @Test
    fun anOpaquePromiseIsFoundInsideACallback() {
        val handler = LambdaTypeName
            .get(parameters = listOf(ParameterSpec.unnamed(PORTABLE_PROMISE.parameterizedBy(STAR))), returnType = UNIT)
            .copy(nullable = true)

        assertTrue(portableClass(functions = listOf(consume(handler))).needsIrSuppression)
        assertFalse(portableClass(functions = listOf(consume(EVENT_HANDLER))).needsIrSuppression)
    }

    /** Builds inert callbacks that ignore parameters and return inert results. */
    @Test
    fun inertCallbacksIgnoreTheirParametersAndReturnAnInertResult() {
        assertEquals("null", inertLiteral(EVENT_HANDLER).toString())
        assertEquals("{ _, _ -> }", inertLiteral(MUTATION_CALLBACK).toString())
        assertEquals("{ }", inertLiteral(LambdaTypeName.get(returnType = UNIT)).toString())
        assertEquals(
            "{ _ -> 0 }",
            inertLiteral(
                LambdaTypeName.get(parameters = listOf(ParameterSpec.unnamed(NODE)), returnType = SHORT),
            ).toString(),
        )
        assertEquals(
            "{ null }",
            inertLiteral(LambdaTypeName.get(returnType = PORTABLE_JS_ANY.copy(nullable = true))).toString(),
        )
    }

    /** Defers facade return values to [JvmStubValues] singleton support. */
    @Test
    fun aCallbackReturningAFacadeClassifierAsksForTheSingleton() {
        val nodeList = portableClass(name = "NodeList")
        val values = JvmStubValues(mapOf(nodeList.portableName to nodeList))
        val callback = LambdaTypeName.get(
            parameters = listOf(ParameterSpec.unnamed(NODE)),
            returnType = nodeList.portableName,
        )

        assertNull(inertLiteral(callback, values.classes))
        assertEquals("{ _ -> kotlinx.browser.dom.EmptyNodeList }", values.value(callback).toString())
        val singletonFile = values.singletonFiles().single()
        assertEquals(PORTABLE_DOM_PACKAGE, singletonFile.packageName)
        assertEquals("JvmStubValues", singletonFile.name)
        assertTrue("internal object EmptyNodeList : NodeList()" in singletonFile.toString())
    }
}

private val NULLABLE_JS_ANY = PORTABLE_JS_ANY.copy(nullable = true)
private val EVENT = ClassName(PORTABLE_EVENTS_PACKAGE, "Event")
private val EVENT_LISTENER = ClassName(PORTABLE_EVENTS_PACKAGE, "EventListener")
private val NODE = ClassName(PORTABLE_DOM_PACKAGE, "Node")
private val MUTATION_OBSERVER = ClassName(PORTABLE_DOM_PACKAGE, "MutationObserver")
private val MUTATION_RECORD = ClassName(PORTABLE_DOM_PACKAGE, "MutationRecord")

/** `((Event) -> Unit)?`, the shape of every event handler property. */
private val EVENT_HANDLER = LambdaTypeName
    .get(parameters = listOf(ParameterSpec.unnamed(EVENT)), returnType = UNIT)
    .copy(nullable = true)

/** `(JsArray<MutationRecord>, MutationObserver) -> Unit`: a generic nested inside a callback. */
private val MUTATION_CALLBACK = LambdaTypeName.get(
    parameters = listOf(
        ParameterSpec.unnamed(PORTABLE_JS_ARRAY.parameterizedBy(MUTATION_RECORD)),
        ParameterSpec.unnamed(MUTATION_OBSERVER),
    ),
    returnType = UNIT,
)

/** `GlobalEventHandlers.onerror`, the widest callback the browser sources declare. */
private val ON_ERROR = LambdaTypeName
    .get(
        parameters = listOf(NULLABLE_JS_ANY, STRING, INT, INT, NULLABLE_JS_ANY).map(ParameterSpec::unnamed),
        returnType = NULLABLE_JS_ANY,
    )
    .copy(nullable = true)

private fun addEventListener(callback: TypeName): PortableFunction = PortableFunction(
    name = "addEventListener",
    parameters = listOf(parameter("type", STRING), parameter("callback", callback)),
    returnType = UNIT,
    open = false,
    abstractInBrowser = false,
)

private fun consume(type: TypeName): PortableFunction = PortableFunction(
    name = "consume",
    parameters = listOf(parameter("value", type)),
    returnType = UNIT,
    open = false,
    abstractInBrowser = false,
)

private fun parameter(name: String, type: TypeName): PortableParameter =
    PortableParameter(name = name, type = type, isVararg = false, hasDefault = false)

private fun portableClass(
    name: String = "Probe",
    functions: List<PortableFunction> = emptyList(),
): PortableClass = PortableClass(
    browserName = ClassName(DOM_PACKAGE, name),
    parentBrowserName = null,
    superinterfaces = emptyList(),
    ancestors = emptyList(),
    shape = ClassShape.ABSTRACT,
    isDictionary = false,
    isJsAny = true,
    properties = emptyList(),
    functions = functions,
    constructors = emptyList(),
    companion = null,
    factory = null,
    sourceFile = null,
)
