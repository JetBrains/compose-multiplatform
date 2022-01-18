@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.compose.web.css

external interface CSSColorValue : StylePropertyValue, CSSVariableValueAs<CSSColorValue>

object Color {

    @Deprecated("use org.jetbrains.compose.web.css.rgb", ReplaceWith("rgb(r, g, b)"))
    data class RGB(val r: Number, val g: Number, val b: Number) : CSSColorValue {
        override fun toString(): String = "rgb($r, $g, $b)"
    }

    @Deprecated("use org.jetbrains.compose.web.css.rgba", ReplaceWith("rgba(r, g, b, a)"))
    data class RGBA(val r: Number, val g: Number, val b: Number, val a: Number) : CSSColorValue {
        override fun toString(): String = "rgba($r, $g, $b, $a)"
    }

    @Deprecated("use org.jetbrains.compose.web.css.hsl", ReplaceWith("hsl(h, s, l)"))
    data class HSL(val h: CSSAngleValue, val s: Number, val l: Number) : CSSColorValue {
        constructor(h: Number, s: Number, l: Number) : this(h.deg, s, l)

        override fun toString(): String = "hsl($h, $s%, $l%)"
    }

    @Deprecated("use org.jetbrains.compose.web.css.hsla", ReplaceWith("hsla(h, s, l, a)"))
    data class HSLA(val h: CSSAngleValue, val s: Number, val l: Number, val a: Number) : CSSColorValue {
        constructor(h: Number, s: Number, l: Number, a: Number) : this(h.deg, s, l, a)

        override fun toString(): String = "hsla($h, $s%, $l%, $a)"
    }

    inline val aliceblue get() = Color("aliceblue")
    inline val antiquewhite get() = Color("antiquewhite")
    inline val aquamarine get() = Color("aquamarine")
    inline val azure get() = Color("azure")
    inline val beige get() = Color("beige")
    inline val bisque get() = Color("bisque")
    inline val black get() = Color("black")
    inline val blanchedalmond get() = Color("blanchedalmond")
    inline val blue get() = Color("blue")
    inline val blueviolet get() = Color("blueviolet")
    inline val brown get() = Color("brown")
    inline val burlywood get() = Color("burlywood")
    inline val cadetblue get() = Color("cadetblue")
    inline val chartreuse get() = Color("chartreuse")
    inline val chocolate get() = Color("chocolate")
    inline val cornflowerblue get() = Color("cornflowerblue")
    inline val cornsilk get() = Color("cornsilk")
    inline val crimson get() = Color("crimson")
    inline val cyan get() = Color("cyan")
    inline val darkblue get() = Color("darkblue")
    inline val darkcyan get() = Color("darkcyan")
    inline val darkgoldenrod get() = Color("darkgoldenrod")
    inline val darkgray get() = Color("darkgray")
    inline val darkgreen get() = Color("darkgreen")
    inline val darkkhaki get() = Color("darkkhaki")
    inline val darkmagenta get() = Color("darkmagenta")
    inline val darkolivegreen get() = Color("darkolivegreen")
    inline val darkorange get() = Color("darkorange")
    inline val darkorchid get() = Color("darkorchid")
    inline val darkred get() = Color("darkred")
    inline val darksalmon get() = Color("darksalmon")
    inline val darkslateblue get() = Color("darkslateblue")
    inline val darkslategray get() = Color("darkslategray")
    inline val darkturquoise get() = Color("darkturquoise")
    inline val darkviolet get() = Color("darkviolet")
    inline val deeppink get() = Color("deeppink")
    inline val deepskyblue get() = Color("deepskyblue")
    inline val dimgray get() = Color("dimgray")
    inline val dodgerblue get() = Color("dodgerblue")
    inline val firebrick get() = Color("firebrick")
    inline val floralwhite get() = Color("floralwhite")
    inline val forestgreen get() = Color("forestgreen")
    inline val fuchsia get() = Color("fuchsia")
    inline val gainsboro get() = Color("gainsboro")
    inline val ghostwhite get() = Color("ghostwhite")
    inline val goldenrod get() = Color("goldenrod")
    inline val gold get() = Color("gold")
    inline val gray get() = Color("gray")
    inline val green get() = Color("green")
    inline val greenyellow get() = Color("greenyellow")
    inline val honeydew get() = Color("honeydew")
    inline val hotpink get() = Color("hotpink")
    inline val indianred get() = Color("indianred")
    inline val indigo get() = Color("indigo")
    inline val ivory get() = Color("ivory")
    inline val khaki get() = Color("khaki")
    inline val lavenderblush get() = Color("lavenderblush")
    inline val lavender get() = Color("lavender")
    inline val lawngreen get() = Color("lawngreen")
    inline val lemonchiffon get() = Color("lemonchiffon")
    inline val lightblue get() = Color("lightblue")
    inline val lightcoral get() = Color("lightcoral")
    inline val lightcyan get() = Color("lightcyan")
    inline val lightgoldenrodyellow get() = Color("lightgoldenrodyellow")
    inline val lightgray get() = Color("lightgray")
    inline val lightgreen get() = Color("lightgreen")
    inline val lightpink get() = Color("lightpink")
    inline val lightsalmon get() = Color("lightsalmon")
    inline val lightseagreen get() = Color("lightseagreen")
    inline val lightskyblue get() = Color("lightskyblue")
    inline val lightslategray get() = Color("lightslategray")
    inline val lightsteelblue get() = Color("lightsteelblue")
    inline val lightyellow get() = Color("lightyellow")
    inline val limegreen get() = Color("limegreen")
    inline val lime get() = Color("lime")
    inline val linen get() = Color("linen")
    inline val magenta get() = Color("magenta")
    inline val maroon get() = Color("maroon")
    inline val mediumaquamarine get() = Color("mediumaquamarine")
    inline val mediumblue get() = Color("mediumblue")
    inline val mediumorchid get() = Color("mediumorchid")
    inline val mediumpurple get() = Color("mediumpurple")
    inline val mediumseagreen get() = Color("mediumseagreen")
    inline val mediumslateblue get() = Color("mediumslateblue")
    inline val mediumspringgreen get() = Color("mediumspringgreen")
    inline val mediumturquoise get() = Color("mediumturquoise")
    inline val mediumvioletred get() = Color("mediumvioletred")
    inline val midnightblue get() = Color("midnightblue")
    inline val mintcream get() = Color("mintcream")
    inline val mistyrose get() = Color("mistyrose")
    inline val moccasin get() = Color("moccasin")
    inline val navajowhite get() = Color("navajowhite")
    inline val navi get() = Color("navi")
    inline val oldlace get() = Color("oldlace")
    inline val olivedrab get() = Color("olivedrab")
    inline val olive get() = Color("olive")
    inline val orange get() = Color("orange")
    inline val orangered get() = Color("orangered")
    inline val orchid get() = Color("orchid")
    inline val palegoldenrod get() = Color("palegoldenrod")
    inline val palegreen get() = Color("palegreen")
    inline val paleturquoise get() = Color("paleturquoise")
    inline val palevioletred get() = Color("palevioletred")
    inline val papayawhip get() = Color("papayawhip")
    inline val peachpuff get() = Color("peachpuff")
    inline val peru get() = Color("peru")
    inline val pink get() = Color("pink")
    inline val plum get() = Color("plum")
    inline val powderblue get() = Color("powderblue")
    inline val purple get() = Color("purple")
    inline val rebeccapurple get() = Color("rebeccapurple")
    inline val red get() = Color("red")
    inline val rosybrown get() = Color("rosybrown")
    inline val royalblue get() = Color("royalblue")
    inline val saddlebrown get() = Color("saddlebrown")
    inline val salmon get() = Color("salmon")
    inline val sandybrown get() = Color("sandybrown")
    inline val seagreen get() = Color("seagreen")
    inline val seashell get() = Color("seashell")
    inline val sienna get() = Color("sienna")
    inline val silver get() = Color("silver")
    inline val skyblue get() = Color("skyblue")
    inline val slateblue get() = Color("slateblue")
    inline val slategray get() = Color("slategray")
    inline val snow get() = Color("snow")
    inline val springgreen get() = Color("springgreen")
    inline val steelblue get() = Color("steelblue")
    inline val teal get() = Color("teal")
    inline val thistle get() = Color("thistle")
    inline val tomato get() = Color("tomato")
    inline val turquoise get() = Color("turquoise")
    inline val violet get() = Color("violet")
    inline val wheat get() = Color("wheat")
    inline val white get() = Color("white")
    inline val whitesmoke get() = Color("whitesmoke")
    inline val yellowgreen get() = Color("yellowgreen")
    inline val yellow get() = Color("yellow")

    inline val transparent get() = Color("transparent")
    inline val currentColor get() = Color("currentColor")
}

fun Color(name: String): CSSColorValue = name.unsafeCast<CSSColorValue>()

private class RGB(val r: Number, val g: Number, val b: Number): CSSColorValue {
    override fun toString(): String = "rgb($r, $g, $b)"
}

private class RGBA(val r: Number, val g: Number, val b: Number, val a: Number) : CSSColorValue {
    override fun toString(): String = "rgba($r, $g, $b, $a)"
}

private class HSL(val h: CSSAngleValue, val s: Number, val l: Number) : CSSColorValue {
    override fun toString(): String = "hsl($h, $s%, $l%)"
}

private class HSLA(val h: CSSAngleValue, val s: Number, val l: Number, val a: Number) : CSSColorValue {
    override fun toString(): String = "hsla($h, $s%, $l%, $a)"
}

fun rgb(r: Number, g: Number, b: Number): CSSColorValue = RGB(r, g, b)
fun rgba(r: Number, g: Number, b: Number, a: Number): CSSColorValue = RGBA(r, g, b, a)
fun hsl(h: CSSAngleValue, s: Number, l: Number): CSSColorValue = HSL(h, s, l)
fun hsl(h: Number, s: Number, l: Number): CSSColorValue = HSL(h.deg, s, l)
fun hsla(h: CSSAngleValue, s: Number, l: Number, a: Number): CSSColorValue = HSLA(h, s, l, a)
fun hsla(h: Number, s: Number, l: Number, a: Number): CSSColorValue = HSLA(h.deg, s, l, a)
