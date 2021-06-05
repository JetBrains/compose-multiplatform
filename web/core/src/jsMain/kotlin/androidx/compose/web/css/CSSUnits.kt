package org.jetbrains.compose.web.css

interface CSSRelValue
interface CSSAbsValue
interface CSSAngleValue
interface CSSTimeValue
interface CSSFrequencyValue
interface CSSResolutionValue
interface CSSFlexValue

class CSSpercentValue(value: Number) : CSSUnitValue(value, "percent"), CSSRelValue
class CSSemValue(value: Number) : CSSUnitValue(value, "em"), CSSRelValue
class CSSexValue(value: Number) : CSSUnitValue(value, "ex"), CSSRelValue
class CSSchValue(value: Number) : CSSUnitValue(value, "ch"), CSSRelValue
class CSSicValue(value: Number) : CSSUnitValue(value, "ic"), CSSRelValue
class CSSremValue(value: Number) : CSSUnitValue(value, "rem"), CSSRelValue
class CSSlhValue(value: Number) : CSSUnitValue(value, "lh"), CSSRelValue
class CSSrlhValue(value: Number) : CSSUnitValue(value, "rlh"), CSSRelValue
class CSSvwValue(value: Number) : CSSUnitValue(value, "vw"), CSSRelValue
class CSSvhValue(value: Number) : CSSUnitValue(value, "vh"), CSSRelValue
class CSSviValue(value: Number) : CSSUnitValue(value, "vi"), CSSRelValue
class CSSvbValue(value: Number) : CSSUnitValue(value, "vb"), CSSRelValue
class CSSvminValue(value: Number) : CSSUnitValue(value, "vmin"), CSSRelValue
class CSSvmaxValue(value: Number) : CSSUnitValue(value, "vmax"), CSSRelValue
class CSScmValue(value: Number) : CSSUnitValue(value, "cm"), CSSRelValue
class CSSmmValue(value: Number) : CSSUnitValue(value, "mm"), CSSRelValue
class CSSQValue(value: Number) : CSSUnitValue(value, "q"), CSSRelValue

class CSSptValue(value: Number) : CSSUnitValue(value, "pt"), CSSAbsValue
class CSSpcValue(value: Number) : CSSUnitValue(value, "pc"), CSSAbsValue
class CSSpxValue(value: Number) : CSSUnitValue(value, "px"), CSSAbsValue

class CSSdegValue(value: Number) : CSSUnitValue(value, "deg"), CSSAngleValue
class CSSgradValue(value: Number) : CSSUnitValue(value, "grad"), CSSAngleValue
class CSSradValue(value: Number) : CSSUnitValue(value, "rad"), CSSAngleValue
class CSSturnValue(value: Number) : CSSUnitValue(value, "turn"), CSSAngleValue

class CSSsValue(value: Number) : CSSUnitValue(value, "s"), CSSTimeValue
class CSSmsValue(value: Number) : CSSUnitValue(value, "ms"), CSSTimeValue

class CSSHzValue(value: Number) : CSSUnitValue(value, "hz"), CSSFrequencyValue
class CSSkHzValue(value: Number) : CSSUnitValue(value, "khz"), CSSFrequencyValue

class CSSdpiValue(value: Number) : CSSUnitValue(value, "dpi"), CSSResolutionValue
class CSSdpcmValue(value: Number) : CSSUnitValue(value, "dpcm"), CSSResolutionValue
class CSSdppxValue(value: Number) : CSSUnitValue(value, "dppx"), CSSResolutionValue

class CSSfrValue(value: Number) : CSSUnitValue(value, "fr"), CSSFlexValue

val Number.number
    get(): CSSUnitValue = CSS.number(this)

val Number.percent
    get(): CSSpercentValue = CSS.percent(this)

val Number.em
    get(): CSSemValue = CSS.em(this)
val Number.ex
    get(): CSSexValue = CSS.ex(this)
val Number.ch
    get(): CSSchValue = CSS.ch(this)
val Number.cssRem
    get(): CSSremValue = CSS.rem(this)
val Number.vw
    get(): CSSvwValue = CSS.vw(this)
val Number.vh
    get(): CSSvhValue = CSS.vh(this)
val Number.vmin
    get(): CSSvminValue = CSS.vmin(this)
val Number.vmax
    get(): CSSvmaxValue = CSS.vmax(this)
val Number.cm
    get(): CSScmValue = CSS.cm(this)
val Number.mm
    get(): CSSmmValue = CSS.mm(this)
val Number.Q
    get(): CSSQValue = CSS.Q(this)

val Number.pt
    get(): CSSptValue = CSS.pt(this)
val Number.pc
    get(): CSSpcValue = CSS.pc(this)
val Number.px
    get(): CSSpxValue = CSS.px(this)

val Number.deg
    get(): CSSdegValue = CSS.deg(this)
val Number.grad
    get(): CSSgradValue = CSS.grad(this)
val Number.rad
    get(): CSSradValue = CSS.rad(this)
val Number.turn
    get(): CSSturnValue = CSS.turn(this)

val Number.s
    get(): CSSsValue = CSS.s(this)
val Number.ms
    get(): CSSmsValue = CSS.ms(this)

val Number.Hz
    get(): CSSHzValue = CSS.Hz(this)
val Number.kHz
    get(): CSSkHzValue = CSS.kHz(this)

val Number.dpi
    get(): CSSdpiValue = CSS.dpi(this)
val Number.dpcm
    get(): CSSdpcmValue = CSS.dpcm(this)
val Number.dppx
    get(): CSSdppxValue = CSS.dppx(this)

val Number.fr
    get(): CSSfrValue = CSS.fr(this)
