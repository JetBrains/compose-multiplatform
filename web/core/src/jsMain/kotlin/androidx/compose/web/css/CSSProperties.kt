package org.jetbrains.compose.web.css

fun StyleBuilder.opacity(value: Number) {
    property("opacity", value(value))
}

fun StyleBuilder.order(value: Int) {
    property("order", StylePropertyValue(value))
}

fun StyleBuilder.flexGrow(value: Number) {
    property("flex-grow", StylePropertyValue(value))
}

fun StyleBuilder.flexShrink(value: Number) {
    property("flex-shrink", StylePropertyValue(value))
}

fun StyleBuilder.opacity(value: CSSpercentValue) {
    property("opacity", value(value.value as Double / 100))
}

fun StyleBuilder.color(value: String) {
    property("color", value(value))
}

fun StyleBuilder.color(value: Color) {
    // color hasn't Typed OM yet
    property("color", value.styleValue())
}

fun StyleBuilder.backgroundColor(value: CSSVariableValue<Color>) {
    property("background-color", value)
}

fun StyleBuilder.backgroundColor(value: Color) {
    property("background-color", value.styleValue())
}

fun StyleBuilder.backgroundColor(value: String) {
    property("background-color", value(value))
}

enum class LineStyle {
    None,
    Hidden,
    Dotted,
    Dashed,
    Solid,
    Double,
    Groove,
    Ridge,
    Inset,
    Outset
}

enum class DisplayStyle(val value: String) {
    Block("block"),
    Inline("inline"),
    InlineBlock("inline-block"),
    Flex("flex"),
    LegacyInlineFlex("inline-flex"),
    Grid("grid"),
    LegacyInlineGrid("inline-grid"),
    FlowRoot("flow-root"),

    None("none"),
    Contents("contents"),

// TODO(shabunc): This properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far
//    BlockFlow("block flow"),
//    InlineFlow("inline flow"),
//    InlineFlowRoot("inline flow-root"),
//    BlocklFlex("block flex"),
//    InlineFlex("inline flex"),
//    BlockGrid("block grid"),
//    InlineGrid("inline grid"),
//    BlockFlowRoot("block flow-root"),

    Table("table"),
    TableRow("table-row"),
    ListItem("list-item"),

    Inherit("inherit"),
    Initial("initial"),
    Unset("unset")
}

enum class FlexDirection(val value: String) {
    Row("row"),
    RowReverse("row-reverse"),
    Column("column"),
    ColumnReverse("column-reverse")
}

enum class FlexWrap(val value: String) {
    Wrap("wrap"),
    Nowrap("nowrap"),
    WrapReverse("wrap-reverse")
}

enum class JustifyContent(val value: String) {
    Center("center"),
    Start("start"),
    End("end"),
    FlexStart("flex-start"),
    FlexEnd("flex-end"),
    Left("left"),
    Right("right"),
    Normal("normal"),
    SpaceBetween("space-between"),
    SpaceAround("space-around"),
    SpaceEvenly("space-evenly"),
    Stretch("stretch"),
    Inherit("inherit"),
    Initial("initial"),
    Unset("unset"),
    SafeCenter("safe center"),
    UnsafeCenter("unsafe center"),
}

enum class AlignSelf(val value: String) {
    Auto("auto"),
    Normal("normal"),
    Center("center"),
    Start("start"),
    End("end"),
    SelfStart("self-start"),
    SelfEnd("self-end"),
    FlexStart("flex-start"),
    FlexEnd("flex-end"),
    Baseline("baseline"),
//    FirstBaseline("first baseline"),
//    LastBaseline("last baseline"),
    Stretch("stretch"),
    SafeCenter("safe center"),
    UnsafeCenter("unsafe center"),
    Inherit("inherit"),
    Initial("initial"),
    Unset("unset")
}

enum class AlignItems(val value: String) {
    Normal("normal"),
    Stretch("stretch"),
    Center("center"),
    Start("start"),
    End("end"),
    FlexStart("flex-start"),
    FlexEnd("flex-end"),
    Baseline("baseline"),
//    FirstBaseline("first baseline"),
//    LastBaseline("last baseline"),
    SafeCenter("safe center"),
    UnsafeCenter("unsafe center"),

    Inherit("inherit"),
    Initial("initial"),
    Unset("unset")
}

enum class AlignContent(val value: String) {
    Center("center"),
    Start("start"),
    End("end"),
    FlexStart("flex-start"),
    FlexEnd("flex-end"),
    Baseline("baseline"),
//    FirstBaseline("first baseline"),
//    LastBaseline("last baseline"),
    SafeCenter("safe center"),
    UnsafeCenter("unsafe center"),
    SpaceBetween("space-between"),
    SpaceAround("space-around"),
    SpaceEvenly("space-evenly"),
    Stretch("stretch"),

    Inherit("inherit"),
    Initial("initial"),
    Unset("unset")
}

enum class Position(val value: String) {
    Static("static"),
    Relative("relative"),
    Absolute("absolute"),
    Sticky("sticky"),
    Fixed("fixed")
}

class CSSBorder : CustomStyleValue {
    var width: StylePropertyValue? = null
    var style: StylePropertyValue? = null
    var color: StylePropertyValue? = null

    fun width(size: CSSSizeValue) {
        width = StylePropertyValue(size)
    }

    fun style(style: LineStyle) {
        this.style = StylePropertyValue(style.name)
    }

    fun color(color: Color) {
        this.color = color.styleValue()
    }

    fun color(color: CSSVariableValue<Color>) {
        this.color = color
    }

    override fun equals(other: Any?): Boolean {
        return if (other is CSSBorder) {
            styleValue().toString() == other.styleValue().toString()
        } else false
    }

    override fun styleValue(): StylePropertyValue {
        val values = listOfNotNull(width, style, color)
        return StylePropertyValue(values.joinToString(" "))
    }
}

inline fun StyleBuilder.border(crossinline borderBuild: CSSBorder.() -> Unit) {
    val border = CSSBorder().apply(borderBuild)
    property("border", border.styleValue())
}

fun StyleBuilder.border(
    width: CSSSizeValue? = null,
    style: LineStyle? = null,
    color: Color? = null
) {
    border {
        width?.let { width(it) }
        style?.let { style(it) }
        color?.let { color(it) }
    }
}

fun StyleBuilder.display(displayStyle: DisplayStyle) {
    property("display", StylePropertyValue(displayStyle.value))
}

fun StyleBuilder.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", StylePropertyValue(flexDirection.value))
}

fun StyleBuilder.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", StylePropertyValue(flexWrap.value))
}

fun StyleBuilder.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        StylePropertyValue("${flexDirection.value} ${flexWrap.value}")
    )
}

fun StyleBuilder.justifyContent(justifyContent: JustifyContent) {
    property(
        "justify-content",
        StylePropertyValue(justifyContent.value)
    )
}
fun StyleBuilder.alignSelf(alignSelf: AlignSelf) {
    property(
        "align-self",
        StylePropertyValue(alignSelf.value)
    )
}

fun StyleBuilder.alignItems(alignItems: AlignItems) {
    property(
        "align-items",
        StylePropertyValue(alignItems.value)
    )
}

fun StyleBuilder.alignContent(alignContent: AlignContent) {
    property(
        "align-content",
        StylePropertyValue(alignContent.value)
    )
}

fun StyleBuilder.position(position: Position) {
    property(
        "position",
        StylePropertyValue(position.value)
    )
}

fun StyleBuilder.width(value: CSSSizeOrAutoValue) {
    property("width", value)
}

fun StyleBuilder.borderRadius(r: CSSSizeValue) {
    property("border-radius", StylePropertyValue(r.toString()))
}

fun StyleBuilder.borderRadius(topLeft: CSSSizeValue, bottomRight: CSSSizeValue) {
    property("border-radius", StylePropertyValue("$topLeft $bottomRight"))
}

fun StyleBuilder.borderRadius(
    topLeft: CSSSizeValue,
    topRightAndBottomLeft: CSSSizeValue,
    bottomRight: CSSSizeValue
) {
    property("border-radius", StylePropertyValue("$topLeft $topRightAndBottomLeft $bottomRight"))
}

fun StyleBuilder.borderRadius(
    topLeft: CSSSizeValue,
    topRight: CSSSizeValue,
    bottomRight: CSSSizeValue,
    bottomLeft: CSSSizeValue
) {
    property("border-radius", StylePropertyValue("$topLeft $topRight $bottomRight $bottomLeft"))
}

fun StyleBuilder.width(value: CSSSizeValue) {
    width(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.width(value: CSSAutoValue) {
    width(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.height(value: CSSSizeOrAutoValue) {
    property("height", value)
}

fun StyleBuilder.height(value: CSSSizeValue) {
    height(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.height(value: CSSAutoValue) {
    height(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.top(value: CSSSizeOrAutoValue) {
    property("top", value)
}

fun StyleBuilder.top(value: CSSSizeValue) {
    top(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.top(value: CSSAutoValue) {
    top(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.bottom(value: CSSSizeOrAutoValue) {
    property("bottom", value)
}

fun StyleBuilder.bottom(value: CSSSizeValue) {
    bottom(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.bottom(value: CSSAutoValue) {
    bottom(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.left(value: CSSSizeOrAutoValue) {
    property("left", value)
}

fun StyleBuilder.left(value: CSSSizeValue) {
    left(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.left(value: CSSAutoValue) {
    left(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.right(value: CSSSizeOrAutoValue) {
    property("right", value)
}

fun StyleBuilder.right(value: CSSSizeValue) {
    right(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.right(value: CSSAutoValue) {
    right(CSSSizeOrAutoValue(value))
}

fun StyleBuilder.fontSize(value: CSSSizeValue) {
    property("font-size", value(value))
}

fun StyleBuilder.margin(value: CSSSizeValue) {
    // marign hasn't Typed OM yet
    property("margin", value(value.toString()))
}

fun StyleBuilder.marginLeft(value: CSSSizeValue) {
    property("margin-left", value(value.toString()))
}

fun StyleBuilder.marginTop(value: CSSSizeValue) {
    property("margin-top", value(value.toString()))
}

fun StyleBuilder.padding(value: CSSSizeValue) {
    // padding hasn't Typed OM yet
    property("padding", value(value.toString()))
}
