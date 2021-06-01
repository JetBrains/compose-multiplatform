@file:Suppress("UNUSED")

package org.jetbrains.compose.web.css

// fake interfaces to distinguish units
expect interface CSSSizeValue : CSSUnitValue

expect interface CSSnumberValue : CSSUnitValue

expect interface CSSRelValue : CSSSizeValue
expect interface CSSpercentValue : CSSRelValue
expect interface CSSemValue : CSSRelValue
expect interface CSSexValue : CSSRelValue
expect interface CSSchValue : CSSRelValue
expect interface CSSicValue : CSSRelValue
expect interface CSSremValue : CSSRelValue
expect interface CSSlhValue : CSSRelValue
expect interface CSSrlhValue : CSSRelValue
expect interface CSSvwValue : CSSRelValue
expect interface CSSvhValue : CSSRelValue
expect interface CSSviValue : CSSRelValue
expect interface CSSvbValue : CSSRelValue
expect interface CSSvminValue : CSSRelValue
expect interface CSSvmaxValue : CSSRelValue
expect interface CSScmValue : CSSRelValue
expect interface CSSmmValue : CSSRelValue
expect interface CSSQValue : CSSRelValue

expect interface CSSAbsValue : CSSSizeValue
expect interface CSSptValue : CSSAbsValue
expect interface CSSpcValue : CSSAbsValue
expect interface CSSpxValue : CSSAbsValue

expect interface CSSangleValue : CSSUnitValue
expect interface CSSdegValue : CSSangleValue
expect interface CSSgradValue : CSSangleValue
expect interface CSSradValue : CSSangleValue
expect interface CSSturnValue : CSSangleValue

expect interface CSSTimeValue : CSSUnitValue
expect interface CSSsValue : CSSTimeValue
expect interface CSSmsValue : CSSTimeValue

expect interface CSSFrequencyValue : CSSUnitValue
expect interface CSSHzValue : CSSFrequencyValue
expect interface CSSkHzValue : CSSFrequencyValue

expect interface CSSResolutionValue : CSSUnitValue
expect interface CSSdpiValue : CSSResolutionValue
expect interface CSSdpcmValue : CSSResolutionValue
expect interface CSSdppxValue : CSSResolutionValue

expect interface CSSFlexValue : CSSUnitValue
expect interface CSSfrValue : CSSFlexValue

inline val Number.number
    get(): CSSUnitValue = CSS.number(this)

inline val Number.percent
    get(): CSSpercentValue = CSS.percent(this)

inline val Number.em
    get(): CSSemValue = CSS.em(this)
inline val Number.ex
    get(): CSSexValue = CSS.ex(this)
inline val Number.ch
    get(): CSSchValue = CSS.ch(this)
inline val Number.ic
    get(): CSSicValue = CSS.ic(this)
inline val Number.rem
    get(): CSSremValue = CSS.rem(this)
inline val Number.lh
    get(): CSSlhValue = CSS.lh(this)
inline val Number.rlh
    get(): CSSrlhValue = CSS.rlh(this)
inline val Number.vw
    get(): CSSvwValue = CSS.vw(this)
inline val Number.vh
    get(): CSSvhValue = CSS.vh(this)
inline val Number.vi
    get(): CSSviValue = CSS.vi(this)
inline val Number.vb
    get(): CSSvbValue = CSS.vb(this)
inline val Number.vmin
    get(): CSSvminValue = CSS.vmin(this)
inline val Number.vmax
    get(): CSSvmaxValue = CSS.vmax(this)
inline val Number.cm
    get(): CSScmValue = CSS.cm(this)
inline val Number.mm
    get(): CSSmmValue = CSS.mm(this)
inline val Number.Q
    get(): CSSQValue = CSS.Q(this)

inline val Number.pt
    get(): CSSptValue = CSS.pt(this)
inline val Number.pc
    get(): CSSpcValue = CSS.pc(this)
inline val Number.px
    get(): CSSpxValue = CSS.px(this)

inline val Number.deg
    get(): CSSdegValue = CSS.deg(this)
inline val Number.grad
    get(): CSSgradValue = CSS.grad(this)
inline val Number.rad
    get(): CSSradValue = CSS.rad(this)
inline val Number.turn
    get(): CSSturnValue = CSS.turn(this)

inline val Number.s
    get(): CSSsValue = CSS.s(this)
inline val Number.ms
    get(): CSSmsValue = CSS.ms(this)

inline val Number.Hz
    get(): CSSHzValue = CSS.Hz(this)
inline val Number.kHz
    get(): CSSkHzValue = CSS.kHz(this)

inline val Number.dpi
    get(): CSSdpiValue = CSS.dpi(this)
inline val Number.dpcm
    get(): CSSdpcmValue = CSS.dpcm(this)
inline val Number.dppx
    get(): CSSdppxValue = CSS.dppx(this)

inline val Number.fr
    get(): CSSfrValue = CSS.fr(this)