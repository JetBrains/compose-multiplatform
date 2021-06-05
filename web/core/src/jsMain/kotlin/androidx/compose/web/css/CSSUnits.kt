package org.jetbrains.compose.web.css

interface CSSRelValue
interface CSSAbsValue
interface CSSAngleValue
interface CSSTimeValue
interface CSSFrequencyValue
interface CSSResolutionValue
interface CSSFlexValue

interface CSSpercentValue : CSSUnitValue, CSSRelValue
interface CSSemValue : CSSUnitValue, CSSRelValue
interface CSSexValue : CSSUnitValue, CSSRelValue
interface CSSchValue : CSSUnitValue, CSSRelValue
interface CSSicValue : CSSUnitValue, CSSRelValue
interface CSSremValue : CSSUnitValue, CSSRelValue
interface CSSlhValue : CSSUnitValue, CSSRelValue
interface CSSrlhValue : CSSUnitValue, CSSRelValue
interface CSSvwValue : CSSUnitValue, CSSRelValue
interface CSSvhValue : CSSUnitValue, CSSRelValue
interface CSSviValue : CSSUnitValue, CSSRelValue
interface CSSvbValue : CSSUnitValue, CSSRelValue
interface CSSvminValue : CSSUnitValue, CSSRelValue
interface CSSvmaxValue : CSSUnitValue, CSSRelValue
interface CSScmValue : CSSUnitValue, CSSRelValue
interface CSSmmValue : CSSUnitValue, CSSRelValue
interface CSSQValue : CSSUnitValue, CSSRelValue

interface CSSptValue : CSSUnitValue, CSSAbsValue
interface CSSpcValue : CSSUnitValue, CSSAbsValue
interface CSSpxValue : CSSUnitValue, CSSAbsValue

interface CSSdegValue : CSSUnitValue, CSSAngleValue
interface CSSgradValue : CSSUnitValue, CSSAngleValue
interface CSSradValue : CSSUnitValue, CSSAngleValue
interface CSSturnValue : CSSUnitValue, CSSAngleValue

interface CSSsValue : CSSUnitValue, CSSTimeValue
interface CSSmsValue : CSSUnitValue, CSSTimeValue

interface CSSHzValue : CSSUnitValue, CSSFrequencyValue
interface CSSkHzValue : CSSUnitValue, CSSFrequencyValue

interface CSSdpiValue : CSSUnitValue, CSSResolutionValue
interface CSSdpcmValue : CSSUnitValue, CSSResolutionValue
interface CSSdppxValue : CSSUnitValue, CSSResolutionValue

interface CSSfrValue : CSSUnitValue, CSSFlexValue

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
