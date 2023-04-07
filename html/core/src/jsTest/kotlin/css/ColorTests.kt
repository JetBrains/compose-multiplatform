/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals


class ColorTests {
    @Test
    @Suppress("DEPRECATION")
    fun rgbTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.RGB(0, 0, 0)) } })
            Div({ style { color(Color.RGB(200, 10, 20)) } })
        }

        assertEquals("rgb(0, 0, 0)", nextChild().style.color)
        assertEquals("rgb(200, 10, 20)", nextChild().style.color)
    }


    @Test
    fun rgbTest() = runTest {
        composition {
            Div({ style { color(rgb(0, 0, 0)) } })
            Div({ style { color(rgb(200, 10, 20)) } })
        }

        assertEquals("rgb(0, 0, 0)", nextChild().style.color)
        assertEquals("rgb(200, 10, 20)", nextChild().style.color)
    }

    @Test
    @Suppress("DEPRECATION")
    fun rgbaTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.RGBA(0, 220, 0, 0.2)) } })
            Div({ style { color(Color.RGBA(200, 10, 20, 1)) } })
            Div({ style { color(Color.RGBA(200, 10, 20, 0.3)) } })
        }

        assertEquals("rgba(0, 220, 0, 0.2)", nextChild().style.color)
        assertEquals("rgb(200, 10, 20)", nextChild().style.color)
        assertEquals("rgba(200, 10, 20, 0.3)", (root.children[2] as HTMLElement).style.color)
    }


    @Test
    fun rgbaTest() = runTest {
        composition {
            Div({ style { color(rgba(0, 220, 0, 0.2)) } })
            Div({ style { color(rgba(200, 10, 20, 1)) } })
            Div({ style { color(rgba(200, 10, 20, 0.3)) } })
        }

        assertEquals("rgba(0, 220, 0, 0.2)", nextChild().style.color)
        assertEquals("rgb(200, 10, 20)", nextChild().style.color)
        assertEquals("rgba(200, 10, 20, 0.3)", (root.children[2] as HTMLElement).style.color)
    }

    @Test
    @Suppress("DEPRECATION")
    fun hslTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.HSL(100, 120, 50)) } })
            Div({ style { color(Color.HSL(235, 100, 50)) } })
        }

        assertEquals("rgb(85, 255, 0)", nextChild().style.color)
        assertEquals("rgb(0, 21, 255)", nextChild().style.color)
    }

    @Test
    fun hslTest() = runTest {
        composition {
            Div({ style { color(hsl(100, 120, 50)) } })
            Div({ style { color(hsl(235, 100, 50)) } })
        }

        assertEquals("rgb(85, 255, 0)", nextChild().style.color)
        assertEquals("rgb(0, 21, 255)", nextChild().style.color)
    }

    @Test
    @Suppress("DEPRECATION")
    fun hslaTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.HSLA(100, 100, 50, 1)) } })
            Div({ style { color(Color.HSLA(235, 100, 50, .5)) } })
        }

        assertEquals("rgb(85, 255, 0)", nextChild().style.color)
        assertEquals("rgba(0, 21, 255, 0.5)", nextChild().style.color)
    }


    @Test
    fun hslaTest() = runTest {
        composition {
            Div({ style { color(hsla(100, 100, 50, 1)) } })
            Div({ style { color(hsla(235, 100, 50, .5)) } })
        }

        assertEquals("rgb(85, 255, 0)", nextChild().style.color)
        assertEquals("rgba(0, 21, 255, 0.5)", nextChild().style.color)
    }


    @Test
    fun colorConstants() = runTest {
        composition {
            Div({ style { color(Color.aliceblue) } })
            Div({ style { color(Color.antiquewhite) } })
            Div({ style { color(Color.aquamarine) } })
            Div({ style { color(Color.azure) } })
            Div({ style { color(Color.beige) } })
            Div({ style { color(Color.bisque) } })
            Div({ style { color(Color.black) } })
            Div({ style { color(Color.blanchedalmond) } })
            Div({ style { color(Color.blue) } })
            Div({ style { color(Color.blueviolet) } })
            Div({ style { color(Color.brown) } })
            Div({ style { color(Color.burlywood) } })
            Div({ style { color(Color.cadetblue) } })
            Div({ style { color(Color.chartreuse) } })
            Div({ style { color(Color.chocolate) } })
            Div({ style { color(Color.cornflowerblue) } })
            Div({ style { color(Color.cornsilk) } })
            Div({ style { color(Color.crimson) } })
            Div({ style { color(Color.cyan) } })
            Div({ style { color(Color.darkblue) } })
            Div({ style { color(Color.darkcyan) } })
            Div({ style { color(Color.darkgoldenrod) } })
            Div({ style { color(Color.darkgray) } })
            Div({ style { color(Color.darkgreen) } })
            Div({ style { color(Color.darkkhaki) } })
            Div({ style { color(Color.darkmagenta) } })
            Div({ style { color(Color.darkolivegreen) } })
            Div({ style { color(Color.darkorange) } })
            Div({ style { color(Color.darkorchid) } })
            Div({ style { color(Color.darkred) } })
            Div({ style { color(Color.darksalmon) } })
            Div({ style { color(Color.darkslateblue) } })
            Div({ style { color(Color.darkslategray) } })
            Div({ style { color(Color.darkturquoise) } })
            Div({ style { color(Color.darkviolet) } })
            Div({ style { color(Color.deeppink) } })
            Div({ style { color(Color.deepskyblue) } })
            Div({ style { color(Color.dimgray) } })
            Div({ style { color(Color.dodgerblue) } })
            Div({ style { color(Color.firebrick) } })
            Div({ style { color(Color.floralwhite) } })
            Div({ style { color(Color.forestgreen) } })
            Div({ style { color(Color.fuchsia) } })
            Div({ style { color(Color.gainsboro) } })
            Div({ style { color(Color.ghostwhite) } })
            Div({ style { color(Color.goldenrod) } })
            Div({ style { color(Color.gold) } })
            Div({ style { color(Color.gray) } })
            Div({ style { color(Color.green) } })
            Div({ style { color(Color.greenyellow) } })
            Div({ style { color(Color.honeydew) } })
            Div({ style { color(Color.hotpink) } })
            Div({ style { color(Color.indianred) } })
            Div({ style { color(Color.indigo) } })
            Div({ style { color(Color.ivory) } })
            Div({ style { color(Color.khaki) } })
            Div({ style { color(Color.lavenderblush) } })
            Div({ style { color(Color.lavender) } })
            Div({ style { color(Color.lawngreen) } })
            Div({ style { color(Color.lemonchiffon) } })
            Div({ style { color(Color.lightblue) } })
            Div({ style { color(Color.lightcoral) } })
            Div({ style { color(Color.lightcyan) } })
            Div({ style { color(Color.lightgoldenrodyellow) } })
            Div({ style { color(Color.lightgray) } })
            Div({ style { color(Color.lightgreen) } })
            Div({ style { color(Color.lightpink) } })
            Div({ style { color(Color.lightsalmon) } })
            Div({ style { color(Color.lightseagreen) } })
            Div({ style { color(Color.lightskyblue) } })
            Div({ style { color(Color.lightslategray) } })
            Div({ style { color(Color.lightsteelblue) } })
            Div({ style { color(Color.lightyellow) } })
            Div({ style { color(Color.limegreen) } })
            Div({ style { color(Color.lime) } })
            Div({ style { color(Color.linen) } })
            Div({ style { color(Color.magenta) } })
            Div({ style { color(Color.maroon) } })
            Div({ style { color(Color.mediumaquamarine) } })
            Div({ style { color(Color.mediumblue) } })
            Div({ style { color(Color.mediumorchid) } })
            Div({ style { color(Color.mediumpurple) } })
            Div({ style { color(Color.mediumseagreen) } })
            Div({ style { color(Color.mediumslateblue) } })
            Div({ style { color(Color.mediumspringgreen) } })
            Div({ style { color(Color.mediumturquoise) } })
            Div({ style { color(Color.mediumvioletred) } })
            Div({ style { color(Color.midnightblue) } })
            Div({ style { color(Color.mintcream) } })
            Div({ style { color(Color.mistyrose) } })
            Div({ style { color(Color.moccasin) } })
            Div({ style { color(Color.navajowhite) } })
            Div({ style { color(Color.oldlace) } })
            Div({ style { color(Color.olivedrab) } })
            Div({ style { color(Color.olive) } })
            Div({ style { color(Color.orange) } })
            Div({ style { color(Color.orangered) } })
            Div({ style { color(Color.orchid) } })
            Div({ style { color(Color.palegoldenrod) } })
            Div({ style { color(Color.palegreen) } })
            Div({ style { color(Color.paleturquoise) } })
            Div({ style { color(Color.palevioletred) } })
            Div({ style { color(Color.papayawhip) } })
            Div({ style { color(Color.peachpuff) } })
            Div({ style { color(Color.peru) } })
            Div({ style { color(Color.pink) } })
            Div({ style { color(Color.plum) } })
            Div({ style { color(Color.powderblue) } })
            Div({ style { color(Color.purple) } })
            Div({ style { color(Color.rebeccapurple) } })
            Div({ style { color(Color.red) } })
            Div({ style { color(Color.rosybrown) } })
            Div({ style { color(Color.royalblue) } })
            Div({ style { color(Color.saddlebrown) } })
            Div({ style { color(Color.salmon) } })
            Div({ style { color(Color.sandybrown) } })
            Div({ style { color(Color.seagreen) } })
            Div({ style { color(Color.seashell) } })
            Div({ style { color(Color.sienna) } })
            Div({ style { color(Color.silver) } })
            Div({ style { color(Color.skyblue) } })
            Div({ style { color(Color.slateblue) } })
            Div({ style { color(Color.slategray) } })
            Div({ style { color(Color.snow) } })
            Div({ style { color(Color.springgreen) } })
            Div({ style { color(Color.steelblue) } })
            Div({ style { color(Color.teal) } })
            Div({ style { color(Color.thistle) } })
            Div({ style { color(Color.tomato) } })
            Div({ style { color(Color.turquoise) } })
            Div({ style { color(Color.violet) } })
            Div({ style { color(Color.wheat) } })
            Div({ style { color(Color.white) } })
            Div({ style { color(Color.whitesmoke) } })
            Div({ style { color(Color.yellowgreen) } })
            Div({ style { color(Color.yellow) } })

            Div({ style { color(Color.transparent) } })
            Div({ style { color(Color.currentColor) } })
        }

        assertEquals("aliceblue", nextChild().style.color)
        assertEquals("antiquewhite", nextChild().style.color)
        assertEquals("aquamarine", nextChild().style.color)
        assertEquals("azure", nextChild().style.color)
        assertEquals("beige", nextChild().style.color)
        assertEquals("bisque", nextChild().style.color)
        assertEquals("black", nextChild().style.color)
        assertEquals("blanchedalmond", nextChild().style.color)
        assertEquals("blue", nextChild().style.color)
        assertEquals("blueviolet", nextChild().style.color)
        assertEquals("brown", nextChild().style.color)
        assertEquals("burlywood", nextChild().style.color)
        assertEquals("cadetblue", nextChild().style.color)
        assertEquals("chartreuse", nextChild().style.color)
        assertEquals("chocolate", nextChild().style.color)
        assertEquals("cornflowerblue", nextChild().style.color)
        assertEquals("cornsilk", nextChild().style.color)
        assertEquals("crimson", nextChild().style.color)
        assertEquals("cyan", nextChild().style.color)
        assertEquals("darkblue", nextChild().style.color)
        assertEquals("darkcyan", nextChild().style.color)
        assertEquals("darkgoldenrod", nextChild().style.color)
        assertEquals("darkgray", nextChild().style.color)
        assertEquals("darkgreen", nextChild().style.color)
        assertEquals("darkkhaki", nextChild().style.color)
        assertEquals("darkmagenta", nextChild().style.color)
        assertEquals("darkolivegreen", nextChild().style.color)
        assertEquals("darkorange", nextChild().style.color)
        assertEquals("darkorchid", nextChild().style.color)
        assertEquals("darkred", nextChild().style.color)
        assertEquals("darksalmon", nextChild().style.color)
        assertEquals("darkslateblue", nextChild().style.color)
        assertEquals("darkslategray", nextChild().style.color)
        assertEquals("darkturquoise", nextChild().style.color)
        assertEquals("darkviolet", nextChild().style.color)
        assertEquals("deeppink", nextChild().style.color)
        assertEquals("deepskyblue", nextChild().style.color)
        assertEquals("dimgray", nextChild().style.color)
        assertEquals("dodgerblue", nextChild().style.color)
        assertEquals("firebrick", nextChild().style.color)
        assertEquals("floralwhite", nextChild().style.color)
        assertEquals("forestgreen", nextChild().style.color)
        assertEquals("fuchsia", nextChild().style.color)
        assertEquals("gainsboro", nextChild().style.color)
        assertEquals("ghostwhite", nextChild().style.color)
        assertEquals("goldenrod", nextChild().style.color)
        assertEquals("gold", nextChild().style.color)
        assertEquals("gray", nextChild().style.color)
        assertEquals("green", nextChild().style.color)
        assertEquals("greenyellow", nextChild().style.color)
        assertEquals("honeydew", nextChild().style.color)
        assertEquals("hotpink", nextChild().style.color)
        assertEquals("indianred", nextChild().style.color)
        assertEquals("indigo", nextChild().style.color)
        assertEquals("ivory", nextChild().style.color)
        assertEquals("khaki", nextChild().style.color)
        assertEquals("lavenderblush", nextChild().style.color)
        assertEquals("lavender", nextChild().style.color)
        assertEquals("lawngreen", nextChild().style.color)
        assertEquals("lemonchiffon", nextChild().style.color)
        assertEquals("lightblue", nextChild().style.color)
        assertEquals("lightcoral", nextChild().style.color)
        assertEquals("lightcyan", nextChild().style.color)
        assertEquals("lightgoldenrodyellow", nextChild().style.color)
        assertEquals("lightgray", nextChild().style.color)
        assertEquals("lightgreen", nextChild().style.color)
        assertEquals("lightpink", nextChild().style.color)
        assertEquals("lightsalmon", nextChild().style.color)
        assertEquals("lightseagreen", nextChild().style.color)
        assertEquals("lightskyblue", nextChild().style.color)
        assertEquals("lightslategray", nextChild().style.color)
        assertEquals("lightsteelblue", nextChild().style.color)
        assertEquals("lightyellow", nextChild().style.color)
        assertEquals("limegreen", nextChild().style.color)
        assertEquals("lime", nextChild().style.color)
        assertEquals("linen", nextChild().style.color)
        assertEquals("magenta", nextChild().style.color)
        assertEquals("maroon", nextChild().style.color)
        assertEquals("mediumaquamarine", nextChild().style.color)
        assertEquals("mediumblue", nextChild().style.color)
        assertEquals("mediumorchid", nextChild().style.color)
        assertEquals("mediumpurple", nextChild().style.color)
        assertEquals("mediumseagreen", nextChild().style.color)
        assertEquals("mediumslateblue", nextChild().style.color)
        assertEquals("mediumspringgreen", nextChild().style.color)
        assertEquals("mediumturquoise", nextChild().style.color)
        assertEquals("mediumvioletred", nextChild().style.color)
        assertEquals("midnightblue", nextChild().style.color)
        assertEquals("mintcream", nextChild().style.color)
        assertEquals("mistyrose", nextChild().style.color)
        assertEquals("moccasin", nextChild().style.color)
        assertEquals("navajowhite", nextChild().style.color)
        assertEquals("oldlace", nextChild().style.color)
        assertEquals("olivedrab", nextChild().style.color)
        assertEquals("olive", nextChild().style.color)
        assertEquals("orange", nextChild().style.color)
        assertEquals("orangered", nextChild().style.color)
        assertEquals("orchid", nextChild().style.color)
        assertEquals("palegoldenrod", nextChild().style.color)
        assertEquals("palegreen", nextChild().style.color)
        assertEquals("paleturquoise", nextChild().style.color)
        assertEquals("palevioletred", nextChild().style.color)
        assertEquals("papayawhip", nextChild().style.color)
        assertEquals("peachpuff", nextChild().style.color)
        assertEquals("peru", nextChild().style.color)
        assertEquals("pink", nextChild().style.color)
        assertEquals("plum", nextChild().style.color)
        assertEquals("powderblue", nextChild().style.color)
        assertEquals("purple", nextChild().style.color)
        assertEquals("rebeccapurple", nextChild().style.color)
        assertEquals("red", nextChild().style.color)
        assertEquals("rosybrown", nextChild().style.color)
        assertEquals("royalblue", nextChild().style.color)
        assertEquals("saddlebrown", nextChild().style.color)
        assertEquals("salmon", nextChild().style.color)
        assertEquals("sandybrown", nextChild().style.color)
        assertEquals("seagreen", nextChild().style.color)
        assertEquals("seashell", nextChild().style.color)
        assertEquals("sienna", nextChild().style.color)
        assertEquals("silver", nextChild().style.color)
        assertEquals("skyblue", nextChild().style.color)
        assertEquals("slateblue", nextChild().style.color)
        assertEquals("slategray", nextChild().style.color)
        assertEquals("snow", nextChild().style.color)
        assertEquals("springgreen", nextChild().style.color)
        assertEquals("steelblue", nextChild().style.color)
        assertEquals("teal", nextChild().style.color)
        assertEquals("thistle", nextChild().style.color)
        assertEquals("tomato", nextChild().style.color)
        assertEquals("turquoise", nextChild().style.color)
        assertEquals("violet", nextChild().style.color)
        assertEquals("wheat", nextChild().style.color)
        assertEquals("white", nextChild().style.color)
        assertEquals("whitesmoke", nextChild().style.color)
        assertEquals("yellowgreen", nextChild().style.color)
        assertEquals("yellow", nextChild().style.color)
        assertEquals("transparent", nextChild().style.color)
        assertEquals("currentcolor", nextChild().style.color)
    }
}
