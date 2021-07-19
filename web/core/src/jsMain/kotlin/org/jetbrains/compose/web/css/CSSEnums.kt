@file:Suppress(
    "Unused",
    "NOTHING_TO_INLINE",
    "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "INLINE_EXTERNAL_DECLARATION",
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "NESTED_EXTERNAL_DECLARATION"
)

package org.jetbrains.compose.web.css

open class StylePropertyStringValue(private val value: String) : StylePropertyString {
    override fun toString() = value
}

interface LineStyle : StylePropertyString
interface DisplayStyle : StylePropertyString
interface FlexDirection : StylePropertyString
interface FlexWrap : StylePropertyString
interface JustifyContent : StylePropertyString
interface AlignSelf : StylePropertyString
interface AlignItems : StylePropertyString
interface AlignContent : StylePropertyString
interface Position : StylePropertyString
interface StepPosition : StylePropertyString

object None: LineStyle, DisplayStyle, StylePropertyStringValue("none")

object Hidden: LineStyle, StylePropertyStringValue("hidden")
object Dotted: LineStyle, StylePropertyStringValue("dotted")
object Dashed: LineStyle, StylePropertyStringValue("dashed")
object Solid: LineStyle, StylePropertyStringValue("solid")
object Double: LineStyle, StylePropertyStringValue("double")
object Groove: LineStyle, StylePropertyStringValue("groove")
object Ridge: LineStyle, StylePropertyStringValue("ridge")
object Inset: LineStyle, StylePropertyStringValue("inset")
object Outset: LineStyle, StylePropertyStringValue("outset")


object Block: DisplayStyle, StylePropertyStringValue("block")
object Inline: DisplayStyle, StylePropertyStringValue("inline")
object InlineBlock: DisplayStyle, StylePropertyStringValue("inline-block")
object Flex: DisplayStyle, StylePropertyStringValue("flex")
object LegacyInlineFlex: DisplayStyle, StylePropertyStringValue("inline-flex")
object Grid: DisplayStyle, StylePropertyStringValue("grid")
object LegacyInlineGrid: DisplayStyle, StylePropertyStringValue("inline-grid")
object FlowRoot: DisplayStyle, StylePropertyStringValue("flow-root")

object Contents: DisplayStyle, StylePropertyStringValue("contents")

// TODO(shabunc): Following properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far:
// "block flow", "inline flow", "inline flow-root",  "block flex",  "inline flex",  "block grid",  "inline grid", "block flow-root",

object Table: DisplayStyle, StylePropertyStringValue("table")
object TableRow: DisplayStyle, StylePropertyStringValue("table-row")
object ListItem: DisplayStyle, StylePropertyStringValue("list-item")

object Inherit: DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("inherit")
object Initial: DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("initial")
object Unset: DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("unset")

object Row: FlexDirection, StylePropertyStringValue("row")
object RowReverse: FlexDirection, StylePropertyStringValue("row-reverse")
object Column: FlexDirection, StylePropertyStringValue("column")
object ColumnReverse: FlexDirection, StylePropertyStringValue("column-reverse")

object Wrap: FlexWrap, StylePropertyStringValue("wrap")
object Nowrap: FlexWrap, StylePropertyStringValue("nowrap")
object WrapReverse: FlexWrap, StylePropertyStringValue("wrap-reverse")

object Center: JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("center")
object Start: JustifyContent, AlignSelf, AlignItems, AlignContent, StepPosition, StylePropertyStringValue("start")
object End: JustifyContent, AlignSelf, AlignItems, AlignContent, StepPosition, StylePropertyStringValue("end")
object FlexStart: JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("flex-start")
object FlexEnd: JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("flex-end")
object Left: JustifyContent, StylePropertyStringValue("left")
object Right: JustifyContent, StylePropertyStringValue("right")
object Normal: JustifyContent, AlignSelf, AlignItems, StylePropertyStringValue("normal")
object SpaceBetween: JustifyContent, AlignContent, StylePropertyStringValue("space-between")
object SpaceAround: JustifyContent, AlignContent, StylePropertyStringValue("space-around")
object SpaceEvenly: JustifyContent, AlignContent, StylePropertyStringValue("space-evenly")
object Stretch: JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("stretch")
object SafeCenter: JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("safe center")
object UnsafeCenter: JustifyContent, AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("unsafe center")

object Auto : AlignSelf, StylePropertyStringValue("auto")
object SelfStart : AlignSelf, StylePropertyStringValue("self-start")
object SelfEnd : AlignSelf, StylePropertyStringValue("self-end")
object Baseline : AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("baseline")

object Static : Position, StylePropertyStringValue("static")
object Relative : Position, StylePropertyStringValue("relative")
object Absolute : Position, StylePropertyStringValue("absolute")
object Sticky : Position, StylePropertyStringValue("sticky")
object Fixed : Position, StylePropertyStringValue("fixed")

typealias LanguageCode = String

object JumpStart : StepPosition, StylePropertyStringValue("jump-start")
object JumpEnd : StepPosition, StylePropertyStringValue("jump-end")
object JumpNone : StepPosition, StylePropertyStringValue("jump-none")
object JumpBoth : StepPosition, StylePropertyStringValue("jump-both")

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