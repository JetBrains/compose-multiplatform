@file:Suppress("Unused", "ClassName")

package org.jetbrains.compose.web.css

expect interface CSSNumericValue<T : CSSUnit> :
    StylePropertyValue,
    CSSVariableValueAs<CSSNumericValue<T>>

expect interface CSSSizeValue<T : CSSUnit> : CSSNumericValue<T> {
    val value: Float
    val unit: T
}

data class CSSUnitValueTyped<T : CSSUnit>(
    override val value: Float,
    override val unit: T,
) : CSSSizeValue<T> {
    override fun toString(): String = "${value.toCssString()}$unit"
}

private fun Float.toCssString(): String {
    if (this == 0f) return "0"
    if (!isFinite()) return toString()

    val string = toString()
    val exponentIndex = string.indexOfFirst { it == 'e' || it == 'E' }
    val mantissa = if (exponentIndex == -1) string else string.substring(0, exponentIndex)
    val exponent = if (exponentIndex == -1) "" else string.substring(exponentIndex)
    val normalizedMantissa = mantissa.removeSuffix(".0")

    return normalizedMantissa + exponent
}

interface CSSUnitLengthOrPercentage : CSSUnit
interface CSSUnitPercentage : CSSUnitLengthOrPercentage
interface CSSUnitLength : CSSUnitLengthOrPercentage
interface CSSUnitRel : CSSUnitLength
interface CSSUnitAbs : CSSUnitLength
interface CSSUnitAngle : CSSUnit
interface CSSUnitTime : CSSUnit
interface CSSUnitFrequency : CSSUnit
interface CSSUnitResolution : CSSUnit
interface CSSUnitFlex : CSSUnit

typealias CSSAngleValue = CSSSizeValue<out CSSUnitAngle>
typealias CSSLengthOrPercentageValue = CSSSizeValue<out CSSUnitLengthOrPercentage>
typealias CSSLengthValue = CSSSizeValue<out CSSUnitLength>
typealias CSSPercentageValue = CSSSizeValue<out CSSUnitPercentage>
typealias CSSUnitValue = CSSSizeValue<out CSSUnit>
typealias CSSNumeric = CSSNumericValue<out CSSUnit>
typealias CSSpxValue = CSSSizeValue<CSSUnit.px>

// Marker interfaces preserve the compile-time distinctions between CSS units.
interface CSSUnit {
    interface percent : CSSUnitPercentage

    interface em : CSSUnitRel
    interface ex : CSSUnitRel
    interface ch : CSSUnitRel
    interface ic : CSSUnitRel
    interface rem : CSSUnitRel
    interface lh : CSSUnitRel
    interface rlh : CSSUnitRel
    interface vw : CSSUnitRel
    interface vh : CSSUnitRel
    interface vi : CSSUnitRel
    interface vb : CSSUnitRel
    interface vmin : CSSUnitRel
    interface vmax : CSSUnitRel
    interface cm : CSSUnitRel
    interface mm : CSSUnitRel
    interface Q : CSSUnitRel

    interface pt : CSSUnitAbs
    interface pc : CSSUnitAbs
    interface px : CSSUnitAbs

    interface deg : CSSUnitAngle
    interface grad : CSSUnitAngle
    interface rad : CSSUnitAngle
    interface turn : CSSUnitAngle

    interface s : CSSUnitTime
    interface ms : CSSUnitTime

    interface Hz : CSSUnitFrequency
    interface kHz : CSSUnitFrequency

    interface dpi : CSSUnitResolution
    interface dpcm : CSSUnitResolution
    interface dppx : CSSUnitResolution

    interface fr : CSSUnitFlex
    interface number : CSSUnit

    companion object {
        val percent: CSSUnit.percent get() = PercentUnit

        val em: CSSUnit.em get() = EmUnit
        val ex: CSSUnit.ex get() = ExUnit
        val ch: CSSUnit.ch get() = ChUnit
        val ic: CSSUnit.ic get() = IcUnit
        val rem: CSSUnit.rem get() = RemUnit
        val lh: CSSUnit.lh get() = LhUnit
        val rlh: CSSUnit.rlh get() = RlhUnit
        val vw: CSSUnit.vw get() = VwUnit
        val vh: CSSUnit.vh get() = VhUnit
        val vi: CSSUnit.vi get() = ViUnit
        val vb: CSSUnit.vb get() = VbUnit
        val vmin: CSSUnit.vmin get() = VminUnit
        val vmax: CSSUnit.vmax get() = VmaxUnit
        val cm: CSSUnit.cm get() = CmUnit
        val mm: CSSUnit.mm get() = MmUnit
        val Q: CSSUnit.Q get() = QUnit

        val pt: CSSUnit.pt get() = PtUnit
        val pc: CSSUnit.pc get() = PcUnit
        val px: CSSUnit.px get() = PxUnit

        val deg: CSSUnit.deg get() = DegUnit
        val grad: CSSUnit.grad get() = GradUnit
        val rad: CSSUnit.rad get() = RadUnit
        val turn: CSSUnit.turn get() = TurnUnit

        val s: CSSUnit.s get() = SUnit
        val ms: CSSUnit.ms get() = MsUnit

        val Hz: CSSUnit.Hz get() = HzUnit
        val kHz: CSSUnit.kHz get() = KHzUnit

        val dpi: CSSUnit.dpi get() = DpiUnit
        val dpcm: CSSUnit.dpcm get() = DpcmUnit
        val dppx: CSSUnit.dppx get() = DppxUnit

        val fr: CSSUnit.fr get() = FrUnit
        val number: CSSUnit.number get() = NumberUnit
    }
}

private abstract class CSSUnitToken(
    private val value: String,
) : CSSUnit {
    final override fun toString(): String = value
}

private object PercentUnit : CSSUnitToken("%"), CSSUnit.percent

private object EmUnit : CSSUnitToken("em"), CSSUnit.em
private object ExUnit : CSSUnitToken("ex"), CSSUnit.ex
private object ChUnit : CSSUnitToken("ch"), CSSUnit.ch
private object IcUnit : CSSUnitToken("ic"), CSSUnit.ic
private object RemUnit : CSSUnitToken("rem"), CSSUnit.rem
private object LhUnit : CSSUnitToken("lh"), CSSUnit.lh
private object RlhUnit : CSSUnitToken("rlh"), CSSUnit.rlh
private object VwUnit : CSSUnitToken("vw"), CSSUnit.vw
private object VhUnit : CSSUnitToken("vh"), CSSUnit.vh
private object ViUnit : CSSUnitToken("vi"), CSSUnit.vi
private object VbUnit : CSSUnitToken("vb"), CSSUnit.vb
private object VminUnit : CSSUnitToken("vmin"), CSSUnit.vmin
private object VmaxUnit : CSSUnitToken("vmax"), CSSUnit.vmax
private object CmUnit : CSSUnitToken("cm"), CSSUnit.cm
private object MmUnit : CSSUnitToken("mm"), CSSUnit.mm
private object QUnit : CSSUnitToken("Q"), CSSUnit.Q

private object PtUnit : CSSUnitToken("pt"), CSSUnit.pt
private object PcUnit : CSSUnitToken("pc"), CSSUnit.pc
private object PxUnit : CSSUnitToken("px"), CSSUnit.px

private object DegUnit : CSSUnitToken("deg"), CSSUnit.deg
private object GradUnit : CSSUnitToken("grad"), CSSUnit.grad
private object RadUnit : CSSUnitToken("rad"), CSSUnit.rad
private object TurnUnit : CSSUnitToken("turn"), CSSUnit.turn

private object SUnit : CSSUnitToken("s"), CSSUnit.s
private object MsUnit : CSSUnitToken("ms"), CSSUnit.ms

private object HzUnit : CSSUnitToken("Hz"), CSSUnit.Hz
private object KHzUnit : CSSUnitToken("kHz"), CSSUnit.kHz

private object DpiUnit : CSSUnitToken("dpi"), CSSUnit.dpi
private object DpcmUnit : CSSUnitToken("dpcm"), CSSUnit.dpcm
private object DppxUnit : CSSUnitToken("dppx"), CSSUnit.dppx

private object FrUnit : CSSUnitToken("fr"), CSSUnit.fr
private object NumberUnit : CSSUnitToken("number"), CSSUnit.number

val Number.number: CSSSizeValue<CSSUnit.number>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.number)

val Number.percent: CSSSizeValue<CSSUnit.percent>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.percent)

val Number.em: CSSSizeValue<CSSUnit.em>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.em)

val Number.ex: CSSSizeValue<CSSUnit.ex>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.ex)

val Number.ch: CSSSizeValue<CSSUnit.ch>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.ch)

val Number.cssRem: CSSSizeValue<CSSUnit.rem>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.rem)

val Number.vw: CSSSizeValue<CSSUnit.vw>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.vw)

val Number.vh: CSSSizeValue<CSSUnit.vh>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.vh)

val Number.vmin: CSSSizeValue<CSSUnit.vmin>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.vmin)

val Number.vmax: CSSSizeValue<CSSUnit.vmax>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.vmax)

val Number.cm: CSSSizeValue<CSSUnit.cm>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.cm)

val Number.mm: CSSSizeValue<CSSUnit.mm>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.mm)

val Number.Q: CSSSizeValue<CSSUnit.Q>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.Q)

val Number.pt: CSSSizeValue<CSSUnit.pt>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.pt)

val Number.pc: CSSSizeValue<CSSUnit.pc>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.pc)

val Number.px: CSSSizeValue<CSSUnit.px>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.px)

val Number.deg: CSSSizeValue<CSSUnit.deg>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.deg)

val Number.grad: CSSSizeValue<CSSUnit.grad>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.grad)

val Number.rad: CSSSizeValue<CSSUnit.rad>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.rad)

val Number.turn: CSSSizeValue<CSSUnit.turn>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.turn)

val Number.s: CSSSizeValue<CSSUnit.s>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.s)

val Number.ms: CSSSizeValue<CSSUnit.ms>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.ms)

val Number.Hz: CSSSizeValue<CSSUnit.Hz>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.Hz)

val Number.kHz: CSSSizeValue<CSSUnit.kHz>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.kHz)

val Number.dpi: CSSSizeValue<CSSUnit.dpi>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.dpi)

val Number.dpcm: CSSSizeValue<CSSUnit.dpcm>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.dpcm)

val Number.dppx: CSSSizeValue<CSSUnit.dppx>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.dppx)

val Number.fr: CSSSizeValue<CSSUnit.fr>
    get() = CSSUnitValueTyped(toFloat(), CSSUnit.fr)
