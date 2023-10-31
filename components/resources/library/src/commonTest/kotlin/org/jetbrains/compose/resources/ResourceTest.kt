/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ResourceTest {
    @Test
    fun testResourceEquals() = runBlockingTest {
        assertEquals(getPathById("a"), getPathById("a"))
    }

    @Test
    fun testResourceNotEquals() = runBlockingTest {
        assertNotEquals(getPathById("a"), getPathById("b"))
    }
}
