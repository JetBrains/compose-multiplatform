/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalResourceApi::class)
class ResourceTest {
    @Test
    fun testResourceEquals() {
        assertEquals(resource("a"), resource("a"))
    }

    @Test
    fun testResourceNotEquals() {
        assertNotEquals(resource("a"), resource("b"))
    }
}
