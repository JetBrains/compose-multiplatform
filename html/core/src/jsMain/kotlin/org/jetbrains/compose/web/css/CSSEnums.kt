@file:Suppress("Unused", "NOTHING_TO_INLINE", "NESTED_CLASS_IN_EXTERNAL_INTERFACE", "INLINE_EXTERNAL_DECLARATION", "WRONG_BODY_OF_EXTERNAL_DECLARATION", "NESTED_EXTERNAL_DECLARATION")

package org.jetbrains.compose.web.css

interface StylePropertyEnum: StylePropertyString
inline val StylePropertyEnum.name get() = this.unsafeCast<String>()
inline val StylePropertyEnum.value get() = this.unsafeCast<String>()

interface LineStyle: StylePropertyEnum {
    companion object {
        inline val None get() = LineStyle("none")
        inline val Hidden get() = LineStyle("hidden")
        inline val Dotted get() = LineStyle("dotted")
        inline val Dashed get() = LineStyle("dashed")
        inline val Solid get() = LineStyle("solid")
        inline val Double get() = LineStyle("double")
        inline val Groove get() = LineStyle("groove")
        inline val Ridge get() = LineStyle("ridge")
        inline val Inset get() = LineStyle("inset")
        inline val Outset get() = LineStyle("outset")
    }
}
inline fun LineStyle(value: String) = value.unsafeCast<LineStyle>()

interface DisplayStyle: StylePropertyEnum {
    companion object {
        inline val Block get() = DisplayStyle("block")
        inline val Inline get() = DisplayStyle("inline")
        inline val InlineBlock get() = DisplayStyle("inline-block")
        inline val Flex get() = DisplayStyle("flex")
        inline val LegacyInlineFlex get() = DisplayStyle("inline-flex")
        inline val Grid get() = DisplayStyle("grid")
        inline val LegacyInlineGrid get() = DisplayStyle("inline-grid")
        inline val FlowRoot get() = DisplayStyle("flow-root")

        inline val None get() = DisplayStyle("none")
        inline val Contents get() = DisplayStyle("contents")

// TODO(shabunc): This properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far
//    BlockFlow("block flow")
//    InlineFlow("inline flow")
//    InlineFlowRoot("inline flow-root")
//    BlocklFlex("block flex")
//    InlineFlex("inline flex")
//    BlockGrid("block grid")
//    InlineGrid("inline grid")
//    BlockFlowRoot("block flow-root")

        inline val Table get() = DisplayStyle("table")
        inline val TableRow get() = DisplayStyle("table-row")
        inline val ListItem get() = DisplayStyle("list-item")

        inline val Inherit get() = DisplayStyle("inherit")
        inline val Initial get() = DisplayStyle("initial")
        inline val Unset get() = DisplayStyle("unset")
    }
}
inline fun DisplayStyle(value: String) = value.unsafeCast<DisplayStyle>()

interface FlexDirection: StylePropertyEnum {
    companion object {
        inline val Row get() = FlexDirection("row")
        inline val RowReverse get() = FlexDirection("row-reverse")
        inline val Column get() = FlexDirection("column")
        inline val ColumnReverse get() = FlexDirection("column-reverse")
    }
}
inline fun FlexDirection(value: String) = value.unsafeCast<FlexDirection>()

interface FlexWrap: StylePropertyEnum {
    companion object {
        inline val Wrap get() = FlexWrap("wrap")
        inline val Nowrap get() = FlexWrap("nowrap")
        inline val WrapReverse get() = FlexWrap("wrap-reverse")
    }
}
inline fun FlexWrap(value: String) = value.unsafeCast<FlexWrap>()

interface JustifyContent: StylePropertyEnum {
    companion object {
        inline val Center get() = JustifyContent("center")
        inline val Start get() = JustifyContent("start")
        inline val End get() = JustifyContent("end")
        inline val FlexStart get() = JustifyContent("flex-start")
        inline val FlexEnd get() = JustifyContent("flex-end")
        inline val Left get() = JustifyContent("left")
        inline val Right get() = JustifyContent("right")
        inline val Normal get() = JustifyContent("normal")
        inline val SpaceBetween get() = JustifyContent("space-between")
        inline val SpaceAround get() = JustifyContent("space-around")
        inline val SpaceEvenly get() = JustifyContent("space-evenly")
        inline val Stretch get() = JustifyContent("stretch")
        inline val Inherit get() = JustifyContent("inherit")
        inline val Initial get() = JustifyContent("initial")
        inline val Unset get() = JustifyContent("unset")
        inline val SafeCenter get() = JustifyContent("safe center")
        inline val UnsafeCenter get() = JustifyContent("unsafe center")
    }
}
inline fun JustifyContent(value: String) = value.unsafeCast<JustifyContent>()

interface AlignSelf: StylePropertyEnum {
    companion object {
        inline val Auto get() = AlignSelf("auto")
        inline val Normal get() = AlignSelf("normal")
        inline val Center get() = AlignSelf("center")
        inline val Start get() = AlignSelf("start")
        inline val End get() = AlignSelf("end")
        inline val SelfStart get() = AlignSelf("self-start")
        inline val SelfEnd get() = AlignSelf("self-end")
        inline val FlexStart get() = AlignSelf("flex-start")
        inline val FlexEnd get() = AlignSelf("flex-end")
        inline val Baseline get() = AlignSelf("baseline")
//    FirstBaseline("first baseline")
//    LastBaseline("last baseline")
        inline val Stretch get() = AlignSelf("stretch")
        inline val SafeCenter get() = AlignSelf("safe center")
        inline val UnsafeCenter get() = AlignSelf("unsafe center")
        inline val Inherit get() = AlignSelf("inherit")
        inline val Initial get() = AlignSelf("initial")
        inline val Unset get() = AlignSelf("unset")
    }
}
inline fun AlignSelf(value: String) = value.unsafeCast<AlignSelf>()

interface AlignItems: StylePropertyEnum {
    companion object {
        inline val Normal get() = AlignItems("normal")
        inline val Stretch get() = AlignItems("stretch")
        inline val Center get() = AlignItems("center")
        inline val Start get() = AlignItems("start")
        inline val End get() = AlignItems("end")
        inline val FlexStart get() = AlignItems("flex-start")
        inline val FlexEnd get() = AlignItems("flex-end")
        inline val Baseline get() = AlignItems("baseline")
//    FirstBaseline("first baseline")
//    LastBaseline("last baseline")
        inline val SafeCenter get() = AlignItems("safe center")
        inline val UnsafeCenter get() = AlignItems("unsafe center")

        inline val Inherit get() = AlignItems("inherit")
        inline val Initial get() = AlignItems("initial")
        inline val Unset get() = AlignItems("unset")
    }
}
inline fun AlignItems(value: String) = value.unsafeCast<AlignItems>()

interface AlignContent: StylePropertyEnum {
    companion object {
        inline val Center get() = AlignContent("center")
        inline val Start get() = AlignContent("start")
        inline val End get() = AlignContent("end")
        inline val FlexStart get() = AlignContent("flex-start")
        inline val FlexEnd get() = AlignContent("flex-end")
        inline val Baseline get() = AlignContent("baseline")
//    FirstBaseline("first baseline")
//    LastBaseline("last baseline")
        inline val SafeCenter get() = AlignContent("safe center")
        inline val UnsafeCenter get() = AlignContent("unsafe center")
        inline val SpaceBetween get() = AlignContent("space-between")
        inline val SpaceAround get() = AlignContent("space-around")
        inline val SpaceEvenly get() = AlignContent("space-evenly")
        inline val Stretch get() = AlignContent("stretch")

        inline val Inherit get() = AlignContent("inherit")
        inline val Initial get() = AlignContent("initial")
        inline val Unset get() = AlignContent("unset")
    }
}
inline fun AlignContent(value: String) = value.unsafeCast<AlignContent>()

interface Position: StylePropertyEnum {
    companion object {
        inline val Static get() = Position("static")
        inline val Relative get() = Position("relative")
        inline val Absolute get() = Position("absolute")
        inline val Sticky get() = Position("sticky")
        inline val Fixed get() = Position("fixed")
    }
}
inline fun Position(value: String) = value.unsafeCast<Position>()

typealias LanguageCode = String

interface StepPosition: StylePropertyEnum {
    companion object {
        inline val JumpStart get() = StepPosition("jump-start")
        inline val JumpEnd get() = StepPosition("jump-end")
        inline val JumpNone get() = StepPosition("jump-none")
        inline val JumpBoth get() = StepPosition("jump-both")
        inline val Start get() = StepPosition("start")
        inline val End get() = StepPosition("end")
    }
}
inline fun StepPosition(value: String) = value.unsafeCast<StepPosition>()

interface AnimationTimingFunction: StylePropertyEnum {
    companion object {
        inline val Ease get() = AnimationTimingFunction("ease")
        inline val EaseIn get() = AnimationTimingFunction("ease-in")
        inline val EaseOut get() = AnimationTimingFunction("ease-out")
        inline val EaseInOut get() = AnimationTimingFunction("ease-in-out")
        inline val Linear get() = AnimationTimingFunction("linear")
        inline val StepStart get() = AnimationTimingFunction("step-start")
        inline val StepEnd get() = AnimationTimingFunction("step-end")

        inline fun cubicBezier(x1: Double, y1: Double, x2: Double, y2: Double) = AnimationTimingFunction("cubic-bezier($x1, $y1, $x2, $y2)")
        inline fun steps(count: Int, stepPosition: StepPosition) = AnimationTimingFunction("steps($count, $stepPosition)")
        inline fun steps(count: Int) = AnimationTimingFunction("steps($count)")

        inline val Inherit get() = AnimationTimingFunction("inherit")
        inline val Initial get() = AnimationTimingFunction("initial")
        inline val Unset get() = AnimationTimingFunction("unset")
    }
}
inline fun AnimationTimingFunction(value: String) = value.unsafeCast<AnimationTimingFunction>()

interface AnimationDirection: StylePropertyEnum {
    companion object {
        inline val Normal get() = AnimationDirection("normal")
        inline val Reverse get() = AnimationDirection("reverse")
        inline val Alternate get() = AnimationDirection("alternate")
        inline val AlternateReverse get() = AnimationDirection("alternate-reverse")

        inline val Inherit get() = AnimationDirection("inherit")
        inline val Initial get() = AnimationDirection("initial")
        inline val Unset get() = AnimationDirection("unset")
    }
}
inline fun AnimationDirection(value: String) = value.unsafeCast<AnimationDirection>()

interface AnimationFillMode: StylePropertyEnum {
    companion object {
        inline val None get() = AnimationFillMode("none")
        inline val Forwards get() = AnimationFillMode("forwards")
        inline val Backwards get() = AnimationFillMode("backwards")
        inline val Both get() = AnimationFillMode("both")
    }
}
inline fun AnimationFillMode(value: String) = value.unsafeCast<AnimationFillMode>()

interface AnimationPlayState: StylePropertyEnum {
    companion object {
        inline val Running get() = AnimationPlayState("running")
        inline val Paused get() = AnimationPlayState("Paused")
        inline val Backwards get() = AnimationPlayState("backwards")
        inline val Both get() = AnimationPlayState("both")

        inline val Inherit get() = AnimationPlayState("inherit")
        inline val Initial get() = AnimationPlayState("initial")
        inline val Unset get() = AnimationPlayState("unset")
    }
}
inline fun AnimationPlayState(value: String) = value.unsafeCast<AnimationPlayState>()


object GridAutoFlow : StylePropertyString  {
    inline val Row get() = "row".unsafeCast<GridAutoFlow>()
    inline val Column get() = "column".unsafeCast<GridAutoFlow>()
    inline val Dense get() = "dense".unsafeCast<GridAutoFlow>()
    inline val RowDense get() = "row dense".unsafeCast<GridAutoFlow>()
    inline val ColumnDense get() = "column dense".unsafeCast<GridAutoFlow>()
}

interface VisibilityStyle: StylePropertyEnum {
    companion object {
        inline val Visible get() = VisibilityStyle("visible")
        inline val Hidden get() = VisibilityStyle("hidden")
        inline val Collapse get() = VisibilityStyle("collapse")


        inline val Inherit get() = VisibilityStyle("inherit")
        inline val Initial get() = VisibilityStyle("initial")

        inline val Revert get() = VisibilityStyle("revert")
        inline val RevertLayer get() = VisibilityStyle("revert-layer")

        inline val Unset get() = VisibilityStyle("unset")
    }
}
inline fun VisibilityStyle(value: String) = value.unsafeCast<VisibilityStyle>()
