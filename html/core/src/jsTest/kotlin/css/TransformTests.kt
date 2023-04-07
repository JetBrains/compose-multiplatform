/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalComposeWebApi
class TransformTests {
    @Test
    fun matrix() = runTest {
        composition {
            Div({ style { transform { matrix(1, 2, -1, 1, 80, 80) } } })
        }

        assertEquals("matrix(1, 2, -1, 1, 80, 80)", nextChild().style.transform)
    }

    @Test
    fun matrix3d() = runTest {
        composition {
            Div({ style { transform { matrix3d(1, 0, 0, 0, 0, 1, 6, 0, 0, 0, 1, 0, 50, 100, 0, 1.1) } } })
        }

        assertEquals("matrix3d(1, 0, 0, 0, 0, 1, 6, 0, 0, 0, 1, 0, 50, 100, 0, 1.1)", nextChild().style.transform)
    }

    @Test
    fun perspective() = runTest {
        composition {
            Div({ style { transform { perspective(3.cm) } } })
        }

        assertEquals("perspective(3cm)", nextChild().style.transform)
    }

    @Test
    fun rotate() = runTest {
        composition {
            Div({ style { transform { rotate(3.deg) } } })
        }

        assertEquals("rotate(3deg)", nextChild().style.transform)
    }

    @Test
    fun rotate3d() = runTest {
        composition {
            Div({ style { transform { rotate3d(1, 1, 0, 2.deg) } } })
        }

        assertEquals("rotate3d(1, 1, 0, 2deg)", nextChild().style.transform)
    }

    @Test
    fun rotateX() = runTest {
        composition {
            Div({ style { transform { rotateX(60.deg) } } })
            Div({ style { transform { rotateX(-0.25.turn) } } })
            Div({ style { transform { rotateX(3.14.rad) } } })
        }

        assertEquals("rotateX(60deg)", nextChild().style.transform)
        assertEquals("rotateX(-0.25turn)", nextChild().style.transform)
        assertEquals("rotateX(3.14rad)", nextChild().style.transform)
    }

    @Test
    fun rotateY() = runTest {
        composition {
            Div({ style { transform { rotateY(60.deg) } } })
            Div({ style { transform { rotateY(-0.25.turn) } } })
            Div({ style { transform { rotateY(3.14.rad) } } })
        }

        assertEquals("rotateY(60deg)", nextChild().style.transform)
        assertEquals("rotateY(-0.25turn)", nextChild().style.transform)
        assertEquals("rotateY(3.14rad)", nextChild().style.transform)
    }

    @Test
    fun rotateZ() = runTest {
        composition {
            Div({ style { transform { rotateZ(60.deg) } } })
            Div({ style { transform { rotateZ(-0.25.turn) } } })
            Div({ style { transform { rotateZ(3.14.rad) } } })
        }

        assertEquals("rotateZ(60deg)", nextChild().style.transform)
        assertEquals("rotateZ(-0.25turn)", nextChild().style.transform)
        assertEquals("rotateZ(3.14rad)", nextChild().style.transform)
    }

    @Test
    fun scale() = runTest {
        composition {
            Div({ style { transform { scale(0.6) } } })
            Div({ style { transform { scale(0.2, 0.3) } } })
        }

        assertEquals("scale(0.6)", nextChild().style.transform)
        assertEquals("scale(0.2, 0.3)", nextChild().style.transform)
    }

    @Test
    fun scale3d() = runTest {
        composition {
            Div({ style { transform { scale3d(0.2, 0.3, 0.1) } } })
        }

        assertEquals("scale3d(0.2, 0.3, 0.1)", nextChild().style.transform)
    }

    @Test
    fun scaleX() = runTest {
        composition {
            Div({ style { transform { scaleX(0.5) } } })
        }

        assertEquals("scaleX(0.5)", nextChild().style.transform)
    }

    @Test
    fun scaleY() = runTest {
        composition {
            Div({ style { transform { scaleY(0.7) } } })
        }

        assertEquals("scaleY(0.7)", nextChild().style.transform)
    }

    @Test
    fun scaleZ() = runTest {
        composition {
            Div({ style { transform { scaleZ(0.12) } } })
        }

        assertEquals("scaleZ(0.12)", nextChild().style.transform)
    }

    @Test
    fun skew() = runTest {
        composition {
            Div({ style { transform { skew(2.deg) } } })
            Div({ style { transform { skew(1.rad, 2.deg) } } })
        }

        assertEquals("skew(2deg)", nextChild().style.transform)
        assertEquals("skew(1rad, 2deg)", nextChild().style.transform)
    }

    @Test
    fun skewX() = runTest {
        composition {
            Div({ style { transform { skewX(2.deg) } } })
        }

        assertEquals("skewX(2deg)", nextChild().style.transform)
    }

    @Test
    fun skewY() = runTest {
        composition {
            Div({ style { transform { skewY(2.rad) } } })
        }

        assertEquals("skewY(2rad)", nextChild().style.transform)
    }

    @Test
    fun translate() = runTest {
        composition {
            Div({ style { transform { translate(10.px) } } })
            Div({ style { transform { translate(4.percent) } } })
            Div({ style { transform { translate(2.percent, 10.px) } } })
            Div({ style { transform { translate(10.px, 3.percent) } } })
            Div({ style { transform { translate(20.px, 10.px) } } })
            Div({ style { transform { translate(5.percent, 8.percent) } } })
        }

        assertEquals("translate(10px)", nextChild().style.transform)
        assertEquals("translate(4%)", nextChild().style.transform)
        assertEquals("translate(2%, 10px)", nextChild().style.transform)
        assertEquals("translate(10px, 3%)", nextChild().style.transform)
        assertEquals("translate(20px, 10px)", nextChild().style.transform)
        assertEquals("translate(5%, 8%)", nextChild().style.transform)
    }

    @Test
    fun translate3d() = runTest {
        composition {
            Div({ style { transform { translate3d(2.percent, 10.px, 1.em) } } })
            Div({ style { transform { translate3d(10.px, 3.percent, 2.em) } } })
            Div({ style { transform { translate3d(20.px, 10.px, 3.em) } } })
            Div({ style { transform { translate3d(5.percent, 8.percent, 4.em) } } })
        }

        assertEquals("translate3d(2%, 10px, 1em)", nextChild().style.transform)
        assertEquals("translate3d(10px, 3%, 2em)", nextChild().style.transform)
        assertEquals("translate3d(20px, 10px, 3em)", nextChild().style.transform)
        assertEquals("translate3d(5%, 8%, 4em)", nextChild().style.transform)
    }


    @Test
    fun translateX() = runTest {
        composition {
            Div({ style { transform { translateX(10.px) } } })
            Div({ style { transform { translateX(4.percent) } } })
        }

        assertEquals("translateX(10px)", nextChild().style.transform)
        assertEquals("translateX(4%)", nextChild().style.transform)
    }

    @Test
    fun translateY() = runTest {
        composition {
            Div({ style { transform { translateY(12.px) } } })
            Div({ style { transform { translateY(3.percent) } } })
        }

        assertEquals("translateY(12px)", nextChild().style.transform)
        assertEquals("translateY(3%)", nextChild().style.transform)
    }

    @Test
    fun translateZ() = runTest {
        composition {
            Div({ style { transform { translateZ(7.px) } } })
        }

        assertEquals("translateZ(7px)", nextChild().style.transform)
    }

    @Test
    fun mutliples() = runTest {
        composition {
            Div({
                style {
                    transform {
                        perspective(3.cm)
                        translate(10.px, 3.px)
                        rotateY(3.deg)
                    }
                }
            })
        }

        assertEquals("perspective(3cm) translate(10px, 3px) rotateY(3deg)", nextChild().style.transform)
    }
}
