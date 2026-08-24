/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies source shapes for mutable option-dictionary properties.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.STRING
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DictionaryPropertyEmissionTest {
    @Test
    fun commonAndWebKeepMutablePropertiesAndTypeIdentity() {
        val common = commonDictionariesFile(EVENTS, listOf(KEYWORD_INIT)).toString()
        val web = browserLeafDictionariesFile(EVENTS, listOf(KEYWORD_INIT)).toString()

        assertTrue(KEYWORD_INIT.needsIrSuppression)
        assertContains(common, "@Suppress(\"EXPECT_ACTUAL_IR_INCOMPATIBILITY\")")
        assertContains(common, "public expect interface KeywordInit : EventInit, JsAny")
        assertContains(common, "public var `is`: String?")
        assertContains(web, "public actual typealias KeywordInit = org.w3c.dom.events.KeywordInit")
        assertContains(web, "\"EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE\"")
        assertFalse("DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS" in web, web)
        assertContains(web, "param_is = param_is")
        assertContains(web, "bubbles = bubbles")
    }

    @Test
    fun compatibleInterfaceMembersDoNotTriggerBroadWebSuppressions() {
        val mixin = dictionary(
            browserPackage = DOM_EVENTS_PACKAGE,
            name = "CompatibleMixin",
            properties = listOf(property("label", STRING)),
            factoryParameters = emptyList(),
        ).copy(isDictionary = false, factory = null)

        val web = browserLeafDeclarationsFile(EVENTS, listOf(mixin), emptyList()).toString()

        assertFalse("Suppress" in web, web)
    }

    @Test
    fun jvmFactoryBuildsFreshStatefulClassWithOwnAndInheritedProperties() {
        val values = JvmStubValues(listOf(EVENT_INIT, KEYWORD_INIT).associateBy(PortableClass::portableName))
        val jvm = jvmDictionariesFile(
            EVENTS,
            listOf(KEYWORD_INIT),
            values,
            JvmConstantValues(emptyList()),
        ).toString()

        assertContains(jvm, "private class JvmKeywordInit(")
        assertContains(jvm, "override var `is`: String?")
        assertContains(jvm, "override var bubbles: Boolean?")
        assertContains(jvm, ") : KeywordInit")
        assertContains(jvm, "KeywordInit = JvmKeywordInit(param_is, bubbles)")
        assertFalse("object JvmKeywordInit" in jvm)
    }

    @Test
    fun generationFailsWhenAPropertyHasNoFactoryParameter() {
        val broken = dictionary(
            browserPackage = DOM_EVENTS_PACKAGE,
            name = "BrokenInit",
            properties = listOf(property("missing", STRING.copy(nullable = true))),
            factoryParameters = emptyList(),
        )
        val values = JvmStubValues(mapOf(broken.portableName to broken))

        val failure = assertFailsWith<IllegalStateException> {
            jvmDictionariesFile(EVENTS, listOf(broken), values, JvmConstantValues(emptyList()))
        }
        assertContains(failure.message.orEmpty(), "No factory parameter initializes")
        assertContains(failure.message.orEmpty(), ".missing")
    }
}

private val EVENTS = PortablePackageMapping(PORTABLE_EVENTS_PACKAGE, "PortableEvents", "EventDictionaries")

private fun property(name: String, type: com.squareup.kotlinpoet.TypeName): PortableProperty =
    PortableProperty(name, type, mutable = true, open = true, abstractInBrowser = false)

private fun parameter(name: String, type: com.squareup.kotlinpoet.TypeName): PortableParameter =
    PortableParameter(name, type, isVararg = false, hasDefault = true)

private fun dictionary(
    browserPackage: String,
    name: String,
    properties: List<PortableProperty>,
    factoryParameters: List<PortableParameter>,
    superinterfaces: List<ClassName> = emptyList(),
): PortableClass = PortableClass(
    browserName = ClassName(browserPackage, name),
    parentBrowserName = null,
    superinterfaces = superinterfaces,
    ancestors = emptyList(),
    shape = ClassShape.INTERFACE,
    isDictionary = true,
    isJsAny = true,
    properties = properties,
    functions = emptyList(),
    constructors = emptyList(),
    companion = null,
    factory = PortableFactory(factoryParameters),
    sourceFile = null,
)

private val EVENT_INIT = dictionary(
    browserPackage = DOM_PACKAGE,
    name = "EventInit",
    properties = listOf(property("bubbles", BOOLEAN.copy(nullable = true))),
    factoryParameters = listOf(parameter("bubbles", BOOLEAN.copy(nullable = true))),
)

private val KEYWORD_INIT = dictionary(
    browserPackage = DOM_EVENTS_PACKAGE,
    name = "KeywordInit",
    properties = listOf(property("is", STRING.copy(nullable = true))),
    factoryParameters = listOf(
        parameter("param_is", STRING.copy(nullable = true)),
        parameter("bubbles", BOOLEAN.copy(nullable = true)),
    ),
    superinterfaces = listOf(EVENT_INIT.portableName),
)
