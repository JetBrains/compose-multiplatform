@file:Suppress("Unused", "NOTHING_TO_INLINE", "NESTED_CLASS_IN_EXTERNAL_INTERFACE", "INLINE_EXTERNAL_DECLARATION", "WRONG_BODY_OF_EXTERNAL_DECLARATION", "NESTED_EXTERNAL_DECLARATION", "ClassName")

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

interface CSSNumericValue<T : CSSUnit> : StylePropertyValue, CSSVariableValueAs<CSSNumericValue<T>>

interface CSSSizeValue<T : CSSUnit> : CSSNumericValue<T> {
    val value: Float
    val unit: T
}

data class CSSUnitValueTyped<T : CSSUnit>(
    override val value: Float,
    override val unit: T
) : CSSSizeValue<T> {
    override fun toString(): String = "$value$unit"
}

interface CSSUnitLengthOrPercentage: CSSUnit
interface CSSUnitPercentage: CSSUnitLengthOrPercentage
interface CSSUnitLength: CSSUnitLengthOrPercentage
interface CSSUnitRel : CSSUnitLength
interface CSSUnitAbs: CSSUnitLength
interface CSSUnitAngle: CSSUnit
interface CSSUnitTime: CSSUnit
interface CSSUnitFrequency: CSSUnit
interface CSSUnitResolution: CSSUnit
interface CSSUnitFlex: CSSUnit

typealias CSSAngleValue = CSSSizeValue<out CSSUnitAngle>
typealias CSSLengthOrPercentageValue = CSSSizeValue<out CSSUnitLengthOrPercentage>
typealias CSSLengthValue = CSSSizeValue<out CSSUnitLength>
typealias CSSPercentageValue = CSSSizeValue<out CSSUnitPercentage>
typealias CSSUnitValue = CSSSizeValue<out CSSUnit>
typealias CSSNumeric = CSSNumericValue<out CSSUnit>
typealias CSSpxValue = CSSSizeValue<CSSUnit.px>

// fake interfaces to distinguish units
interface CSSUnit {
    interface percent: CSSUnitPercentage

    interface em: CSSUnitRel

    interface ex: CSSUnitRel

    interface ch: CSSUnitRel

    interface ic: CSSUnitRel

    interface rem: CSSUnitRel

    interface lh: CSSUnitRel

    interface rlh: CSSUnitRel

    interface vw: CSSUnitRel

    interface vh: CSSUnitRel

    interface vi: CSSUnitRel

    interface vb: CSSUnitRel

    interface vmin: CSSUnitRel

    interface vmax: CSSUnitRel

    interface cm: CSSUnitRel

    interface mm: CSSUnitRel

    interface Q: CSSUnitRel

    interface pt: CSSUnitAbs

    interface pc: CSSUnitAbs

    interface px: CSSUnitAbs

    interface deg: CSSUnitAngle

    interface grad: CSSUnitAngle

    interface rad: CSSUnitAngle

    interface turn: CSSUnitAngle

    interface s: CSSUnitTime

    interface ms: CSSUnitTime

    interface Hz: CSSUnitFrequency

    interface kHz: CSSUnitFrequency

    interface dpi: CSSUnitResolution

    interface dpcm: CSSUnitResolution

    interface dppx: CSSUnitResolution

    interface fr: CSSUnitFlex

    interface number: CSSUnit

    @OptIn(ComposeWebInternalApi::class)
    companion object {
        inline val percent get() = "%".castToCSSUnit<percent>()

        inline val em get() = "em".castToCSSUnit<em>()

        inline val ex get() = "ex".castToCSSUnit<ex>()

        inline val ch get() = "ch".castToCSSUnit<ch>()

        inline val ic get() = "ic".castToCSSUnit<ic>()

        inline val rem get() = "rem".castToCSSUnit<rem>()

        inline val lh get() = "lh".castToCSSUnit<lh>()

        inline val rlh get() = "rlh".castToCSSUnit<rlh>()

        inline val vw get() = "vw".castToCSSUnit<vw>()

        inline val vh get() = "vh".castToCSSUnit<vh>()

        inline val vi get() = "vi".castToCSSUnit<vi>()

        inline val vb get() = "vb".castToCSSUnit<vb>()

        inline val vmin get() = "vmin".castToCSSUnit<vmin>()

        inline val vmax get() = "vmax".castToCSSUnit<vmax>()

        inline val cm get() = "cm".castToCSSUnit<cm>()

        inline val mm get() = "mm".castToCSSUnit<mm>()

        inline val Q get() = "Q".castToCSSUnit<Q>()

        inline val pt get() = "pt".castToCSSUnit<pt>()

        inline val pc get() = "pc".castToCSSUnit<pc>()

        inline val px get() = "px".castToCSSUnit<px>()

        inline val deg get() = "deg".castToCSSUnit<deg>()

        inline val grad get() = "grad".castToCSSUnit<grad>()

        inline val rad get() = "rad".castToCSSUnit<rad>()

        inline val turn get() = "turn".castToCSSUnit<turn>()

        inline val s get() = "s".castToCSSUnit<s>()

        inline val ms get() = "ms".castToCSSUnit<ms>()

        inline val Hz get() = "Hz".castToCSSUnit<Hz>()

        inline val kHz get() = "kHz".castToCSSUnit<kHz>()

        inline val dpi get() = "dpi".castToCSSUnit<dpi>()

        inline val dpcm get() = "dpcm".castToCSSUnit<dpcm>()

        inline val dppx get() = "dppx".castToCSSUnit<dppx>()

        inline val fr get() = "fr".castToCSSUnit<fr>()

        inline val number get() = "number".castToCSSUnit<number>()
    }
}

@ComposeWebInternalApi
expect inline fun <reified T> String.castToCSSUnit(): T

val Number.number
    get(): CSSSizeValue<CSSUnit.number> = CSSUnitValueTyped(this.toFloat(), CSSUnit.number)

val Number.percent
    get() : CSSSizeValue<CSSUnit.percent> = CSSUnitValueTyped(this.toFloat(), CSSUnit.percent)

val Number.em
    get() : CSSSizeValue<CSSUnit.em> = CSSUnitValueTyped(this.toFloat(), CSSUnit.em)

val Number.ex
    get(): CSSSizeValue<CSSUnit.ex> = CSSUnitValueTyped(this.toFloat(), CSSUnit.ex)

val Number.ch
    get(): CSSSizeValue<CSSUnit.ch> = CSSUnitValueTyped(this.toFloat(), CSSUnit.ch)

val Number.cssRem
    get(): CSSSizeValue<CSSUnit.rem> = CSSUnitValueTyped(this.toFloat(), CSSUnit.rem)

val Number.vw
    get(): CSSSizeValue<CSSUnit.vw> = CSSUnitValueTyped(this.toFloat(), CSSUnit.vw)

val Number.vh
    get(): CSSSizeValue<CSSUnit.vh> = CSSUnitValueTyped(this.toFloat(), CSSUnit.vh)

val Number.vmin
    get(): CSSSizeValue<CSSUnit.vmin> = CSSUnitValueTyped(this.toFloat(), CSSUnit.vmin)

val Number.vmax
    get(): CSSSizeValue<CSSUnit.vmax> = CSSUnitValueTyped(this.toFloat(), CSSUnit.vmax)

val Number.cm
    get(): CSSSizeValue<CSSUnit.cm> = CSSUnitValueTyped(this.toFloat(), CSSUnit.cm)

val Number.mm
    get(): CSSSizeValue<CSSUnit.mm> = CSSUnitValueTyped(this.toFloat(), CSSUnit.mm)

val Number.Q
    get() : CSSSizeValue<CSSUnit.Q> = CSSUnitValueTyped(this.toFloat(), CSSUnit.Q)

val Number.pt
    get(): CSSSizeValue<CSSUnit.pt> = CSSUnitValueTyped(this.toFloat(), CSSUnit.pt)
val Number.pc
    get(): CSSSizeValue<CSSUnit.pc> = CSSUnitValueTyped(this.toFloat(), CSSUnit.pc)
val Number.px
    get(): CSSSizeValue<CSSUnit.px> = CSSUnitValueTyped(this.toFloat(), CSSUnit.px)

val Number.deg
    get(): CSSSizeValue<CSSUnit.deg> = CSSUnitValueTyped(this.toFloat(), CSSUnit.deg)
val Number.grad
    get(): CSSSizeValue<CSSUnit.grad> = CSSUnitValueTyped(this.toFloat(), CSSUnit.grad)
val Number.rad
    get(): CSSSizeValue<CSSUnit.rad> = CSSUnitValueTyped(this.toFloat(), CSSUnit.rad)
val Number.turn
    get(): CSSSizeValue<CSSUnit.turn> = CSSUnitValueTyped(this.toFloat(), CSSUnit.turn)

val Number.s
    get(): CSSSizeValue<CSSUnit.s> = CSSUnitValueTyped(this.toFloat(), CSSUnit.s)
val Number.ms
    get(): CSSSizeValue<CSSUnit.ms> = CSSUnitValueTyped(this.toFloat(), CSSUnit.ms)

val Number.Hz
    get(): CSSSizeValue<CSSUnit.Hz> = CSSUnitValueTyped(this.toFloat(), CSSUnit.Hz)
val Number.kHz
    get(): CSSSizeValue<CSSUnit.kHz> = CSSUnitValueTyped(this.toFloat(), CSSUnit.kHz)

val Number.dpi
    get(): CSSSizeValue<CSSUnit.dpi> = CSSUnitValueTyped(this.toFloat(), CSSUnit.dpi)
val Number.dpcm
    get(): CSSSizeValue<CSSUnit.dpcm> = CSSUnitValueTyped(this.toFloat(), CSSUnit.dpcm)
val Number.dppx
    get(): CSSSizeValue<CSSUnit.dppx> = CSSUnitValueTyped(this.toFloat(), CSSUnit.dppx)

val Number.fr
    get(): CSSSizeValue<CSSUnit.fr> = CSSUnitValueTyped(this.toFloat(), CSSUnit.fr)
