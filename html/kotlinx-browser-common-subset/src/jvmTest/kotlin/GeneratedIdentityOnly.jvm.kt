// Supplies JVM fixtures for identity-only facade types.
package kotlinx.browser.dom.references

import kotlinx.browser.webgl.ArrayBuffer
import kotlinx.browser.webgl.ArrayBufferView
import kotlinx.browser.webgl.Float32Array
import kotlinx.browser.webgl.Float64Array
import kotlinx.browser.webgl.Uint8ClampedArray

// Identity-only JVM stubs use Kotlin's synthesized no-argument constructor.

internal actual fun newArrayBuffer(): ArrayBuffer = ArrayBuffer()

internal actual fun newTypedArrayViews(): List<ArrayBufferView> = listOf(
    Uint8ClampedArray(),
    Float32Array(),
    Float64Array(),
)
