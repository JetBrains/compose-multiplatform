/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies common behavior of generated interop wrappers.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.browser.JsArray
import kotlinx.browser.JsDouble
import kotlinx.browser.JsString
import kotlinx.browser.dom.MutationObserverInit
import kotlinx.browser.get
import kotlinx.browser.length
import kotlinx.browser.set
import kotlinx.browser.toDouble
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsDouble
import kotlinx.browser.toJsNumber
import kotlinx.browser.toJsString
import kotlinx.browser.toKotlinString
import kotlinx.browser.toKotlinDouble
import kotlinx.browser.toList

// Exercises the generated interop bridges on every target.
class GeneratedInteropTest {
    @Test
    fun scalarBridgesRoundTrip() {
        assertEquals("common", "common".toJsString().toKotlinString())
        assertEquals(42.5, 42.5.toJsNumber().toDouble())
        assertEquals(42.5, 42.5.toJsDouble().toKotlinDouble())
    }

    @Test
    fun jsArrayBridgeSupportsCommonElements() {
        val values: JsArray<JsString> = listOf("first".toJsString(), "second".toJsString()).toJsArray()

        assertEquals(2, values.length)
        assertEquals("first", values[0]?.toKotlinString())
        values[1] = "changed".toJsString()
        assertEquals(listOf("first", "changed"), values.toList().map { it.toKotlinString() })
        assertNull(values[2])
    }

    @Test
    fun numericSequenceUsesItsTargetSpecificElementType() {
        val values: JsArray<JsDouble> = listOf(1.25.toJsDouble(), 2.5.toJsDouble()).toJsArray()

        assertEquals(listOf(1.25, 2.5), values.toList().map { it.toKotlinDouble() })
    }

    // Verifies each target-specific dictionary factory forwards its nested `JsArray<JsString>`.
    @Test
    fun nestedGenericDictionaryFactoryForwardsOnEveryTarget() {
        val filter: JsArray<JsString> = listOf("class".toJsString(), "style".toJsString()).toJsArray()

        assertEquals(listOf("class", "style"), filter.toList().map { it.toKotlinString() })
        val init = MutationObserverInit(childList = true, subtree = true, attributeFilter = filter)

        assertNotNull(init)
        assertEquals(true, init.childList)
        assertEquals(true, init.subtree)
        assertEquals(listOf("class", "style"), init.attributeFilter?.toList()?.map { it.toKotlinString() })

        init.childList = false
        init.attributeFilter = listOf("id".toJsString()).toJsArray()

        assertEquals(false, init.childList)
        assertEquals(listOf("id"), init.attributeFilter?.toList()?.map { it.toKotlinString() })
        assertNull(MutationObserverInit().childList)
    }
}
