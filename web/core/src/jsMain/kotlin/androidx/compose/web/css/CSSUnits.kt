@file:Suppress("UNUSED")

package org.jetbrains.compose.web.css

external interface CSSSizeValue : CSSUnitValue

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
    get(): CSSpercentValue = CSS.percent(this).unsafeCast<CSSpercentValue>()

val Number.em
    get(): CSSemValue = CSS.em(this).unsafeCast<CSSemValue>()
val Number.ex
    get(): CSSexValue = CSS.ex(this).unsafeCast<CSSexValue>()
val Number.ch
    get(): CSSchValue = CSS.ch(this).unsafeCast<CSSchValue>()
val Number.ic
    get(): CSSicValue = CSS.ic(this).unsafeCast<CSSicValue>()
val Number.rem
    get(): CSSremValue = CSS.rem(this).unsafeCast<CSSremValue>()
val Number.lh
    get(): CSSlhValue = CSS.lh(this).unsafeCast<CSSlhValue>()
val Number.rlh
    get(): CSSrlhValue = CSS.rlh(this).unsafeCast<CSSrlhValue>()
val Number.vw
    get(): CSSvwValue = CSS.vw(this).unsafeCast<CSSvwValue>()
val Number.vh
    get(): CSSvhValue = CSS.vh(this).unsafeCast<CSSvhValue>()
val Number.vi
    get(): CSSviValue = CSS.vi(this).unsafeCast<CSSviValue>()
val Number.vb
    get(): CSSvbValue = CSS.vb(this).unsafeCast<CSSvbValue>()
val Number.vmin
    get(): CSSvminValue = CSS.vmin(this).unsafeCast<CSSvminValue>()
val Number.vmax
    get(): CSSvmaxValue = CSS.vmax(this).unsafeCast<CSSvmaxValue>()
val Number.cm
    get(): CSScmValue = CSS.cm(this).unsafeCast<CSScmValue>()
val Number.mm
    get(): CSSmmValue = CSS.mm(this).unsafeCast<CSSmmValue>()
val Number.Q
    get(): CSSQValue = CSS.Q(this).unsafeCast<CSSQValue>()

val Number.pt
    get(): CSSptValue = CSS.pt(this).unsafeCast<CSSptValue>()
val Number.pc
    get(): CSSpcValue = CSS.pc(this).unsafeCast<CSSpcValue>()
val Number.px
    get(): CSSpxValue = CSS.px(this).unsafeCast<CSSpxValue>()

val Number.deg
    get(): CSSdegValue = CSS.deg(this).unsafeCast<CSSdegValue>()
val Number.grad
    get(): CSSgradValue = CSS.grad(this).unsafeCast<CSSgradValue>()
val Number.rad
    get(): CSSradValue = CSS.rad(this).unsafeCast<CSSradValue>()
val Number.turn
    get(): CSSturnValue = CSS.turn(this).unsafeCast<CSSturnValue>()

val Number.s
    get(): CSSsValue = CSS.s(this).unsafeCast<CSSsValue>()
val Number.ms
    get(): CSSmsValue = CSS.ms(this).unsafeCast<CSSmsValue>()

val Number.Hz
    get(): CSSHzValue = CSS.Hz(this).unsafeCast<CSSHzValue>()
val Number.kHz
    get(): CSSkHzValue = CSS.kHz(this).unsafeCast<CSSkHzValue>()

val Number.dpi
    get(): CSSdpiValue = CSS.dpi(this).unsafeCast<CSSdpiValue>()
val Number.dpcm
    get(): CSSdpcmValue = CSS.dpcm(this).unsafeCast<CSSdpcmValue>()
val Number.dppx
    get(): CSSdppxValue = CSS.dppx(this).unsafeCast<CSSdppxValue>()

val Number.fr
    get(): CSSfrValue = CSS.fr(this).unsafeCast<CSSfrValue>()
