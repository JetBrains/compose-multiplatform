/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Supplies JS instances for identity-only facade types.
package kotlinx.browser.dom.references

import kotlinx.browser.webgl.ArrayBuffer
import kotlinx.browser.webgl.ArrayBufferView
import kotlinx.browser.webgl.Float32Array
import kotlinx.browser.webgl.Float64Array
import kotlinx.browser.webgl.Uint8ClampedArray

internal actual fun newArrayBuffer(): ArrayBuffer = ArrayBuffer(8)

internal actual fun newTypedArrayViews(): List<ArrayBufferView> = listOf(
    Uint8ClampedArray(1),
    Float32Array(1),
    Float64Array(1),
)
