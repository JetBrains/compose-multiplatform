/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies JVM hierarchy and behavior for identity-only facade types.
package kotlinx.browser.dom.references

import kotlinx.browser.JsAny
import kotlinx.browser.webgl.ArrayBuffer
import kotlinx.browser.webgl.ArrayBufferView
import kotlinx.browser.webgl.Float32Array
import kotlinx.browser.webgl.Float64Array
import kotlinx.browser.webgl.Uint8ClampedArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Verifies compiled identity-only stubs remain opaque to consumers.
class GeneratedIdentityOnlyJvmTest {
    @Test
    fun theStubsDeclareNoMembersOfTheirOwn() {
        IDENTITY_ONLY.forEach { type ->
            assertEquals(emptyList(), type.declaredFields.map { it.name }, "$type declares fields")
            assertEquals(emptyList(), type.declaredMethods.map { it.name }, "$type declares methods")
        }
    }

    /** The subtype edges the facade does keep, which is the only structure it promises. */
    @Test
    fun theStubsKeepTheirDeclaredSupertypes() {
        assertTrue(JsAny::class.java.isAssignableFrom(ArrayBuffer::class.java))
        assertTrue(JsAny::class.java.isAssignableFrom(ArrayBufferView::class.java))
        listOf(Uint8ClampedArray::class.java, Float32Array::class.java, Float64Array::class.java).forEach { view ->
            assertTrue(ArrayBufferView::class.java.isAssignableFrom(view), "$view is not an ArrayBufferView")
        }
        // ... and nothing else. `BufferDataSource` is a browser supertype of both, and unlisted, so it
        // is not emitted and the facade does not claim it.
        assertEquals(
            listOf("kotlinx.browser.webgl.ArrayBufferView", "kotlinx.browser.JsAny"),
            Float32Array::class.java.interfaces.map { it.name },
        )
    }

    /** The stub is instantiable on its own, which is what lets it stand in for a browser value. */
    @Test
    fun theStubsCanBeBuiltFromNothing() {
        assertEquals(3, newTypedArrayViews().size)
    }
}

private val IDENTITY_ONLY = listOf(
    ArrayBuffer::class.java,
    ArrayBufferView::class.java,
    Float32Array::class.java,
    Float64Array::class.java,
    Uint8ClampedArray::class.java,
)
