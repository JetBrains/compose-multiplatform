package org.jetbrains.compose.web.css

interface CSSRelValue
interface CSSAbsValue
interface CSSAngleValue
interface CSSTimeValue
interface CSSFrequencyValue
interface CSSResolutionValue
interface CSSFlexValue

class CSSpercentValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "percent"
}

class CSSemValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "em"
}

class CSSexValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "ex"
}

class CSSchValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "ch"
}

class CSSicValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "ic"
}

class CSSremValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "rem"
}

class CSSlhValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "lh"
}

class CSSrlhValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "rlh"
}

class CSSvwValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "vw"
}

class CSSvhValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "vh"
}

class CSSviValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "vi"
}

class CSSvbValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "vb"
}

class CSSvminValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "vmin"
}

class CSSvmaxValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "vmax"
}

class CSScmValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "cm"
}

class CSSmmValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "mm"
}

class CSSQValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: String
        get() = "q"
}

class CSSptValue(override val value: Number) : CSSUnitValue, CSSAbsValue {
    override val unit: String
        get() = "pt"
}

class CSSpcValue(override val value: Number) : CSSUnitValue, CSSAbsValue {
    override val unit: String
        get() = "pc"
}

class CSSpxValue(override val value: Number) : CSSUnitValue, CSSAbsValue {
    override val unit: String
        get() = "px"
}

class CSSdegValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: String
        get() = "deg"
}

class CSSgradValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: String
        get() = "grad"
}

class CSSradValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: String
        get() = "rad"
}

class CSSturnValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: String
        get() = "turn"
}

class CSSsValue(override val value: Number) : CSSUnitValue, CSSTimeValue {
    override val unit: String
        get() = "s"
}

class CSSmsValue(override val value: Number) : CSSUnitValue, CSSTimeValue {
    override val unit: String
        get() = "ms"
}

class CSSHzValue(override val value: Number) : CSSUnitValue, CSSFrequencyValue {
    override val unit: String
        get() = "hz"
}

class CSSkHzValue(override val value: Number) : CSSUnitValue, CSSFrequencyValue {
    override val unit: String
        get() = "khz"
}

class CSSdpiValue(override val value: Number) : CSSUnitValue, CSSResolutionValue {
    override val unit: String
        get() = "dpi"
}

class CSSdpcmValue(override val value: Number) : CSSUnitValue, CSSResolutionValue {
    override val unit: String
        get() = "dpcm"
}

class CSSdppxValue(override val value: Number) : CSSUnitValue, CSSResolutionValue {
    override val unit: String
        get() = "dppx"
}

class CSSfrValue(override val value: Number) : CSSUnitValue, CSSFlexValue {
    override val unit: String
        get() = "fr"
}

class CSSnumberValue(override val value: Number) : CSSUnitValue, CSSFlexValue {
    override val unit: String
    get() = "number"
}

val Number.number
    get(): CSSUnitValue = CSSnumberValue(this)

val Number.percent
    get(): CSSpercentValue = CSSpercentValue(this)

val Number.em
    get(): CSSemValue = CSSemValue(this)
val Number.ex
    get(): CSSexValue = CSSexValue(this)
val Number.ch
    get(): CSSchValue = CSSchValue(this)
val Number.cssRem
    get(): CSSremValue = CSSremValue(this)
val Number.vw
    get(): CSSvwValue = CSSvwValue(this)
val Number.vh
    get(): CSSvhValue = CSSvhValue(this)
val Number.vmin
    get(): CSSvminValue = CSSvminValue(this)
val Number.vmax
    get(): CSSvmaxValue = CSSvmaxValue(this)
val Number.cm
    get(): CSScmValue = CSScmValue(this)
val Number.mm
    get(): CSSmmValue = CSSmmValue(this)
val Number.Q
    get(): CSSQValue = CSSQValue(this)

val Number.pt
    get(): CSSptValue = CSSptValue(this)
val Number.pc
    get(): CSSpcValue = CSSpcValue(this)
val Number.px
    get(): CSSpxValue = CSSpxValue(this)

val Number.deg
    get(): CSSdegValue = CSSdegValue(this)
val Number.grad
    get(): CSSgradValue = CSSgradValue(this)
val Number.rad
    get(): CSSradValue = CSSradValue(this)
val Number.turn
    get(): CSSturnValue = CSSturnValue(this)

val Number.s
    get(): CSSsValue = CSSsValue(this)
val Number.ms
    get(): CSSmsValue = CSSmsValue(this)

val Number.Hz
    get(): CSSHzValue = CSSHzValue(this)
val Number.kHz
    get(): CSSkHzValue = CSSkHzValue(this)

val Number.dpi
    get(): CSSdpiValue = CSSdpiValue(this)
val Number.dpcm
    get(): CSSdpcmValue = CSSdpcmValue(this)
val Number.dppx
    get(): CSSdppxValue = CSSdppxValue(this)

val Number.fr
    get(): CSSfrValue = CSSfrValue(this)
