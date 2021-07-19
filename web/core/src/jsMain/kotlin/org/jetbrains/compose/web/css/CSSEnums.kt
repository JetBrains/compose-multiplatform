@file:Suppress(
    "Unused",
    "NOTHING_TO_INLINE",
    "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "INLINE_EXTERNAL_DECLARATION",
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "NESTED_EXTERNAL_DECLARATION"
)

package org.jetbrains.compose.web.css

object LineStyle : StylePropertyString {
    val None = LineStyle("none")
    val Hidden = LineStyle("hidden")
    val Dotted = LineStyle("dotted")
    val Dashed = LineStyle("dashed")
    val Solid = LineStyle("solid")
    val Double = LineStyle("double")
    val Groove = LineStyle("groove")
    val Ridge = LineStyle("ridge")
    val Inset = LineStyle("inset")
    val Outset = LineStyle("outset")

    inline operator fun invoke(value: String) = value.unsafeCast<LineStyle>()
}

object DisplayStyle : StylePropertyString {
    val Block = DisplayStyle("block")
    val Inline = DisplayStyle("inline")
    val InlineBlock = DisplayStyle("inline-block")
    val Flex = DisplayStyle("flex")
    val LegacyInlineFlex = DisplayStyle("inline-flex")
    val Grid = DisplayStyle("grid")
    val LegacyInlineGrid = DisplayStyle("inline-grid")
    val FlowRoot = DisplayStyle("flow-root")

    val None = DisplayStyle("none")
    val Contents = DisplayStyle("contents")

// TODO(shabunc): Following properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far:
// "block flow", "inline flow", "inline flow-root",  "block flex",  "inline flex",  "block grid",  "inline grid", "block flow-root",

    val Table = DisplayStyle("table")
    val TableRow = DisplayStyle("table-row")
    val ListItem = DisplayStyle("list-item")

    val Inherit = DisplayStyle("inherit")
    val Initial = DisplayStyle("initial")
    val Unset = DisplayStyle("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<DisplayStyle>()
}

object FlexDirection : StylePropertyString {
    val Row = FlexDirection("row")
    val RowReverse = FlexDirection("row-reverse")
    val Column = FlexDirection("column")
    val ColumnReverse = FlexDirection("column-reverse")

    inline operator fun invoke(value: String) = value.unsafeCast<FlexDirection>()
}

object FlexWrap : StylePropertyString {
    val Wrap = FlexWrap("wrap")
    val Nowrap = FlexWrap("nowrap")
    val WrapReverse = FlexWrap("wrap-reverse")

    inline operator fun invoke(value: String) = value.unsafeCast<FlexWrap>()
}

object JustifyContent : StylePropertyString {
    val Center = JustifyContent("center")
    val Start = JustifyContent("start")
    val End = JustifyContent("end")
    val FlexStart = JustifyContent("flex-start")
    val FlexEnd = JustifyContent("flex-end")
    val Left = JustifyContent("left")
    val Right = JustifyContent("right")
    val Normal = JustifyContent("normal")
    val SpaceBetween = JustifyContent("space-between")
    val SpaceAround = JustifyContent("space-around")
    val SpaceEvenly = JustifyContent("space-evenly")
    val Stretch = JustifyContent("stretch")
    val Inherit = JustifyContent("inherit")
    val Initial = JustifyContent("initial")
    val Unset = JustifyContent("unset")
    val SafeCenter = JustifyContent("safe center")
    val UnsafeCenter = JustifyContent("unsafe center")

    inline operator fun invoke(value: String) = value.unsafeCast<JustifyContent>()
}

object AlignSelf : StylePropertyString {
    val Auto = AlignSelf("auto")
    val Normal = AlignSelf("normal")
    val Center = AlignSelf("center")
    val Start = AlignSelf("start")
    val End = AlignSelf("end")
    val SelfStart = AlignSelf("self-start")
    val SelfEnd = AlignSelf("self-end")
    val FlexStart = AlignSelf("flex-start")
    val FlexEnd = AlignSelf("flex-end")
    val Baseline = AlignSelf("baseline")

    //    FirstBaseline("first baseline")
//    LastBaseline("last baseline")
    val Stretch = AlignSelf("stretch")
    val SafeCenter = AlignSelf("safe center")
    val UnsafeCenter = AlignSelf("unsafe center")
    val Inherit = AlignSelf("inherit")
    val Initial = AlignSelf("initial")
    val Unset = AlignSelf("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<AlignSelf>()
}

object AlignItems : StylePropertyString {
    val Normal = AlignItems("normal")
    val Stretch = AlignItems("stretch")
    val Center = AlignItems("center")
    val Start = AlignItems("start")
    val End = AlignItems("end")
    val FlexStart = AlignItems("flex-start")
    val FlexEnd = AlignItems("flex-end")
    val Baseline = AlignItems("baseline")

    //    FirstBaseline("first baseline")
//    LastBaseline("last baseline")
    val SafeCenter = AlignItems("safe center")
    val UnsafeCenter = AlignItems("unsafe center")

    val Inherit = AlignItems("inherit")
    val Initial = AlignItems("initial")
    val Unset = AlignItems("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<AlignItems>()
}

object AlignContent : StylePropertyString {
    val Center = AlignContent("center")
    val Start = AlignContent("start")
    val End = AlignContent("end")
    val FlexStart = AlignContent("flex-start")
    val FlexEnd = AlignContent("flex-end")
    val Baseline = AlignContent("baseline")

    //    FirstBaseline("first baseline")
//    LastBaseline("last baseline")
    val SafeCenter = AlignContent("safe center")
    val UnsafeCenter = AlignContent("unsafe center")
    val SpaceBetween = AlignContent("space-between")
    val SpaceAround = AlignContent("space-around")
    val SpaceEvenly = AlignContent("space-evenly")
    val Stretch = AlignContent("stretch")

    val Inherit = AlignContent("inherit")
    val Initial = AlignContent("initial")
    val Unset = AlignContent("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<AlignContent>()
}

object Position : StylePropertyString {
    val Static = Position("static")
    val Relative = Position("relative")
    val Absolute = Position("absolute")
    val Sticky = Position("sticky")
    val Fixed = Position("fixed")

    inline operator fun invoke(value: String) = value.unsafeCast<Position>()
}

typealias LanguageCode = String

object StepPosition : StylePropertyString {
    val JumpStart = StepPosition("jump-start")
    val JumpEnd = StepPosition("jump-end")
    val JumpNone = StepPosition("jump-none")
    val JumpBoth = StepPosition("jump-both")
    val Start = StepPosition("start")
    val End = StepPosition("end")

    inline operator fun invoke(value: String) = value.unsafeCast<StepPosition>()
}

object AnimationTimingFunction : StylePropertyString {
    val Ease = AnimationTimingFunction("ease")
    val EaseIn = AnimationTimingFunction("ease-in")
    val EaseOut = AnimationTimingFunction("ease-out")
    val EaseInOut = AnimationTimingFunction("ease-in-out")
    val Linear = AnimationTimingFunction("linear")
    val StepStart = AnimationTimingFunction("step-start")
    val StepEnd = AnimationTimingFunction("step-end")

    inline fun cubicBezier(x1: Double, y1: Double, x2: Double, y2: Double) =
        AnimationTimingFunction("cubic-bezier($x1, $y1, $x2, $y2)")

    inline fun steps(count: Int, stepPosition: StepPosition) = AnimationTimingFunction("steps($count, $stepPosition)")
    inline fun steps(count: Int) = AnimationTimingFunction("steps($count)")

    val Inherit = AnimationTimingFunction("inherit")
    val Initial = AnimationTimingFunction("initial")
    val Unset = AnimationTimingFunction("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<AnimationTimingFunction>()
}

object AnimationDirection : StylePropertyString {
    val Normal = AnimationDirection("normal")
    val Reverse = AnimationDirection("reverse")
    val Alternate = AnimationDirection("alternate")
    val AlternateReverse = AnimationDirection("alternate-reverse")

    val Inherit = AnimationDirection("inherit")
    val Initial = AnimationDirection("initial")
    val Unset = AnimationDirection("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<AnimationDirection>()
}

object AnimationFillMode : StylePropertyString {
    val None = AnimationFillMode("none")
    val Forwards = AnimationFillMode("forwards")
    val Backwards = AnimationFillMode("backwards")
    val Both = AnimationFillMode("both")

    inline operator fun invoke(value: String) = value.unsafeCast<AnimationFillMode>()
}

object AnimationPlayState : StylePropertyString {
    val Running = AnimationPlayState("running")
    val Paused = AnimationPlayState("Paused")
    val Backwards = AnimationPlayState("backwards")
    val Both = AnimationPlayState("both")

    val Inherit = AnimationPlayState("inherit")
    val Initial = AnimationPlayState("initial")
    val Unset = AnimationPlayState("unset")

    inline operator fun invoke(value: String) = value.unsafeCast<AnimationPlayState>()
}