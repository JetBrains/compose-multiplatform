@file:Suppress("UNUSED")

package org.jetbrains.compose.web.css

// fake interfaces to distinguish units
actual external interface CSSSizeValue : CSSUnitValue

actual external interface CSSnumberValue : CSSUnitValue

actual external interface CSSRelValue : CSSSizeValue
actual external interface CSSpercentValue : CSSRelValue
actual external interface CSSemValue : CSSRelValue
actual external interface CSSexValue : CSSRelValue
actual external interface CSSchValue : CSSRelValue
actual external interface CSSicValue : CSSRelValue
actual external interface CSSremValue : CSSRelValue
actual external interface CSSlhValue : CSSRelValue
actual external interface CSSrlhValue : CSSRelValue
actual external interface CSSvwValue : CSSRelValue
actual external interface CSSvhValue : CSSRelValue
actual external interface CSSviValue : CSSRelValue
actual external interface CSSvbValue : CSSRelValue
actual external interface CSSvminValue : CSSRelValue
actual external interface CSSvmaxValue : CSSRelValue
actual external interface CSScmValue : CSSRelValue
actual external interface CSSmmValue : CSSRelValue
actual external interface CSSQValue : CSSRelValue

actual external interface CSSAbsValue : CSSSizeValue
actual external interface CSSptValue : CSSAbsValue
actual external interface CSSpcValue : CSSAbsValue
actual external interface CSSpxValue : CSSAbsValue

actual external interface CSSangleValue : CSSUnitValue
actual external interface CSSdegValue : CSSangleValue
actual external interface CSSgradValue : CSSangleValue
actual external interface CSSradValue : CSSangleValue
actual external interface CSSturnValue : CSSangleValue

actual external interface CSSTimeValue : CSSUnitValue
actual external interface CSSsValue : CSSTimeValue
actual external interface CSSmsValue : CSSTimeValue

actual external interface CSSFrequencyValue : CSSUnitValue
actual external interface CSSHzValue : CSSFrequencyValue
actual external interface CSSkHzValue : CSSFrequencyValue

actual external interface CSSResolutionValue : CSSUnitValue
actual external interface CSSdpiValue : CSSResolutionValue
actual external interface CSSdpcmValue : CSSResolutionValue
actual external interface CSSdppxValue : CSSResolutionValue

actual external interface CSSFlexValue : CSSUnitValue
actual external interface CSSfrValue : CSSFlexValue