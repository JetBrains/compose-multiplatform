/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FailingTestCases {

    @Test
    fun externalInterfaceSimulatingEnum() {
        var expectedErrorThrown = false
        try {
            // Doesn't throw in kotlin 1.5.10.
            // Since kotlin 1.5.20 (and 1.5.21) throws `ReferenceError: MyI is not defined`
            useMyI(MyI.Value1)
        } catch (e: Throwable) {
            expectedErrorThrown = true
        }
        // No exception in Kotlin 2.1.0
        assertFalse(expectedErrorThrown)
    }
}

@Suppress("Unused", "NOTHING_TO_INLINE", "NESTED_CLASS_IN_EXTERNAL_INTERFACE", "INLINE_EXTERNAL_DECLARATION", "WRONG_BODY_OF_EXTERNAL_DECLARATION", "NESTED_EXTERNAL_DECLARATION", "ClassName")
private external interface MyI {
    companion object {
        inline val Value1 get() = MyI("value1")
        inline val Value2 get() = MyI("value2")
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun MyI(value: String) = value.unsafeCast<MyI>()

private fun useMyI(myI: MyI) {
    println("Using MyI = " + myI.unsafeCast<String>())
}
