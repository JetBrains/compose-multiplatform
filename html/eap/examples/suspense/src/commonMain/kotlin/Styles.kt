/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import org.jetbrains.compose.web.css.*

object PageStyles : StyleSheet() {
    private val ink = Color("#f7f8fc")
    private val muted = Color("#9ba6bd")
    private val lime = Color("#b8ff65")
    private val cyan = Color("#72e6ff")
    private val surface = Color("#111827")
    private val lineColor = Color("#263248")

    val page by style {
        maxWidth(900.px); property("margin", "0 auto"); padding(72.px, 24.px, 40.px)
    }
    val masthead by style {
        color(lime); fontSize(12.px); fontWeight(700); letterSpacing(0.14.em)
    }
    val eyebrow by style {
        margin(0.px); color(lime); fontSize(10.px); fontWeight(700); letterSpacing(0.14.em)
    }
    val accent by style { color(lime) }
    val hero by style {
        self + " h1" style {
            margin(18.px, 0.px, 14.px); property("font-size", "clamp(46px, 8vw, 80px)")
            lineHeight("0.95"); letterSpacing((-0.05).em)
        }
        self + " > p" style {
            maxWidth(650.px); color(Color("#b3bdd1")); fontSize(18.px); lineHeight("1.6")
        }
    }
    val demo by style {
        margin(40.px, 0.px, 18.px); borderRadius(22.px); overflow("hidden")
        border(1.px, LineStyle.Solid, lineColor)
        background("radial-gradient(circle at 90% 0%, #18333d, $surface 45%)")
    }
    val demoHeader by style {
        display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); gap(16.px)
        padding(20.px, 30.px); property("border-bottom", "1px solid $lineColor")
        color(cyan); fontSize(10.px); fontWeight(700); letterSpacing(0.1.em)
    }
    val demoFooter by style {
        display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); gap(16.px)
        flexWrap(FlexWrap.Wrap); padding(20.px, 30.px); property("border-top", "1px solid $lineColor")
        color(muted); fontSize(10.px); letterSpacing(0.06.em)
    }
    val profile by style {
        display(DisplayStyle.Flex); alignItems(AlignItems.Start); gap(26.px); padding(30.px)
        minHeight(275.px)
        self + " h2" style { margin(12.px, 0.px, 8.px); fontSize(36.px); letterSpacing((-0.04).em) }
        self + " p:not(.$eyebrow)" style { color(muted); fontSize(14.px); lineHeight("1.6") }
    }
    val avatar by style {
        display(DisplayStyle.Flex); flex("none"); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
        width(72.px); height(72.px); borderRadius(16.px); background("#18333d")
        border(1.px, LineStyle.Solid, Color("#345660")); color(cyan); fontSize(24.px); fontWeight(700)
    }
    val profileBody by style { flex(1); minWidth(0.px) }
    val role by style { marginTop(0.px) }
    val button by style {
        display(DisplayStyle.InlineBlock); marginTop(8.px); padding(13.px, 18.px)
        color(Color("#071006")); backgroundColor(lime); border(0.px); borderRadius(10.px)
        fontWeight(800); cursor("pointer"); textDecoration("none")
        property("transition", "transform .15s, box-shadow .15s")
    }
    val hint by style { display(DisplayStyle.Block); marginTop(14.px); color(muted); fontSize(11.px); lineHeight("1.5") }
    val shimmer by keyframes {
        from { opacity(0.45) }
        to { opacity(1) }
    }
    val skeleton by style {
        animation(shimmer) { duration(700.ms); iterationCount(null); direction(AnimationDirection.Alternate) }
    }
    val skeletonLine by style {
        height(24.px); maxWidth(320.px); margin(16.px, 0.px); borderRadius(6.px); background("#263248")
        animation(shimmer) { duration(700.ms); iterationCount(null); direction(AnimationDirection.Alternate) }
    }
    val explanation by style { padding(26.px, 0.px, 16.px) }
    val grid by style {
        display(DisplayStyle.Grid); gridTemplateColumns("repeat(auto-fit, minmax(210px, 1fr))")
        gap(14.px); marginTop(24.px)
    }
    val card by style {
        padding(20.px); backgroundColor(surface); border(1.px, LineStyle.Solid, lineColor); borderRadius(16.px)
        self + " h3" style { margin(18.px, 0.px, 8.px); fontSize(20.px) }
        self + " code" style { fontSize(11.px); color(cyan); property("overflow-wrap", "anywhere") }
        self + " > p:last-child" style { color(muted); fontSize(14.px); lineHeight("1.5") }
    }
    val codeSection by style {
        padding(12.px, 0.px)
        self + " pre" style {
            overflowX("auto"); padding(22.px); backgroundColor(Color("#05070d"))
            property("border-left", "4px solid $cyan"); borderRadius(8.px)
            font("12px/1.6 ui-monospace, SFMono-Regular, Consolas, monospace")
        }
    }
    val footer by style {
        display(DisplayStyle.Block); marginTop(18.px); color(Color("#69748c"))
        textAlign("center"); fontSize(12.px); lineHeight("1.6")
    }

    init {
        "*" style { boxSizing("border-box") }
        "body" style {
            margin(0.px); color(ink); backgroundColor(Color("#090d18"))
            fontFamily("Inter", "ui-sans-serif", "system-ui", "sans-serif")
        }
        "a" style { color(cyan); property("text-underline-offset", "3px") }
        ".$button:hover" style {
            property("transform", "translateY(-2px)"); property("box-shadow", "0 10px 30px #b8ff6533")
        }
        ".$button:active" style { property("transform", "translateY(1px) scale(.96)") }
        "a:focus-visible, button:focus-visible" style {
            outline("3px solid $cyan"); property("outline-offset", "4px")
        }
        media("(max-width: 560px)") {
            ".$page" style { padding(48.px, 20.px, 28.px) }
            ".$profile" style { flexDirection(FlexDirection.Column); gap(22.px); padding(24.px) }
            ".$demoHeader, .$demoFooter" style { padding(18.px, 24.px) }
        }
        media("(prefers-reduced-motion: reduce)") {
            ".$skeleton, .$skeletonLine" style { property("animation", "none") }
            ".$button" style { property("transition", "none") }
        }
    }
}
