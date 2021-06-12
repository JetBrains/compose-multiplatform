package org.jetbrains.compose.web.css

interface CSSSizeValue : CSSUnitValue, StylePropertyValue

interface CSSRelValue : CSSSizeValue
interface CSSpercentValue : CSSRelValue
interface CSSemValue : CSSRelValue
interface CSSexValue : CSSRelValue
interface CSSchValue : CSSRelValue
interface CSSicValue : CSSRelValue
interface CSSremValue : CSSRelValue
interface CSSlhValue : CSSRelValue
interface CSSrlhValue : CSSRelValue
interface CSSvwValue : CSSRelValue
interface CSSvhValue : CSSRelValue
interface CSSviValue : CSSRelValue
interface CSSvbValue : CSSRelValue
interface CSSvminValue : CSSRelValue
interface CSSvmaxValue : CSSRelValue
interface CSScmValue : CSSRelValue
interface CSSmmValue : CSSRelValue
interface CSSQValue : CSSRelValue

interface CSSAbsValue : CSSSizeValue
interface CSSptValue : CSSAbsValue
interface CSSpcValue : CSSAbsValue
interface CSSpxValue : CSSAbsValue

interface CSSangleValue : CSSUnitValue
interface CSSdegValue : CSSangleValue
interface CSSgradValue : CSSangleValue
interface CSSradValue : CSSangleValue
interface CSSturnValue : CSSangleValue

interface CSSTimeValue : CSSUnitValue
interface CSSsValue : CSSTimeValue
interface CSSmsValue : CSSTimeValue

interface CSSFrequencyValue : CSSUnitValue
interface CSSHzValue : CSSFrequencyValue
interface CSSkHzValue : CSSFrequencyValue

interface CSSResolutionValue : CSSUnitValue
interface CSSdpiValue : CSSResolutionValue
interface CSSdpcmValue : CSSResolutionValue
interface CSSdppxValue : CSSResolutionValue

interface CSSFlexValue : CSSUnitValue
interface CSSfrValue : CSSFlexValue

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
