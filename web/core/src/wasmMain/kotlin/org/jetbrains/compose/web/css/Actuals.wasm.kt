package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi


open class StylePropertyStringWasmImpl(val value: String): StylePropertyString
open class StylePropertyNumberWasmImpl(val value: Number): StylePropertyNumber


inline actual fun StylePropertyValue(value: String): StylePropertyString = StylePropertyStringWasmImpl(value)
inline actual fun StylePropertyValue(value: Number): StylePropertyNumber = StylePropertyNumberWasmImpl(value)

open class CSSStyleValueWasmImpl(value: String): CSSStyleValue, StylePropertyStringWasmImpl(value) {
    override fun toString(): String {
        return value
    }
}
inline actual fun CSSStyleValue(value: String): CSSStyleValue = CSSStyleValueWasmImpl(value)

class CSSKeywordValueWasmImpl(value: String): CSSKeywordValue, CSSStyleValueWasmImpl(value)
inline actual fun CSSKeywordValue(value: String): CSSKeywordValue = CSSKeywordValueWasmImpl(value)

actual inline val StylePropertyEnum.name: String get() = (this as StylePropertyStringWasmImpl).value
actual inline val StylePropertyEnum.value: String get() = (this as StylePropertyStringWasmImpl).value

class LineStyleWasmImpl(value: String): LineStyle, StylePropertyStringWasmImpl(value)
inline actual fun LineStyle(value: String): LineStyle = LineStyleWasmImpl(value)

class DisplayStyleWasmImpl(value: String): DisplayStyle, StylePropertyStringWasmImpl(value)
inline actual fun DisplayStyle(value: String): DisplayStyle = DisplayStyleWasmImpl(value)

class FlexDirectionWasmImpl(value: String): FlexDirection, StylePropertyStringWasmImpl(value)
inline actual fun FlexDirection(value: String): FlexDirection = FlexDirectionWasmImpl(value)

class FlexWrapWasmImpl(value: String): FlexWrap, StylePropertyStringWasmImpl(value)
inline actual fun FlexWrap(value: String): FlexWrap = FlexWrapWasmImpl(value)

class JustifyContentWasmImpl(value: String): JustifyContent, StylePropertyStringWasmImpl(value)
inline actual fun JustifyContent(value: String): JustifyContent = JustifyContentWasmImpl(value)

class AlignSelfWasmImpl(value: String): AlignSelf, StylePropertyStringWasmImpl(value)
inline actual fun AlignSelf(value: String): AlignSelf = AlignSelfWasmImpl(value)

class AlignItemsWasmImpl(value: String): AlignItems, StylePropertyStringWasmImpl(value)
inline actual fun AlignItems(value: String): AlignItems = AlignItemsWasmImpl(value)

class AlignContentWasmImpl(value: String): AlignContent, StylePropertyStringWasmImpl(value)
inline actual fun AlignContent(value: String): AlignContent = AlignContentWasmImpl(value)

class PositionWasmImpl(value: String): Position, StylePropertyStringWasmImpl(value)
inline actual fun Position(value: String): Position = PositionWasmImpl(value)

class StepPositionWasmImpl(value: String): StepPosition, StylePropertyStringWasmImpl(value)
inline actual fun StepPosition(value: String): StepPosition = StepPositionWasmImpl(value)

class AnimationTimingFunctionWasmImpl(value: String): AnimationTimingFunction, StylePropertyStringWasmImpl(value)
inline actual fun AnimationTimingFunction(value: String): AnimationTimingFunction = AnimationTimingFunctionWasmImpl(value)

class AnimationDirectionWasmImpl(value: String): AnimationDirection, StylePropertyStringWasmImpl(value)
inline actual fun AnimationDirection(value: String): AnimationDirection = AnimationDirectionWasmImpl(value)

class AnimationFillModeWasmImpl(value: String): AnimationFillMode, StylePropertyStringWasmImpl(value)
inline actual fun AnimationFillMode(value: String): AnimationFillMode = AnimationFillModeWasmImpl(value)

class AnimationPlayStateWasmImpl(value: String): AnimationPlayState, StylePropertyStringWasmImpl(value)
inline actual fun AnimationPlayState(value: String): AnimationPlayState = AnimationPlayStateWasmImpl(value)

class GridAutoFlowWasmImpl(value: String): GridAutoFlow, StylePropertyStringWasmImpl(value)
inline actual fun GridAutoFlow(value: String): GridAutoFlow = GridAutoFlowWasmImpl(value)

@ComposeWebInternalApi
open class CSSUnitStringWrapper internal constructor(private val value: String) {
    override fun toString(): String {
        return value
    }
}

@ComposeWebInternalApi
object CssUnitPercentageWasmImpl : CSSUnit.percent, CSSUnitStringWrapper("%")

@ComposeWebInternalApi
object CssUnitEmWasmImpl : CSSUnit.em, CSSUnitStringWrapper("em")

@ComposeWebInternalApi
object CssUnitExWasmImpl : CSSUnit.ex, CSSUnitStringWrapper("ex")

@ComposeWebInternalApi
object CssUnitChWasmImpl : CSSUnit.ch, CSSUnitStringWrapper("ch")

@ComposeWebInternalApi
object CssUnitIcWasmImpl : CSSUnit.ic, CSSUnitStringWrapper("ic")

@ComposeWebInternalApi
object CssUnitRemWasmImpl : CSSUnit.rem, CSSUnitStringWrapper("rem")

@ComposeWebInternalApi
object CssUnitLhWasmImpl : CSSUnit.lh, CSSUnitStringWrapper("lh")

@ComposeWebInternalApi
object CssUnitRlhWasmImpl : CSSUnit.rlh, CSSUnitStringWrapper("rlh")

@ComposeWebInternalApi
object CssUnitVwWasmImpl : CSSUnit.vw, CSSUnitStringWrapper("vw")

@ComposeWebInternalApi
object CssUnitVhWasmImpl : CSSUnit.vh, CSSUnitStringWrapper("vh")

@ComposeWebInternalApi
object CssUnitViWasmImpl : CSSUnit.vi, CSSUnitStringWrapper("vi")

@ComposeWebInternalApi
object CssUnitVbWasmImpl : CSSUnit.vb, CSSUnitStringWrapper("vb")

@ComposeWebInternalApi
object CssUnitVminWasmImpl : CSSUnit.vmin, CSSUnitStringWrapper("vmin")

@ComposeWebInternalApi
object CssUnitVmaxWasmImpl : CSSUnit.vmax, CSSUnitStringWrapper("vmax")

@ComposeWebInternalApi
object CssUnitCmWasmImpl : CSSUnit.cm, CSSUnitStringWrapper("cm")

@ComposeWebInternalApi
object CssUnitMmWasmImpl : CSSUnit.mm, CSSUnitStringWrapper("mm")

@ComposeWebInternalApi
object CssUnitQWasmImpl : CSSUnit.Q, CSSUnitStringWrapper("Q")

@ComposeWebInternalApi
object CssUnitPtWasmImpl : CSSUnit.pt, CSSUnitStringWrapper("pt")

@ComposeWebInternalApi
object CssUnitPcWasmImpl : CSSUnit.pc, CSSUnitStringWrapper("pc")

@ComposeWebInternalApi
object CssUnitPxWasmImpl : CSSUnit.px, CSSUnitStringWrapper("px")

@ComposeWebInternalApi
object CssUnitDegWasmImpl : CSSUnit.deg, CSSUnitStringWrapper("deg")

@ComposeWebInternalApi
object CssUnitGradWasmImpl : CSSUnit.grad, CSSUnitStringWrapper("grad")

@ComposeWebInternalApi
object CssUnitRadWasmImpl : CSSUnit.rad, CSSUnitStringWrapper("rad")

@ComposeWebInternalApi
object CssUnitTurnWasmImpl : CSSUnit.turn, CSSUnitStringWrapper("turn")

@ComposeWebInternalApi
object CssUnitSWasmImpl : CSSUnit.s, CSSUnitStringWrapper("s")

@ComposeWebInternalApi
object CssUnitMsWasmImpl : CSSUnit.ms, CSSUnitStringWrapper("ms")

@ComposeWebInternalApi
object CssUnitHzWasmImpl : CSSUnit.Hz, CSSUnitStringWrapper("Hz")

@ComposeWebInternalApi
object CssUnitKHzWasmImpl : CSSUnit.kHz, CSSUnitStringWrapper("kHz")

@ComposeWebInternalApi
object CssUnitDpiWasmImpl : CSSUnit.dpi, CSSUnitStringWrapper("dpi")

@ComposeWebInternalApi
object CssUnitDpcmWasmImpl : CSSUnit.dpcm, CSSUnitStringWrapper("dpcm")

@ComposeWebInternalApi
object CssUnitDppxWasmImpl : CSSUnit.dppx, CSSUnitStringWrapper("dppx")

@ComposeWebInternalApi
object CssUnitFrWasmImpl : CSSUnit.fr, CSSUnitStringWrapper("fr")

@ComposeWebInternalApi
object CssUnitNumberWasmImpl : CSSUnit.number, CSSUnitStringWrapper("number")

@ComposeWebInternalApi
actual inline fun <reified T> String.castToCSSUnit(): T {
    return when (this) {
        "%" -> CssUnitPercentageWasmImpl
        "em" -> CssUnitEmWasmImpl
        "ex" -> CssUnitExWasmImpl
        "ch" -> CssUnitChWasmImpl
        "ic" -> CssUnitIcWasmImpl
        "rem" -> CssUnitRemWasmImpl
        "lh" -> CssUnitLhWasmImpl
        "rlh" -> CssUnitRlhWasmImpl
        "vw" -> CssUnitVwWasmImpl
        "vh" -> CssUnitVhWasmImpl
        "vi" -> CssUnitViWasmImpl
        "vb" -> CssUnitVbWasmImpl
        "vmin" -> CssUnitVminWasmImpl
        "vmax" -> CssUnitVmaxWasmImpl
        "cm" -> CssUnitCmWasmImpl
        "mm" -> CssUnitMmWasmImpl
        "Q" -> CssUnitQWasmImpl
        "pt" -> CssUnitPtWasmImpl
        "pc" -> CssUnitPcWasmImpl
        "px" -> CssUnitPxWasmImpl
        "deg" -> CssUnitDegWasmImpl
        "grad" -> CssUnitGradWasmImpl
        "rad" -> CssUnitRadWasmImpl
        "turn" -> CssUnitTurnWasmImpl
        "s" -> CssUnitSWasmImpl
        "ms" -> CssUnitMsWasmImpl
        "Hz" -> CssUnitHzWasmImpl
        "kHz" -> CssUnitKHzWasmImpl
        "dpi" -> CssUnitDpiWasmImpl
        "dpcm" -> CssUnitDpcmWasmImpl
        "dppx" -> CssUnitDppxWasmImpl
        "fr" -> CssUnitFrWasmImpl
        "number" -> CssUnitNumberWasmImpl
        else -> throw IllegalArgumentException("Unsupported CSS unit: $this")
    } as T
}

@ComposeWebInternalApi
value class CSSColorValueWasmImpl internal constructor(val value: String): CSSColorValue {
    override fun toString(): String = value
}

@OptIn(ComposeWebInternalApi::class)
actual fun Color(name: String): CSSColorValue = CSSColorValueWasmImpl(name)