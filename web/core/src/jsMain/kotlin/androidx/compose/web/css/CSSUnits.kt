package org.jetbrains.compose.web.css
external interface CSSSizeValue : CSSUnitValue, CSSSizeOrAutoValue

// fake interfaces to distinguish units
external interface CSSRelValue : CSSSizeValue
external interface CSSpercentValue : CSSRelValue
external interface CSSemValue : CSSRelValue
external interface CSSexValue : CSSRelValue
external interface CSSchValue : CSSRelValue
external interface CSSicValue : CSSRelValue
external interface CSSremValue : CSSRelValue
external interface CSSlhValue : CSSRelValue
external interface CSSrlhValue : CSSRelValue
external interface CSSvwValue : CSSRelValue
external interface CSSvhValue : CSSRelValue
external interface CSSviValue : CSSRelValue
external interface CSSvbValue : CSSRelValue
external interface CSSvminValue : CSSRelValue
external interface CSSvmaxValue : CSSRelValue
external interface CSScmValue : CSSRelValue
external interface CSSmmValue : CSSRelValue
external interface CSSQValue : CSSRelValue

external interface CSSAbsValue : CSSSizeValue
external interface CSSptValue : CSSAbsValue
external interface CSSpcValue : CSSAbsValue
external interface CSSpxValue : CSSAbsValue

external interface CSSangleValue : CSSUnitValue
external interface CSSdegValue : CSSangleValue
external interface CSSgradValue : CSSangleValue
external interface CSSradValue : CSSangleValue
external interface CSSturnValue : CSSangleValue

external interface CSSTimeValue : CSSUnitValue
external interface CSSsValue : CSSTimeValue
external interface CSSmsValue : CSSTimeValue

external interface CSSFrequencyValue : CSSUnitValue
external interface CSSHzValue : CSSFrequencyValue
external interface CSSkHzValue : CSSFrequencyValue

external interface CSSResolutionValue : CSSUnitValue
external interface CSSdpiValue : CSSResolutionValue
external interface CSSdpcmValue : CSSResolutionValue
external interface CSSdppxValue : CSSResolutionValue

external interface CSSFlexValue : CSSUnitValue
external interface CSSfrValue : CSSFlexValue

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
