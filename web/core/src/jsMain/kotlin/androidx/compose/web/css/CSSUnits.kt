package org.jetbrains.compose.web.css

interface CSSSizeValue<out T : CSSUnit> : CSSNumericValue {
    val value: Float
    val unit: T
    fun asString(): String = "${value}${unit.value}"
    fun newUnit(value: Float): CSSSizeValue<T>
}

private data class CSSUnitValueTyped<out T : CSSUnit>(
    override val value: Float,
    override val unit: T
) : CSSSizeValue<T> {
    override fun newUnit(value: Float): CSSSizeValue<T> = copy(value = value)
}

operator fun <T : CSSUnit> CSSSizeValue<T>.times(num: Number): CSSSizeValue<T> = newUnit(value * num.toFloat())
operator fun <T : CSSUnit> Number.times(unit: CSSSizeValue<T>): CSSSizeValue<T> = unit.newUnit(unit.value * toFloat())

operator fun <T : CSSUnit> CSSSizeValue<T>.div(num: Number): CSSSizeValue<T> = newUnit(value / num.toFloat())

operator fun <T: CSSUnit> CSSSizeValue<T>.plus(b: CSSSizeValue<T>): CSSSizeValue<T> = newUnit(value + b.value)
operator fun <T: CSSUnit> CSSSizeValue<T>.minus(b: CSSSizeValue<T>): CSSSizeValue<T> = newUnit(value - b.value)

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

typealias CSSAngleValue = CSSSizeValue<CSSUnitAngle>
typealias CSSLengthOrPercentageValue = CSSSizeValue<CSSUnitLengthOrPercentage>
typealias CSSLengthValue = CSSSizeValue<CSSUnitLength>
typealias CSSPercentageValue = CSSSizeValue<CSSUnitPercentage>
typealias CSSUnitValue = CSSSizeValue<CSSUnit>
typealias CSSpxValue = CSSSizeValue<CSSUnit.px>


sealed interface CSSUnit {
    val value: String

    object percent: CSSUnitPercentage {
        override val value: String = "%"
    }

    object em: CSSUnitRel {
        override val value = "em"
    }

    object ex: CSSUnitRel {
        override val value = "ex"
    }

    object ch: CSSUnitRel {
        override val value = "ch"
    }

    object ic: CSSUnitRel {
        override val value = "ic"
    }

    object rem: CSSUnitRel {
        override val value = "rem"
    }

    object lh: CSSUnitRel {
        override val value = "lh"
    }

    object rlh: CSSUnitRel {
        override val value = "rlh"
    }

    object vw: CSSUnitRel {
        override val value = "vw"
    }

    object vh: CSSUnitRel {
        override val value = "vh"
    }

    object vi: CSSUnitRel {
        override val value = "vi"
    }

    object vb: CSSUnitRel {
        override val value = "vb"
    }

    object vmin: CSSUnitRel {
        override val value = "vmin"
    }

    object vmax: CSSUnitRel {
        override val value = "vmax"
    }

    object cm: CSSUnitRel {
        override val value = "cm"
    }

    object mm: CSSUnitRel {
        override val value = "mm"
    }

    object q: CSSUnitRel {
        override val value = "q"
    }

    object pt: CSSUnitAbs {
        override val value = "pt"
    }

    object pc: CSSUnitAbs {
        override val value = "pc"
    }

    object px: CSSUnitAbs {
        override val value = "px"
    }

    object deg: CSSUnitAngle {
        override val value = "deg"
    }

    object grad: CSSUnitAngle {
        override val value = "grad"
    }

    object rad: CSSUnitAngle {
        override val value = "rad"
    }

    object turn: CSSUnitAngle {
        override val value = "turn"
    }

    object s: CSSUnitTime {
        override val value = "s"
    }

    object ms: CSSUnitTime {
        override val value = "ms"
    }

    object hz: CSSUnitFrequency {
        override val value = "hz"
    }

    object khz: CSSUnitFrequency {
        override val value = "khz"
    }

    object dpi: CSSUnitResolution {
        override val value = "dpi"
    }

    object dpcm: CSSUnitResolution {
        override val value = "dpcm"
    }

    object dppx: CSSUnitResolution {
        override val value = "dppx"
    }

    object fr: CSSUnitFlex {
        override val value = "fr"
    }

    object number: CSSUnit {
        override val value = "number"
    }
}


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
    get() : CSSSizeValue<CSSUnit.q> = CSSUnitValueTyped(this.toFloat(), CSSUnit.q)

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
    get(): CSSSizeValue<CSSUnit.hz> = CSSUnitValueTyped(this.toFloat(), CSSUnit.hz)
val Number.kHz
    get(): CSSSizeValue<CSSUnit.khz> = CSSUnitValueTyped(this.toFloat(), CSSUnit.khz)

val Number.dpi
    get(): CSSSizeValue<CSSUnit.dpi> = CSSUnitValueTyped(this.toFloat(), CSSUnit.dpi)
val Number.dpcm
    get(): CSSSizeValue<CSSUnit.dpcm> = CSSUnitValueTyped(this.toFloat(), CSSUnit.dpcm)
val Number.dppx
    get(): CSSSizeValue<CSSUnit.dppx> = CSSUnitValueTyped(this.toFloat(), CSSUnit.dppx)

val Number.fr
    get(): CSSSizeValue<CSSUnit.fr> = CSSUnitValueTyped(this.toFloat(), CSSUnit.fr)
