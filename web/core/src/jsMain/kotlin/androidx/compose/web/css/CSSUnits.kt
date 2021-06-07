package org.jetbrains.compose.web.css

interface CSSUnitValueTyped<T : CSSUnit> : CSSNumericValue {
    val value: Float
    val unit: T
    fun asString(): String = "${value}${unit.value}"
}

interface CSSUnitValue : CSSUnitValueTyped<CSSUnit>

interface CSSUnitRel : CSSUnit
interface CSSUnitAbs: CSSUnit
interface CSSUnitAngle: CSSUnit
interface CSSUnitTime: CSSUnit
interface CSSUnitFrequency: CSSUnit
interface CSSUnitResolution: CSSUnit
interface CSSUnitFlex: CSSUnit

interface CSSAngleValue : CSSUnitValueTyped<CSSUnitAngle>

sealed interface CSSUnit {
    val value: String

    object percent: CSSUnitRel {
        override val value: String
            get() = "percent"
    }

    object em: CSSUnitRel {
        override val value: String
            get() = "em"
    }

    object ex: CSSUnitRel {
        override val value: String
            get() = "ex"
    }

    object ch: CSSUnitRel {
        override val value: String
            get() = "ch"
    }

    object ic: CSSUnitRel {
        override val value: String
            get() = "ic"
    }

    object rem: CSSUnitRel {
        override val value: String
            get() = "rem"
    }

    object lh: CSSUnitRel {
        override val value: String
            get() = "lh"
    }

    object rlh: CSSUnitRel {
        override val value: String
            get() = "rlh"
    }

    object vw: CSSUnitRel {
        override val value: String
            get() = "vw"
    }

    object vh: CSSUnitRel {
        override val value: String
            get() = "vh"
    }

    object vi: CSSUnitRel {
        override val value: String
            get() = "vi"
    }

    object vb: CSSUnitRel {
        override val value: String
            get() = "vb"
    }

    object vmin: CSSUnitRel {
        override val value: String
            get() = "vmin"
    }

    object vmax: CSSUnitRel {
        override val value: String
            get() = "vmax"
    }

    object cm: CSSUnitRel {
        override val value: String
            get() = "cm"
    }

    object mm: CSSUnitRel {
        override val value: String
            get() = "mm"
    }

    object q: CSSUnitRel {
        override val value: String
            get() = "q"
    }

    object pt: CSSUnitAbs {
        override val value: String
            get() = "pt"
    }

    object pc: CSSUnitAbs {
        override val value: String
            get() = "pc"
    }

    object px: CSSUnitAbs {
        override val value: String
            get() = "px"
    }

    object deg: CSSUnitAngle {
        override val value: String
            get() = "deg"
    }

    object grad: CSSUnitAngle {
        override val value: String
            get() = "grad"
    }

    object rad: CSSUnitAngle {
        override val value: String
            get() = "rad"
    }

    object turn: CSSUnitAngle {
        override val value: String
            get() = "turn"
    }

    object s: CSSUnitTime {
        override val value: String
            get() = "s"
    }

    object ms: CSSUnitTime {
        override val value: String
            get() = "ms"
    }

    object hz: CSSUnitFrequency {
        override val value: String
            get() = "hz"
    }

    object khz: CSSUnitFrequency {
        override val value: String
            get() = "khz"
    }

    object dpi: CSSUnitResolution {
        override val value: String
            get() = "dpi"
    }

    object dpcm: CSSUnitResolution {
        override val value: String
            get() = "dpcm"
    }

    object dppx: CSSUnitResolution {
        override val value: String
            get() = "dppx"
    }

    object fr: CSSUnitFlex {
        override val value: String
            get() = "fr"
    }

    object number: CSSUnit {
        override val value: String
            get() = "number"
    }
}

value class CSSpercentValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.percent
}

value class CSSemValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.em
}

value class CSSexValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.ex
}

value class CSSchValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.ch
}

value class CSSicValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.ic
}

value class CSSremValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.rem
}

value class CSSlhValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.lh
}

value class CSSrlhValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.rlh
}

value class CSSvwValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.vw
}

value class CSSvhValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.vh
}

value class CSSviValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.vi
}

value class CSSvbValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.vb
}

value class CSSvminValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.vmin
}

value class CSSvmaxValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.vmax
}

value class CSScmValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.cm
}

value class CSSmmValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.mm
}

value class CSSQValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.q
}

value class CSSptValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.pt
}

value class CSSpcValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.pc
}

value class CSSpxValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.px
}

value class CSSdegValue(override val value: Float) : CSSAngleValue {
    override val unit: CSSUnitAngle
        get() = CSSUnit.deg
}

value class CSSgradValue(override val value: Float) : CSSAngleValue {
    override val unit: CSSUnitAngle
        get() = CSSUnit.grad
}

value class CSSradValue(override val value: Float) : CSSAngleValue {
    override val unit: CSSUnitAngle
        get() = CSSUnit.rad
}

value class CSSturnValue(override val value: Float) : CSSAngleValue {
    override val unit: CSSUnitAngle
        get() = CSSUnit.turn
}

value class CSSsValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.s
}

value class CSSmsValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.ms
}

value class CSSHzValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.hz
}

value class CSSkHzValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.khz
}

value class CSSdpiValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.dpi
}

value class CSSdpcmValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.dpcm
}

value class CSSdppxValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.dppx
}

value class CSSfrValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.fr
}

value class CSSnumberValue(override val value: Float) : CSSUnitValue {
    override val unit: CSSUnit
        get() = CSSUnit.number
}

val Number.number
    get(): CSSnumberValue = CSSnumberValue(this.toFloat())

val Number.percent
    get(): CSSpercentValue = CSSpercentValue(this.toFloat())

val Number.em
    get(): CSSemValue = CSSemValue(this.toFloat())

val Number.ex
    get(): CSSexValue = CSSexValue(this.toFloat())

val Number.ch
    get(): CSSchValue = CSSchValue(this.toFloat())

val Number.cssRem
    get(): CSSremValue = CSSremValue(this.toFloat())

val Number.vw
    get(): CSSvwValue = CSSvwValue(this.toFloat())

val Number.vh
    get(): CSSvhValue = CSSvhValue(this.toFloat())

val Number.vmin
    get(): CSSvminValue = CSSvminValue(this.toFloat())

val Number.vmax
    get(): CSSvmaxValue = CSSvmaxValue(this.toFloat())

val Number.cm
    get(): CSScmValue = CSScmValue(this.toFloat())

val Number.mm
    get(): CSSmmValue = CSSmmValue(this.toFloat())

val Number.Q
    get(): CSSQValue = CSSQValue(this.toFloat())

val Number.pt
    get(): CSSptValue = CSSptValue(this.toFloat())
val Number.pc
    get(): CSSpcValue = CSSpcValue(this.toFloat())
val Number.px
    get(): CSSpxValue = CSSpxValue(this.toFloat())

val Number.deg
    get(): CSSdegValue = CSSdegValue(this.toFloat())
val Number.grad
    get(): CSSgradValue = CSSgradValue(this.toFloat())
val Number.rad
    get(): CSSradValue = CSSradValue(this.toFloat())
val Number.turn
    get(): CSSturnValue = CSSturnValue(this.toFloat())

val Number.s
    get(): CSSsValue = CSSsValue(this.toFloat())
val Number.ms
    get(): CSSmsValue = CSSmsValue(this.toFloat())

val Number.Hz
    get(): CSSHzValue = CSSHzValue(this.toFloat())
val Number.kHz
    get(): CSSkHzValue = CSSkHzValue(this.toFloat())

val Number.dpi
    get(): CSSdpiValue = CSSdpiValue(this.toFloat())
val Number.dpcm
    get(): CSSdpcmValue = CSSdpcmValue(this.toFloat())
val Number.dppx
    get(): CSSdppxValue = CSSdppxValue(this.toFloat())

val Number.fr
    get(): CSSfrValue = CSSfrValue(this.toFloat())
