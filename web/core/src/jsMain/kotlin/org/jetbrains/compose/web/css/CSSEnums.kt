@file:Suppress(
    "Unused",
    "NOTHING_TO_INLINE",
    "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "INLINE_EXTERNAL_DECLARATION",
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "NESTED_EXTERNAL_DECLARATION"
)

package org.jetbrains.compose.web.css

typealias LanguageCode = String

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
interface AnimationTimingFunction : StylePropertyString {
    fun cubicBezier(x1: Double, y1: Double, x2: Double, y2: Double) =
        "cubic-bezier($x1, $y1, $x2, $y2)".unsafeCast<AnimationTimingFunction>()

    fun steps(count: Int, stepPosition: StepPosition) =
        "steps($count, $stepPosition)".unsafeCast<AnimationTimingFunction>()

    fun steps(count: Int) = "steps($count)".unsafeCast<AnimationTimingFunction>()
}

interface AnimationDirection : StylePropertyString
interface AnimationFillMode : StylePropertyString
interface AnimationPlayState : StylePropertyString

object None : LineStyle, DisplayStyle, AnimationFillMode, StylePropertyStringValue("none")

object Hidden : LineStyle, StylePropertyStringValue("hidden")
object Dotted : LineStyle, StylePropertyStringValue("dotted")
object Dashed : LineStyle, StylePropertyStringValue("dashed")
object Solid : LineStyle, StylePropertyStringValue("solid")
object Double : LineStyle, StylePropertyStringValue("double")
object Groove : LineStyle, StylePropertyStringValue("groove")
object Ridge : LineStyle, StylePropertyStringValue("ridge")
object Inset : LineStyle, StylePropertyStringValue("inset")
object Outset : LineStyle, StylePropertyStringValue("outset")

object Block : DisplayStyle, StylePropertyStringValue("block")
object Inline : DisplayStyle, StylePropertyStringValue("inline")
object InlineBlock : DisplayStyle, StylePropertyStringValue("inline-block")
object Flex : DisplayStyle, StylePropertyStringValue("flex")
object LegacyInlineFlex : DisplayStyle, StylePropertyStringValue("inline-flex")
object Grid : DisplayStyle, StylePropertyStringValue("grid")
object LegacyInlineGrid : DisplayStyle, StylePropertyStringValue("inline-grid")
object FlowRoot : DisplayStyle, StylePropertyStringValue("flow-root")

object Contents : DisplayStyle, StylePropertyStringValue("contents")

// TODO(shabunc): Following properties behave them iconsistenly in both Chrome and Firefox so I turned the off so far:
// "block flow", "inline flow", "inline flow-root",  "block flex",  "inline flex",  "block grid",  "inline grid", "block flow-root",

object Table : DisplayStyle, StylePropertyStringValue("table")
object TableRow : DisplayStyle, StylePropertyStringValue("table-row")
object ListItem : DisplayStyle, StylePropertyStringValue("list-item")

object Inherit : DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, AnimationTimingFunction,
    AnimationDirection, AnimationPlayState, StylePropertyStringValue("inherit")

object Initial : DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, AnimationTimingFunction,
    AnimationDirection, AnimationPlayState, StylePropertyStringValue("initial")

object Unset : DisplayStyle, JustifyContent, AlignSelf, AlignItems, AlignContent, AnimationTimingFunction,
    AnimationDirection, AnimationPlayState, StylePropertyStringValue("unset")

object Row : FlexDirection, StylePropertyStringValue("row")
object RowReverse : FlexDirection, StylePropertyStringValue("row-reverse")
object Column : FlexDirection, StylePropertyStringValue("column")
object ColumnReverse : FlexDirection, StylePropertyStringValue("column-reverse")

object Wrap : FlexWrap, StylePropertyStringValue("wrap")
object Nowrap : FlexWrap, StylePropertyStringValue("nowrap")
object WrapReverse : FlexWrap, StylePropertyStringValue("wrap-reverse")

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

object Auto : AlignSelf, StylePropertyStringValue("auto")
object SelfStart : AlignSelf, StylePropertyStringValue("self-start")
object SelfEnd : AlignSelf, StylePropertyStringValue("self-end")
object Baseline : AlignSelf, AlignItems, AlignContent, StylePropertyStringValue("baseline")

object Static : Position, StylePropertyStringValue("static")
object Relative : Position, StylePropertyStringValue("relative")
object Absolute : Position, StylePropertyStringValue("absolute")
object Sticky : Position, StylePropertyStringValue("sticky")
object Fixed : Position, StylePropertyStringValue("fixed")


object JumpStart : StepPosition, StylePropertyStringValue("jump-start")
object JumpEnd : StepPosition, StylePropertyStringValue("jump-end")
object JumpNone : StepPosition, StylePropertyStringValue("jump-none")
object JumpBoth : StepPosition, StylePropertyStringValue("jump-both")

object Ease : AnimationTimingFunction, StylePropertyStringValue("ease")
object EaseIn : AnimationTimingFunction, StylePropertyStringValue("ease-in")
object EaseOut : AnimationTimingFunction, StylePropertyStringValue("ease-out")
object EaseInOut : AnimationTimingFunction, StylePropertyStringValue("ease-in-out")
object Linear : AnimationTimingFunction, StylePropertyStringValue("linear")
object StepStart : AnimationTimingFunction, StylePropertyStringValue("step-start")
object StepEnd : AnimationTimingFunction, StylePropertyStringValue("step-end")

object Reverse : AnimationDirection, StylePropertyStringValue("reverse")
object Alternate : AnimationDirection, StylePropertyStringValue("alternate")
object AlternateReverse : AnimationDirection, StylePropertyStringValue("alternate-reverse")

object Forwards : AnimationFillMode, StylePropertyStringValue("forwards")
object Backwards : AnimationFillMode, AnimationPlayState, StylePropertyStringValue("backwards")
object Both : AnimationFillMode, AnimationPlayState, StylePropertyStringValue("both")

object Running : AnimationPlayState, StylePropertyStringValue("running")
object Paused : AnimationPlayState, StylePropertyStringValue("paused")