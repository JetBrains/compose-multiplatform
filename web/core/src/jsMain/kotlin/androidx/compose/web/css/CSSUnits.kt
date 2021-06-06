package org.jetbrains.compose.web.css

interface CSSUnitValue : CSSNumericValue {
    val value: Number
    val unit: CSSUnit
    fun asString(): String = "${value}${unit.value}"
}

sealed interface CSSUnit {
    val value: String

    object percent: CSSUnit {
        override val value: String
            get() = "percent"
    }

    object em: CSSUnit {
        override val value: String
            get() = "em"
    }

    object ex: CSSUnit {
        override val value: String
            get() = "ex"
    }

    object ch: CSSUnit {
        override val value: String
            get() = "ch"
    }

    object ic: CSSUnit {
        override val value: String
            get() = "ic"
    }

    object rem: CSSUnit {
        override val value: String
            get() = "rem"
    }

    object lh: CSSUnit {
        override val value: String
            get() = "lh"
    }

    object rlh: CSSUnit {
        override val value: String
            get() = "rlh"
    }

    object vw: CSSUnit {
        override val value: String
            get() = "vw"
    }

    object vh: CSSUnit {
        override val value: String
            get() = "vh"
    }

    object vi: CSSUnit {
        override val value: String
            get() = "vi"
    }

    object vb: CSSUnit {
        override val value: String
            get() = "vb"
    }

    object vmin: CSSUnit {
        override val value: String
            get() = "vmin"
    }

    object vmax: CSSUnit {
        override val value: String
            get() = "vmax"
    }

    object cm: CSSUnit {
        override val value: String
            get() = "cm"
    }

    object mm: CSSUnit {
        override val value: String
            get() = "mm"
    }

    object q: CSSUnit {
        override val value: String
            get() = "q"
    }

    object pt: CSSUnit {
        override val value: String
            get() = "pt"
    }

    object pc: CSSUnit {
        override val value: String
            get() = "pc"
    }

    object px: CSSUnit {
        override val value: String
            get() = "px"
    }

    object deg: CSSUnit {
        override val value: String
            get() = "deg"
    }

    object grad: CSSUnit {
        override val value: String
            get() = "grad"
    }

    object rad: CSSUnit {
        override val value: String
            get() = "rad"
    }

    object turn: CSSUnit {
        override val value: String
            get() = "turn"
    }

    object s: CSSUnit {
        override val value: String
            get() = "s"
    }

    object ms: CSSUnit {
        override val value: String
            get() = "ms"
    }

    object hz: CSSUnit {
        override val value: String
            get() = "hz"
    }

    object khz: CSSUnit {
        override val value: String
            get() = "khz"
    }

    object dpi: CSSUnit {
        override val value: String
            get() = "dpi"
    }

    object dpcm: CSSUnit {
        override val value: String
            get() = "dpcm"
    }

    object dppx: CSSUnit {
        override val value: String
            get() = "dppx"
    }

    object fr: CSSUnit {
        override val value: String
            get() = "fr"
    }

    object number: CSSUnit {
        override val value: String
            get() = "number"
    }
}

interface CSSRelValue
interface CSSAbsValue
interface CSSAngleValue
interface CSSTimeValue
interface CSSFrequencyValue
interface CSSResolutionValue
interface CSSFlexValue

value class CSSpercentValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.percent
}

value class CSSemValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.em
}

value class CSSexValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.ex
}

value class CSSchValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.ch
}

value class CSSicValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.ic
}

value class CSSremValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.rem
}

value class CSSlhValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.lh
}

value class CSSrlhValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.rlh
}

value class CSSvwValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.vw
}

value class CSSvhValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.vh
}

value class CSSviValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.vi
}

value class CSSvbValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.vb
}

value class CSSvminValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.vmin
}

value class CSSvmaxValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.vmax
}

value class CSScmValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.cm
}

value class CSSmmValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.mm
}

value class CSSQValue(override val value: Number) : CSSUnitValue, CSSRelValue {
    override val unit: CSSUnit
        get() = CSSUnit.q
}

value class CSSptValue(override val value: Number) : CSSUnitValue, CSSAbsValue {
    override val unit: CSSUnit
        get() = CSSUnit.pt
}

value class CSSpcValue(override val value: Number) : CSSUnitValue, CSSAbsValue {
    override val unit: CSSUnit
        get() = CSSUnit.pc
}

value class CSSpxValue(override val value: Number) : CSSUnitValue, CSSAbsValue {
    override val unit: CSSUnit
        get() = CSSUnit.px
}

value class CSSdegValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: CSSUnit
        get() = CSSUnit.deg
}

value class CSSgradValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: CSSUnit
        get() = CSSUnit.grad
}

value class CSSradValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: CSSUnit
        get() = CSSUnit.rad
}

value class CSSturnValue(override val value: Number) : CSSUnitValue, CSSAngleValue {
    override val unit: CSSUnit
        get() = CSSUnit.turn
}

value class CSSsValue(override val value: Number) : CSSUnitValue, CSSTimeValue {
    override val unit: CSSUnit
        get() = CSSUnit.s
}

value class CSSmsValue(override val value: Number) : CSSUnitValue, CSSTimeValue {
    override val unit: CSSUnit
        get() = CSSUnit.ms
}

value class CSSHzValue(override val value: Number) : CSSUnitValue, CSSFrequencyValue {
    override val unit: CSSUnit
        get() = CSSUnit.hz
}

value class CSSkHzValue(override val value: Number) : CSSUnitValue, CSSFrequencyValue {
    override val unit: CSSUnit
        get() = CSSUnit.khz
}

value class CSSdpiValue(override val value: Number) : CSSUnitValue, CSSResolutionValue {
    override val unit: CSSUnit
        get() = CSSUnit.dpi
}

value class CSSdpcmValue(override val value: Number) : CSSUnitValue, CSSResolutionValue {
    override val unit: CSSUnit
        get() = CSSUnit.dpcm
}

value class CSSdppxValue(override val value: Number) : CSSUnitValue, CSSResolutionValue {
    override val unit: CSSUnit
        get() = CSSUnit.dppx
}

value class CSSfrValue(override val value: Number) : CSSUnitValue, CSSFlexValue {
    override val unit: CSSUnit
        get() = CSSUnit.fr
}

value class CSSnumberValue(override val value: Number) : CSSUnitValue, CSSFlexValue {
    override val unit: CSSUnit
        get() = CSSUnit.number
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
