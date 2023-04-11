/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.svg

import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.svg.*
import org.jetbrains.compose.web.testutils.*
import org.w3c.dom.svg.SVGCircleElement
import org.w3c.dom.svg.SVGElement
import org.w3c.dom.svg.SVGTextElement
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalComposeWebSvgApi
class SvgTests {
    @Test
    fun nodeNames() = runTest {
        composition {
            Svg {
                Animate()
                AnimateMotion()
                AnimateTransform()
                Defs()
                Filter()
                G()
                Marker()
                Mpath()
                Switch()
                Tspan()
            }
        }

        assertEquals(
            "<svg><animate></animate><animateMotion></animateMotion><animateTransform></animateTransform><defs></defs><filter></filter><g></g><marker></marker><mpath></mpath><switch></switch><tspan></tspan></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun clipPathTest() = runTest {
        composition {
            Svg {
                ClipPath("myClip") {
                    Circle(40.px, 35.px, 36.px)
                }
            }
        }

        assertEquals(
            "<svg><clipPath id=\"myClip\"><circle cx=\"40px\" cy=\"35px\" r=\"36px\"></circle></clipPath></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun maskTest() = runTest {
        composition {
            Svg {
                Mask("myMask")
            }
        }

        assertEquals("<svg><mask id=\"myMask\"></mask></svg>", nextChild<SVGCircleElement>().outerHTML)
    }


    @Test
    fun svgATest() = runTest {
        composition {
            Svg {
                SvgA("/docs/Web/SVG/Element/circle", {
                    attr("target", "_blank")
                })
            }
        }

        assertEquals(
            "<svg><a href=\"/docs/Web/SVG/Element/circle\" target=\"_blank\"></a></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun descTest() = runTest {
        composition {
            Svg {
                Desc("some description", { attr("id", "myDesc") })
            }
        }

        assertEquals("<svg><desc id=\"myDesc\">some description</desc></svg>", nextChild<SVGCircleElement>().outerHTML)
    }

    @Test
    fun setTest() = runTest {
        composition {
            Svg {
                Rect(0, 0, 10, 10, { attr("id", "rect") }) {
                    Set(attributeName = "class", to = "round", { attr("begin", "me.click"); attr("dur", "2s") })
                }
            }
        }

        assertEquals(
            "<svg><rect x=\"0\" y=\"0\" width=\"10\" height=\"10\" id=\"rect\"><set attributeName=\"class\" to=\"round\" begin=\"me.click\" dur=\"2s\"></set></rect></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun titleTest() = runTest {
        composition {
            Svg {
                Rect(10, 20, 30, 30) {
                    Title("some title")
                }
            }
        }

        assertEquals(
            "<svg><rect x=\"10\" y=\"20\" width=\"30\" height=\"30\"><title>some title</title></rect></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun svgTextTest() = runTest {
        composition {
            Svg {
                SvgText("some text", 20, 30) {
                    classes("small")
                }
            }
        }

        with(nextChild<SVGElement>().firstChild!! as SVGTextElement) {
            assertEquals("text", this.nodeName.lowercase())
            assertEquals(3, this.attributes.length)
            assertEquals("small", this.getAttribute("class"))
            assertEquals("20", this.getAttribute("x"))
            assertEquals("30", this.getAttribute("y"))
            assertEquals("some text", this.innerHTML)
        }
    }

    @Test
    fun textPathTest() = runTest {
        composition {
            Svg {
                TextPath("#someHref", "Some text")
            }
        }

        assertEquals(
            "<svg><textPath href=\"#someHref\">Some text</textPath></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun ellipseTest() = runTest {
        composition {
            Svg {
                Ellipse(50, 60, 70, 20, {
                    attr("color", "yellow")
                })
                Ellipse(50.px, 60.px, 70.percent, 20.px, {
                    attr("color", "red")
                })
            }
        }

        assertEquals(
            "<svg><ellipse cx=\"50\" cy=\"60\" rx=\"70\" ry=\"20\" color=\"yellow\"></ellipse><ellipse cx=\"50px\" cy=\"60px\" rx=\"70%\" ry=\"20px\" color=\"red\"></ellipse></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun circleTest() = runTest {
        composition {
            Svg {
                Circle(50, 60, 70, {
                    attr("color", "red")
                })
                Circle(50.px, 60.px, 70.percent, {
                    attr("color", "red")
                })
            }
        }

        assertEquals(
            "<svg><circle cx=\"50\" cy=\"60\" r=\"70\" color=\"red\"></circle><circle cx=\"50px\" cy=\"60px\" r=\"70%\" color=\"red\"></circle></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun rectTest() = runTest {
        composition {
            Svg {
                Rect(0, 20, 100, 200, {
                    attr("color", "red")
                })
                Rect(0.px, 20.px, 100.px, 200.px, {
                    attr("color", "red")
                })

            }
        }

        assertEquals(
            "<svg><rect x=\"0\" y=\"20\" width=\"100\" height=\"200\" color=\"red\"></rect><rect x=\"0px\" y=\"20px\" width=\"100px\" height=\"200px\" color=\"red\"></rect></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun imageTest() = runTest {
        composition {
            Svg {
                Image("/image.png", {
                    attr("preserveAspectRatio", "xMidYMid meet")
                })
            }
        }

        assertEquals(
            "<svg><image href=\"/image.png\" preserveAspectRatio=\"xMidYMid meet\"></image></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun lineTest() = runTest {
        composition {
            Svg {
                Line(0, 80, 100, 20, {
                    attr("stroke", "red")
                })
                Line(0.px, 80.px, 100.px, 20.px, {
                    attr("stroke", "black")
                })
            }
        }

        assertEquals(
            "<svg><line x1=\"0\" y1=\"80\" x2=\"100\" y2=\"20\" stroke=\"red\"></line><line x1=\"0px\" y1=\"80px\" x2=\"100px\" y2=\"20px\" stroke=\"black\"></line></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun polylineTest() = runTest {
        composition {
            Svg {
                Polyline(0, 100, 50, 25, 50, 75, 100, 0, attrs = {
                    attr("stroke", "red")
                })
            }
        }

        assertEquals(
            "<svg><polyline points=\"0,100 50,25 50,75 100,0\" stroke=\"red\"></polyline></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun polygonTest() = runTest {
        composition {
            Svg {
                Polygon(0, 100, 50, 25, 50, 75, 100, 0, attrs = {
                    attr("stroke", "red")
                })
            }
        }

        assertEquals(
            "<svg><polygon points=\"0,100 50,25 50,75 100,0\" stroke=\"red\"></polygon></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun linearGradientTest() = runTest {
        composition {
            Svg {
                LinearGradient("myGradient") {
                    Stop({
                        attr("offset", 10.percent.toString())
                        attr("stop-color", "gold")
                    })
                    Stop({
                        attr("offset", 95.percent.toString())
                        attr("stop-color", "red")
                    })
                }
            }
        }

        assertEquals(
            "<svg><linearGradient id=\"myGradient\"><stop offset=\"10%\" stop-color=\"gold\"></stop><stop offset=\"95%\" stop-color=\"red\"></stop></linearGradient></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun radialGradientTest() = runTest {
        composition {
            Svg {
                RadialGradient("myGradient") {
                    Stop({
                        attr("offset", 10.percent.toString())
                        attr("stop-color", "gold")
                    })
                    Stop({
                        attr("offset", 95.percent.toString())
                        attr("stop-color", "red")
                    })
                }
            }
        }

        assertEquals(
            "<svg><radialGradient id=\"myGradient\"><stop offset=\"10%\" stop-color=\"gold\"></stop><stop offset=\"95%\" stop-color=\"red\"></stop></radialGradient></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun patternTest() = runTest {
        composition {
            Svg {
                Pattern("something") {
                    Polygon(0, 100, 50, 25, 50, 75, 100, 0, attrs = {
                        attr("stroke", "red")
                    })
                }
            }
        }

        assertEquals(
            "<svg><pattern id=\"something\"><polygon points=\"0,100 50,25 50,75 100,0\" stroke=\"red\"></polygon></pattern></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun viewTest() = runTest {
        composition {
            Svg {
                View("one", "0 0 100 100")
            }
        }

        assertEquals(
            "<svg><view id=\"one\" viewBox=\"0 0 100 100\"></view></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }


    @Test
    fun pathTest() = runTest {
        composition {
            Svg {
                Path(
                    """
           M 10,30
           A 20,20 0,0,1 50,30
           A 20,20 0,0,1 90,30
           Q 90,60 50,90
           Q 10,60 10,30 z
                """.trimIndent()
                )
            }
        }

        assertEquals(
            "<svg><path d=\"M 10,30\n" +
                    "A 20,20 0,0,1 50,30\n" +
                    "A 20,20 0,0,1 90,30\n" +
                    "Q 90,60 50,90\n" +
                    "Q 10,60 10,30 z\"></path></svg>", nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun useTest() = runTest {
        composition {
            Svg {
                Symbol("myDot", {
                    attr("width", "10")
                    attr("height", "10")
                    attr("viewBox", "0 0 2 2")
                }) {
                    Circle(1.px, 1.px, 1.px)
                }

                Use("myDot", {
                    attr("x", "5")
                    attr("y", "5")
                    style {
                        opacity(1)
                    }
                })
            }
        }

        assertEquals(
            "<svg><symbol id=\"myDot\" width=\"10\" height=\"10\" viewBox=\"0 0 2 2\"><circle cx=\"1px\" cy=\"1px\" r=\"1px\"></circle></symbol><use style=\"opacity: 1;\" href=\"myDot\" x=\"5\" y=\"5\"></use></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun svgElementTest() = runTest {
        composition {
            Svg {
                SvgElement<SVGCircleElement>("circle", {
                    attr("cx", 12.px.toString())
                    attr("cy", 22.px.toString())
                    attr("r", 5.percent.toString())
                })
            }
        }

        assertEquals(
            "<svg><circle cx=\"12px\" cy=\"22px\" r=\"5%\"></circle></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

    @Test
    fun svgElementWithViewBoxTest() = runTest {
        composition {
            Svg(viewBox = "0 0 200 200")
        }

        assertEquals(
            "<svg viewBox=\"0 0 200 200\"></svg>",
            nextChild<SVGCircleElement>().outerHTML
        )
    }

}
