// Verifies identity-only facade declarations.
package kotlinx.browser.dom.references

import kotlinx.browser.JsAny
import kotlinx.browser.JsArray
import kotlinx.browser.JsDouble
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsDouble
import kotlinx.browser.dom.DOMMatrixReadOnly
import kotlinx.browser.webgl.ArrayBuffer
import kotlinx.browser.webgl.ArrayBufferView
import kotlinx.browser.webgl.Float32Array
import kotlinx.browser.webgl.Float64Array
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal expect fun newArrayBuffer(): ArrayBuffer

internal expect fun newTypedArrayViews(): List<ArrayBufferView>

class GeneratedIdentityOnlyTest {
    @Test
    fun theTypedArrayHierarchyHolds() {
        val buffer: JsAny = newArrayBuffer()
        assertNotNull(buffer)

        val views = newTypedArrayViews()
        assertEquals(3, views.size)
        views.forEach { view ->
            val asView: ArrayBufferView = view
            val asJsAny: JsAny = asView
            assertNotNull(asJsAny)
        }
    }

    @Test
    fun identityOnlyReturnTypesAreCallable() {
        val values: JsArray<JsDouble> = listOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            .map(Double::toJsDouble)
            .toJsArray()

        DOMMatrixReadOnly(values).identityOnlyViews()
    }
}

private fun DOMMatrixReadOnly.identityOnlyViews(): List<ArrayBufferView> {
    val float32: Float32Array = toFloat32Array()
    val float64: Float64Array = toFloat64Array()
    return listOf(float32, float64)
}
