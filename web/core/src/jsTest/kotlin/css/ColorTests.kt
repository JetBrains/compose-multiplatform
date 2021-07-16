/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
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

        assertEquals("rgb(0, 0, 0)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgb(200, 10, 20)", (root.children[1] as HTMLElement).style.color)
    }


    @Test
    fun rgbTest() = runTest {
        composition {
            Div({ style { color(rgb(0, 0, 0)) } })
            Div({ style { color(rgb(200, 10, 20)) } })
        }

        assertEquals("rgb(0, 0, 0)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgb(200, 10, 20)", (root.children[1] as HTMLElement).style.color)
    }

    @Test
    @Suppress("DEPRECATION")
    fun rgbaTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.RGBA(0, 220, 0, 0.2)) } })
            Div({ style { color(Color.RGBA(200, 10, 20, 1)) } })
            Div({ style { color(Color.RGBA(200, 10, 20, 0.3)) } })
        }

        assertEquals("rgba(0, 220, 0, 0.2)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgb(200, 10, 20)", (root.children[1] as HTMLElement).style.color)
        assertEquals("rgba(200, 10, 20, 0.3)", (root.children[2] as HTMLElement).style.color)
    }


    @Test
    fun rgbaTest() = runTest {
        composition {
            Div({ style { color(rgba(0, 220, 0, 0.2)) } })
            Div({ style { color(rgba(200, 10, 20, 1)) } })
            Div({ style { color(rgba(200, 10, 20, 0.3)) } })
        }

        assertEquals("rgba(0, 220, 0, 0.2)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgb(200, 10, 20)", (root.children[1] as HTMLElement).style.color)
        assertEquals("rgba(200, 10, 20, 0.3)", (root.children[2] as HTMLElement).style.color)
    }

    @Test
    @Suppress("DEPRECATION")
    fun hslTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.HSL(100, 120, 50)) } })
            Div({ style { color(Color.HSL(235, 100, 50)) } })
        }

        assertEquals("rgb(85, 255, 0)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgb(0, 21, 255)", (root.children[1] as HTMLElement).style.color)
    }

    @Test
    fun hslTest() = runTest {
        composition {
            Div({ style { color(hsl(100, 120, 50)) } })
            Div({ style { color(hsl(235, 100, 50)) } })
        }

        assertEquals("rgb(85, 255, 0)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgb(0, 21, 255)", (root.children[1] as HTMLElement).style.color)
    }

    @Test
    @Suppress("DEPRECATION")
    fun hslaTestDeprecated() = runTest {
        composition {
            Div({ style { color(Color.HSLA(100, 100, 50, 1)) } })
            Div({ style { color(Color.HSLA(235, 100, 50, .5)) } })
        }

        assertEquals("rgb(85, 255, 0)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgba(0, 21, 255, 0.5)", (root.children[1] as HTMLElement).style.color)
    }


    @Test
    fun hslaTest() = runTest {
        composition {
            Div({ style { color(hsla(100, 100, 50, 1)) } })
            Div({ style { color(hsla(235, 100, 50, .5)) } })
        }

        assertEquals("rgb(85, 255, 0)", (root.children[0] as HTMLElement).style.color)
        assertEquals("rgba(0, 21, 255, 0.5)", (root.children[1] as HTMLElement).style.color)
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

        var counter = 0
        assertEquals("aliceblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("antiquewhite", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("aquamarine", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("azure", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("beige", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("bisque", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("black", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("blanchedalmond", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("blue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("blueviolet", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("brown", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("burlywood", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("cadetblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("chartreuse", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("chocolate", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("cornflowerblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("cornsilk", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("crimson", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("cyan", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkcyan", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkgoldenrod", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkgray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkgreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkkhaki", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkmagenta", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkolivegreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkorange", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkorchid", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkred", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darksalmon", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkslateblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkslategray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkturquoise", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("darkviolet", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("deeppink", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("deepskyblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("dimgray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("dodgerblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("firebrick", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("floralwhite", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("forestgreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("fuchsia", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("gainsboro", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("ghostwhite", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("goldenrod", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("gold", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("gray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("green", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("greenyellow", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("honeydew", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("hotpink", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("indianred", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("indigo", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("ivory", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("khaki", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lavenderblush", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lavender", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lawngreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lemonchiffon", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightcoral", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightcyan", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightgoldenrodyellow", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightgray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightgreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightpink", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightsalmon", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightseagreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightskyblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightslategray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightsteelblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lightyellow", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("limegreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("lime", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("linen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("magenta", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("maroon", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumaquamarine", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumorchid", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumpurple", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumseagreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumslateblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumspringgreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumturquoise", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mediumvioletred", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("midnightblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mintcream", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("mistyrose", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("moccasin", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("navajowhite", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("oldlace", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("olivedrab", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("olive", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("orange", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("orangered", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("orchid", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("palegoldenrod", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("palegreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("paleturquoise", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("palevioletred", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("papayawhip", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("peachpuff", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("peru", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("pink", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("plum", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("powderblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("purple", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("rebeccapurple", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("red", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("rosybrown", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("royalblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("saddlebrown", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("salmon", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("sandybrown", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("seagreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("seashell", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("sienna", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("silver", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("skyblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("slateblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("slategray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("snow", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("springgreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("steelblue", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("teal", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("thistle", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("tomato", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("turquoise", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("violet", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("wheat", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("white", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("whitesmoke", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("yellowgreen", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("yellow", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("transparent", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("currentcolor", (root.children[counter] as HTMLElement).style.color)
    }
}
