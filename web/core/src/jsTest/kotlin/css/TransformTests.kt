/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

class TransformTests {
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

    @Test
    fun perspective() = runTest {
        composition {
            Div({ style { transform(perspectiveTransform(3.cm)) } })
        }

        assertEquals("perspective(3cm)", nextChild().style.transform)
    }

    @Test
    fun rotate() = runTest {
        composition {
            Div({ style { transform(rotateTransform(3.deg)) } })
        }

        assertEquals("rotate(3deg)", nextChild().style.transform)
    }

    @Test
    fun rotate3d() = runTest {
        composition {
            Div({ style { transform(rotate3dTransform(1, 1, 0, 2.deg)) } })
        }

        assertEquals("rotate3d(1, 1, 0, 2deg)", nextChild().style.transform)
    }

    @Test
    fun rotateX() = runTest {
        composition {
            Div({ style { transform(rotateX(60.deg)) } })
            Div({ style { transform(rotateX(-0.25.turn)) } })
            Div({ style { transform(rotateX(3.14.rad)) } })
        }

        assertEquals("rotateX(60deg)", nextChild().style.transform)
        assertEquals("rotateX(-0.25turn)", nextChild().style.transform)
        assertEquals("rotateX(3.14rad)", nextChild().style.transform)
    }

    @Test
    fun rotateY() = runTest {
        composition {
            Div({ style { transform(rotateY(60.deg)) } })
            Div({ style { transform(rotateY(-0.25.turn)) } })
            Div({ style { transform(rotateY(3.14.rad)) } })
        }

        assertEquals("rotateY(60deg)", nextChild().style.transform)
        assertEquals("rotateY(-0.25turn)", nextChild().style.transform)
        assertEquals("rotateY(3.14rad)", nextChild().style.transform)
    }

    @Test
    fun rotateZ() = runTest {
        composition {
            Div({ style { transform(rotateZ(60.deg)) } })
            Div({ style { transform(rotateZ(-0.25.turn)) } })
            Div({ style { transform(rotateZ(3.14.rad)) } })
        }

        assertEquals("rotateZ(60deg)", nextChild().style.transform)
        assertEquals("rotateZ(-0.25turn)", nextChild().style.transform)
        assertEquals("rotateZ(3.14rad)", nextChild().style.transform)
    }

    @Test
    fun scale() = runTest {
        composition {
            Div({ style { transform(scaleTransform(0.6)) } })
            Div({ style { transform(scaleTransform(0.2, 0.3)) } })
        }

        assertEquals("scale(0.6)", nextChild().style.transform)
        assertEquals("scale(0.2, 0.3)", nextChild().style.transform)
    }

    @Test
    fun scale3d() = runTest {
        composition {
            Div({ style { transform(scale3dTransform(0.2, 0.3, 0.1)) } })
        }

        assertEquals("scale3d(0.2, 0.3, 0.1)", nextChild().style.transform)
    }


}