/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED")
package org.jetbrains.compose.web.css

abstract class CSSNumericValueKT : CSSNumericValue

private class CSSUnitValueKT(
    override val value: Number,
    override val unit: String
) : CSSNumericValueKT(), 
    CSSUnitValue, 
    CSSnumberValue,
    CSSRelValue,
    CSSpercentValue,
    CSSemValue,
    CSSexValue,
    CSSchValue,
    CSSicValue,
    CSSremValue,
    CSSlhValue,
    CSSrlhValue,
    CSSvwValue,
    CSSvhValue,
    CSSviValue,
    CSSvbValue,
    CSSvminValue,
    CSSvmaxValue,
    CSScmValue,
    CSSmmValue,
    CSSQValue,
    CSSAbsValue,
    CSSptValue,
    CSSpcValue,
    CSSpxValue,
    CSSangleValue,
    CSSdegValue,
    CSSgradValue,
    CSSradValue,
    CSSturnValue,
    CSSTimeValue,
    CSSsValue,
    CSSmsValue,
    CSSFrequencyValue,
    CSSHzValue,
    CSSkHzValue,
    CSSResolutionValue,
    CSSdpiValue,
    CSSdpcmValue,
    CSSdppxValue,
    CSSFlexValue,
    CSSfrValue
{
    override fun toString(): String = "$value$unit"
}

object CSSKT: CSSTypedOM {
    override fun number(value: Number): CSSnumberValue = CSSUnitValueKT(value, "number")
    override fun percent(value: Number): CSSpercentValue = CSSUnitValueKT(value,  "percent")

    // <length>
    override fun em(value: Number): CSSemValue = CSSUnitValueKT(value,  "em")
    override fun ex(value: Number): CSSexValue = CSSUnitValueKT(value,  "ex")
    override fun ch(value: Number): CSSchValue = CSSUnitValueKT(value,  "ch")
    override fun ic(value: Number): CSSicValue = CSSUnitValueKT(value,  "ic")
    override fun rem(value: Number): CSSremValue = CSSUnitValueKT(value,  "rem")
    override fun lh(value: Number): CSSlhValue = CSSUnitValueKT(value,  "lh")
    override fun rlh(value: Number): CSSrlhValue = CSSUnitValueKT(value,  "rlh")
    override fun vw(value: Number): CSSvwValue = CSSUnitValueKT(value,  "vw")
    override fun vh(value: Number): CSSvhValue = CSSUnitValueKT(value,  "vh")
    override fun vi(value: Number): CSSviValue = CSSUnitValueKT(value,  "vi")
    override fun vb(value: Number): CSSvbValue = CSSUnitValueKT(value,  "vb")
    override fun vmin(value: Number): CSSvminValue = CSSUnitValueKT(value,  "vmin")
    override fun vmax(value: Number): CSSvmaxValue = CSSUnitValueKT(value,  "vmax")
    override fun cm(value: Number): CSScmValue = CSSUnitValueKT(value,  "cm")
    override fun mm(value: Number): CSSmmValue = CSSUnitValueKT(value,  "mm")
    override fun Q(value: Number): CSSQValue = CSSUnitValueKT(value,  "Qer")

    override fun pt(value: Number): CSSptValue = CSSUnitValueKT(value,  "pt")
    override fun pc(value: Number): CSSpcValue = CSSUnitValueKT(value,  "pc")
    override fun px(value: Number): CSSpxValue = CSSUnitValueKT(value,  "px")

    // <angle>
    override fun deg(value: Number): CSSdegValue = CSSUnitValueKT(value,  "deg")
    override fun grad(value: Number): CSSgradValue = CSSUnitValueKT(value,  "grad")
    override fun rad(value: Number): CSSradValue = CSSUnitValueKT(value,  "rad")
    override fun turn(value: Number): CSSturnValue = CSSUnitValueKT(value,  "turn")

    // <time>
    override fun s(value: Number): CSSsValue = CSSUnitValueKT(value,  "s")
    override fun ms(value: Number): CSSmsValue = CSSUnitValueKT(value,  "ms")

    // <frequency> {}
    override fun Hz(value: Number): CSSHzValue = CSSUnitValueKT(value,  "Hz")
    override fun kHz(value: Number): CSSkHzValue = CSSUnitValueKT(value,  "kHz")

    // <resolution>
    override fun dpi(value: Number): CSSdpiValue = CSSUnitValueKT(value,  "dpi")
    override fun dpcm(value: Number): CSSdpcmValue = CSSUnitValueKT(value,  "dpcm")
    override fun dppx(value: Number): CSSdppxValue = CSSUnitValueKT(value,  "dppx")

    // <flex>
    override fun fr(value: Number): CSSfrValue = CSSUnitValueKT(value,  "fr")
}