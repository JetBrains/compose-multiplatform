package org.jetbrains.compose.web.css

typealias LanguageCode = String

open class StylePropertyStringValue(private val value: String) : StylePropertyString {
    override fun toString() = value
}

external interface LineStyle : StylePropertyString
external interface DisplayStyle : StylePropertyString
external interface FlexDirection : StylePropertyString
external interface FlexWrap : StylePropertyString
external interface JustifyContent : StylePropertyString
external interface AlignSelf : StylePropertyString
external interface AlignItems : StylePropertyString
external interface AlignContent : StylePropertyString
external interface Position : StylePropertyString
external interface StepPosition : StylePropertyString
interface AnimationTimingFunction : StylePropertyString {
    fun cubicBezier(x1: Double, y1: Double, x2: Double, y2: Double) =
        "cubic-bezier($x1, $y1, $x2, $y2)".unsafeCast<AnimationTimingFunction>()

    fun steps(count: Int, stepPosition: StepPosition) =
        "steps($count, $stepPosition)".unsafeCast<AnimationTimingFunction>()

    fun steps(count: Int) = "steps($count)".unsafeCast<AnimationTimingFunction>()
}

external interface AnimationDirection : StylePropertyString
external interface AnimationFillMode : StylePropertyString
external interface AnimationPlayState : StylePropertyString

object None : LineStyle, DisplayStyle, AnimationFillMode, StylePropertyStringValue("none")

val Hidden = "hidden".unsafeCast<LineStyle>()
val Dotted = "dotted".unsafeCast<LineStyle>()
val Dashed = "dashed".unsafeCast<LineStyle>()
val Solid = "solid".unsafeCast<LineStyle>()
val Double = "double".unsafeCast<LineStyle>()
val Groove = "groove".unsafeCast<LineStyle>()
val Ridge = "ridge".unsafeCast<LineStyle>()
val Inset = "inset".unsafeCast<LineStyle>()
val Outset = "outset".unsafeCast<LineStyle>()

val Block = "block".unsafeCast<DisplayStyle>()
val Inline = "inline".unsafeCast<DisplayStyle>()
val InlineBlock = "inline-block".unsafeCast<DisplayStyle>()
val Flex = "flex".unsafeCast<DisplayStyle>()
val LegacyInlineFlex = "inline-flex".unsafeCast<DisplayStyle>()
val Grid = "grid".unsafeCast<DisplayStyle>()
val LegacyInlineGrid = "inline-grid".unsafeCast<DisplayStyle>()
val FlowRoot = "flow-root".unsafeCast<DisplayStyle>()

val Contents = "contents".unsafeCast<DisplayStyle>()

// TODO(shabunc): Following properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far:
// "block flow", "inline flow", "inline flow-root",  "block flex",  "inline flex",  "block grid",  "inline grid", "block flow-root",

val Table = "table".unsafeCast<DisplayStyle>()
val TableRow = "table-row".unsafeCast<DisplayStyle>()
val ListItem = "list-item".unsafeCast<DisplayStyle>()

object Inherit : DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, AnimationTimingFunction,
    AnimationDirection, AnimationPlayState, StylePropertyStringValue("inherit")

object Initial : DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, AnimationTimingFunction,
    AnimationDirection, AnimationPlayState, StylePropertyStringValue("initial")

object Unset : DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, AnimationTimingFunction,
    AnimationDirection, AnimationPlayState, StylePropertyStringValue("unset")

val Row = "row".unsafeCast<FlexDirection>()
val RowReverse = "row-reverse".unsafeCast<FlexDirection>()
val Column = "column".unsafeCast<FlexDirection>()
val ColumnReverse = "column-reverse".unsafeCast<FlexDirection>()

val Wrap = "wrap".unsafeCast<FlexWrap>()
val Nowrap = "nowrap".unsafeCast<FlexWrap>()
val WrapReverse = "wrap-reverse".unsafeCast<FlexWrap>()

object Center : JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("center")
object Start : JustifyContent, AlignSelf, AlignItems, AlignContent, StepPosition, StylePropertyStringValue("start")
object End : JustifyContent, AlignSelf, AlignItems, AlignContent, StepPosition, StylePropertyStringValue("end")
object FlexStart : JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("flex-start")
object FlexEnd : JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("flex-end")
object Left : JustifyContent, StylePropertyStringValue("left")
object Right : JustifyContent, StylePropertyStringValue("right")
object Normal : JustifyContent, AlignSelf, AlignItems, AnimationDirection, StylePropertyStringValue("normal")
object SpaceBetween : JustifyContent, AlignContent, StylePropertyStringValue("space-between")
object SpaceAround : JustifyContent, AlignContent, StylePropertyStringValue("space-around")
object SpaceEvenly : JustifyContent, AlignContent, StylePropertyStringValue("space-evenly")
object Stretch : JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("stretch")
object SafeCenter : JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("safe center")
object UnsafeCenter : JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("unsafe center")

val Auto = "auto".unsafeCast<AlignSelf>()
val SelfStart = "self-start".unsafeCast<AlignSelf>()
val SelfEnd = "self-end".unsafeCast<AlignSelf>()
object Baseline : AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("baseline")

val Static = "static".unsafeCast<Position>()
val Relative = "relative".unsafeCast<Position>()
val Absolute = "absolute".unsafeCast<Position>()
val Sticky = "sticky".unsafeCast<Position>()
val Fixed = "fixed".unsafeCast<Position>()

val JumpStart = "jump-start".unsafeCast<StepPosition>()
val JumpEnd = "jump-end".unsafeCast<StepPosition>()
val JumpNone = "jump-none".unsafeCast<StepPosition>()
val JumpBoth = "jump-both".unsafeCast<StepPosition>()

val Ease = "ease".unsafeCast<AnimationTimingFunction>()
val EaseIn = "ease-in".unsafeCast<AnimationTimingFunction>()
val EaseOut = "ease-out".unsafeCast<AnimationTimingFunction>()
val EaseInOut = "ease-in-out".unsafeCast<AnimationTimingFunction>()
val Linear = "linear".unsafeCast<AnimationTimingFunction>()
val StepStart = "step-start".unsafeCast<AnimationTimingFunction>()
val StepEnd = "step-end".unsafeCast<AnimationTimingFunction>()

val Reverse = "reverse".unsafeCast<AnimationDirection>()
val Alternate = "alternate".unsafeCast<AnimationDirection>()
val AlternateReverse = "alternate-reverse".unsafeCast<AnimationDirection>()

val Forwards = "forwards".unsafeCast<AnimationFillMode>()
object Backwards : AnimationFillMode, AnimationPlayState, StylePropertyStringValue("backwards")
object Both : AnimationFillMode, AnimationPlayState, StylePropertyStringValue("both")

val Running = "running".unsafeCast<AnimationPlayState>()
val Paused = "paused".unsafeCast<AnimationPlayState>()