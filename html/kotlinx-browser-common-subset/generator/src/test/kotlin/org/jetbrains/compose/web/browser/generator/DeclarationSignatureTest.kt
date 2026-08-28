/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies common declaration signatures.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarationSignatureTest {
    @Test
    fun numericSequenceDifferencesAreNormalizedByJsDouble() {
        assertEquals(
            "kotlinx.browser.JsArray<kotlinx.browser.JsDouble>",
            COMMON_JS_ARRAY.parameterizedBy(COMMON_JS_DOUBLE).signature(),
        )
    }

    @Test
    fun aPropertyKeyDoesNotSpellItsMutability() {
        val mutable = property("lineWidth", mutable = true)
        val immutable = property("lineWidth", mutable = false)

        assertEquals("val lineWidth", mutable.key())
        assertEquals(mutable.key(), immutable.key())
    }
}

private fun property(name: String, mutable: Boolean): CommonProperty = CommonProperty(
    name = name,
    type = DOUBLE,
    mutable = mutable,
    open = true,
    abstractInBrowser = true,
)
