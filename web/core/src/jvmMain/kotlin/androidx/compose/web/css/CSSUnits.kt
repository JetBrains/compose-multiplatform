/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.css

// fake interfaces to distinguish units
actual interface CSSSizeValue : CSSUnitValue

actual interface CSSnumberValue : CSSUnitValue

actual interface CSSRelValue : CSSSizeValue
actual interface CSSpercentValue : CSSRelValue
actual interface CSSQValue : CSSRelValue
actual interface CSSemValue : CSSRelValue
actual interface CSSexValue : CSSRelValue
actual interface CSSchValue : CSSRelValue
actual interface CSSicValue : CSSRelValue
actual interface CSSremValue : CSSRelValue
actual interface CSSlhValue : CSSRelValue
actual interface CSSrlhValue : CSSRelValue
actual interface CSSvwValue : CSSRelValue
actual interface CSSvhValue : CSSRelValue
actual interface CSSviValue : CSSRelValue
actual interface CSSvbValue : CSSRelValue
actual interface CSSvminValue : CSSRelValue
actual interface CSSvmaxValue : CSSRelValue
actual interface CSScmValue : CSSRelValue
actual interface CSSmmValue : CSSRelValue
actual interface CSSAbsValue : CSSSizeValue
actual interface CSSptValue : CSSAbsValue
actual interface CSSpcValue : CSSAbsValue
actual interface CSSpxValue : CSSAbsValue
actual interface CSSangleValue : CSSUnitValue
actual interface CSSdegValue : CSSangleValue
actual interface CSSgradValue : CSSangleValue
actual interface CSSradValue : CSSangleValue
actual interface CSSturnValue : CSSangleValue
actual interface CSSTimeValue : CSSUnitValue
actual interface CSSsValue : CSSTimeValue
actual interface CSSmsValue : CSSTimeValue
actual interface CSSFrequencyValue : CSSUnitValue
actual interface CSSHzValue : CSSFrequencyValue
actual interface CSSkHzValue : CSSFrequencyValue
actual interface CSSResolutionValue : CSSUnitValue
actual interface CSSdpiValue : CSSResolutionValue
actual interface CSSdpcmValue : CSSResolutionValue
actual interface CSSdppxValue : CSSResolutionValue
actual interface CSSFlexValue : CSSUnitValue
actual interface CSSfrValue : CSSFlexValue