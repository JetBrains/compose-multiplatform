import org.jetbrains.compose.web.css.*

internal object PageStyles : StyleSheet(usePrefix = false) {
    private val ink = Color("#3a281f")
    private val terracotta = Color("#a45f4d")
    private val rule = Color("#e6d8c9")
    private const val serif = "Georgia, 'Iowan Old Style', 'Palatino Linotype', serif"

    val page by style { maxWidth(1300.px); property("margin", "auto"); padding(0.px, 48.px, 55.px) }
    val masthead by style {
        display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.SpaceBetween)
        gap(24.px); fontSize(10.px); fontWeight(700); letterSpacing(0.15.em)
        padding(27.px, 0.px); property("border-bottom", "1px solid #d9c7bb")
    }
    val brand by style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px) }
    val eyebrow by style {
        margin(0.px); color(terracotta); fontSize(11.px)
        fontWeight(700); letterSpacing(0.17.em)
    }
    val hero by style { padding(95.px, 5.percent, 0.px) }
    val lead by style {
        margin(0.px, 0.px, 88.px); color(terracotta); property("font-size", "clamp(18px, 2.1vw, 26px)")
        lineHeight("1.45")
    }
    val heroFoot by style {
        display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(24.px)
        fontSize(10.px); fontWeight(700); letterSpacing(0.15.em)
        padding(22.px, 0.px); property("border-top", "1px solid #d9c7bb")
    }
    val paper by style {
        overflow("hidden"); background("#fffaf1"); border(1.px, LineStyle.Solid, rule)
        borderRadius(36.px); property("box-shadow", "0 36px 100px #805b4730")
    }
    val paperHeader by style {
        display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); gap(20.px)
        padding(28.px, 6.percent); property("border-bottom", "1px solid $rule"); fontSize(10.px)
        fontWeight(700); letterSpacing(0.17.em)
    }
    val islandSection by style { padding(70.px, 6.percent); property("border-bottom", "1px solid $rule") }
    val islandGrid by style {
        display(DisplayStyle.Grid); gridTemplateColumns("repeat(2, minmax(0, 1fr))")
        property("border-bottom", "1px solid $rule")
    }
    val codeSection by style { padding(70.px, 6.percent) }
    val islandHeading by style {
        display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Start)
        gap(24.px); marginBottom(28.px)
    }
    val islandValue by style {
        flex("none"); alignSelf(AlignSelf.End); color(terracotta)
        font("400 32px/1 $serif"); letterSpacing((-0.04).em); whiteSpace("nowrap")
    }
    val islandBody by style {
        padding(30.px, 36.px); background("#f5e9db"); border(1.px, LineStyle.Solid, Color("#e4cdbd"))
        borderRadius(24.px)
    }
    val weightPreview by style {
        minHeight(210.px); font("clamp(120px, 16vw, 190px)/1 system-ui, sans-serif"); letterSpacing((-0.1).em)
    }
    val sizePreview by style {
        minHeight(210.px); overflowX("auto"); whiteSpace("nowrap")
        fontFamily("Georgia", "Iowan Old Style", "Palatino Linotype", "serif"); letterSpacing((-0.075).em)
        property("justify-content", "safe center")
    }
    val controls by style {
        display(DisplayStyle.Grid); gridTemplateColumns("auto minmax(70px, 1fr) auto")
        alignItems(AlignItems.Center); gap(12.px); paddingTop(20.px)
        property("border-top", "1px solid #dfc9b9"); fontSize(10.px); fontWeight(700)
        letterSpacing(0.12.em)
    }
    val textPreview by style {
        display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
        minHeight(210.px); margin(0.px); font("italic clamp(36px, 6vw, 76px)/1.12 $serif")
        letterSpacing((-0.055).em); textAlign("center"); property("overflow-wrap", "anywhere")
    }
    val textControls by style {
        display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
        gap(20.px); paddingTop(20.px); property("border-top", "1px solid #dfc9b9")
        fontSize(10.px); fontWeight(700); letterSpacing(0.12.em)
    }
    val editButton by style {
        padding(8.px, 0.px); border(0.px); background("transparent")
        color(terracotta); font("inherit"); cursor("pointer")
        textDecoration("underline"); property("text-underline-offset", "4px")
    }
    val inputLabel by style {
        display(DisplayStyle.Block); margin(28.px, 0.px, 8.px); fontSize(10.px)
        fontWeight(700); letterSpacing(0.14.em)
    }
    val textInput by style {
        width(100.percent); padding(14.px, 16.px); border(1.px, LineStyle.Solid, Color("#cba891"))
        borderRadius(9.px); background("#fffaf1"); color(ink)
        font("18px $serif")
    }
    val paperFooter by style {
        padding(30.px, 6.percent); property("border-top", "1px solid $rule"); color(Color("#8a685a"))
        fontSize(10.px); fontWeight(700); letterSpacing(0.15.em)
    }

    init {
        "*" style { boxSizing("border-box") }
        "body" style {
            margin(0.px); color(ink); fontFamily("Arial", "Helvetica", "sans-serif")
            background("radial-gradient(circle at 90% 8%, #f7d5bf, transparent 38%), linear-gradient(145deg, #fff7e9, #f2dcc9 65%, #e9c9b6)")
        }
        ".${hero} h1" style {
            margin(32.px, 0.px); font("400 clamp(64px, 8.8vw, 126px)/.98 $serif"); letterSpacing((-0.075).em)
        }
        ".${hero} em" style { fontWeight(400) }
        ".${islandGrid} .${islandSection}" style {
            minWidth(0.px); padding(52.px, 6.percent, 58.px); property("border-bottom", "0")
        }
        ".${islandGrid} .${islandSection} + .${islandSection}" style { property("border-left", "1px solid $rule") }
        ".${islandGrid} .${islandHeading}" style { flexWrap(FlexWrap.Wrap); gap(12.px) }
        ".${islandGrid} .${islandHeading} h2" style { fontSize(48.px) }
        ".${islandGrid} .${islandBody}" style { padding(24.px) }
        ".${islandHeading} h2" style {
            margin(8.px, 0.px, 0.px); font("400 54px/1 $serif"); letterSpacing((-0.05).em)
        }
        ".${weightPreview}, .${sizePreview}" style {
            display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
            color(ink)
        }
        ".${controls} input" style {
            width(100.percent); margin(0.px); property("accent-color", "$terracotta")
            cursor("pointer")
        }
        ".${controls} input:focus-visible" style {
            outline("3px solid $terracotta"); property("outline-offset", "4px")
        }
        ".${editButton}:hover" style { color(Color("#693626")) }
        ".${editButton}:focus-visible, .${textInput}:focus-visible" style {
            outline("3px solid $terracotta"); property("outline-offset", "4px")
        }
        ".${codeSection} h2" style {
            margin(18.px, 0.px, 12.px); font("400 48px/1 $serif"); letterSpacing((-0.05).em)
        }
        ".${codeSection} > p:not(.${eyebrow})" style {
            maxWidth(650.px); color(Color("#795e51")); fontSize(17.px)
            lineHeight("1.55")
        }
        ".${codeSection} pre" style {
            overflowX("auto"); margin(30.px, 0.px, 0.px); padding(28.px)
            background("#f5e9db"); border(1.px, LineStyle.Solid, Color("#e4cdbd")); borderRadius(18.px)
            color(ink); font("14px/1.7 ui-monospace, SFMono-Regular, Consolas, monospace")
        }
        media("(max-width: 1000px)") {
            ".${islandGrid}" style { gridTemplateColumns("1fr") }
            ".${islandGrid} .${islandSection} + .${islandSection}" style {
                property("border-left", "0"); property("border-top", "1px solid $rule")
            }
        }
        media("(max-width: 720px)") {
            ".${page}" style { padding(0.px, 18.px, 55.px) }
            ".${masthead} > span" style { display(DisplayStyle.None) }
            ".${hero}" style { padding(78.px, 3.percent, 0.px) }
            ".${hero} h1" style { property("font-size", "clamp(52px, 11vw, 80px)") }
            ".${lead}" style { marginBottom(72.px) }
            ".${paper}" style { borderRadius(22.px) }
            ".${paperHeader}" style { flexWrap(FlexWrap.Wrap); padding(22.px, 6.percent) }
            ".${islandSection}, .${codeSection}" style { padding(50.px, 6.percent) }
            ".${islandHeading}" style { flexWrap(FlexWrap.Wrap) }
            ".${islandBody}" style { padding(20.px) }
            ".${weightPreview}" style { minHeight(200.px) }
            ".${sizePreview}" style { minHeight(230.px); justifyContent(JustifyContent.Start) }
            ".${controls}" style { gridTemplateColumns("auto 1fr auto"); gap(10.px) }
            ".${codeSection} pre" style { padding(18.px); fontSize(12.px) }
        }
        media("(max-width: 430px)") {
            ".${textControls}" style {
                alignItems(AlignItems.Start); flexDirection(FlexDirection.Column); gap(4.px)
            }
        }
    }
}
