/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals

class HSVTest {

    @Test
    fun testGreenToHsv() {
        val greenRgb = Color(0xff00ff00)
        val result = greenRgb.toHsv()
        assertEquals(HSV(120f, 1f, 1f), result)
        assertEquals(greenRgb, result.toRgb())
    }

}
