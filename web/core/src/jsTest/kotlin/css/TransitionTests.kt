/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.matrix3dTransform
import org.jetbrains.compose.web.css.matrixTransform
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

class TransitionTests {
    @Test
    fun matrix() = runTest {
        composition {
            Div({ style { transform(matrixTransform(1, 2, -1, 1, 80, 80)) } })
        }

        assertEquals("matrix(1, 2, -1, 1, 80, 80)", nextChild().style.transform)
    }

    @Test
    fun matrix3d() = runTest {
        composition {
            Div({ style { transform(matrix3dTransform(1, 0, 0, 0, 0, 1, 6, 0, 0, 0, 1, 0, 50, 100, 0, 1.1)) } })
        }

        assertEquals("matrix3d(1, 0, 0, 0, 0, 1, 6, 0, 0, 0, 1, 0, 50, 100, 0, 1.1)", nextChild().style.transform)
    }

}