/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED", "NOTHING_TO_INLINE")
package org.jetbrains.compose.web.css

import org.w3c.dom.HTMLElement
import org.w3c.dom.css.*
import org.w3c.dom.css.StyleSheet

actual external interface CSSStyleValue

external interface CSSKeywordValueJS : CSSStyleValue {
    val value: String
}

@JsName("CSSKeywordValueActual")
actual interface CSSKeywordValue : CSSKeywordValueJS, CSSStyleValue {
    actual override val value: String

    actual companion object {
        actual inline operator fun invoke(value: String): CSSKeywordValue =
            CSSKeywordValueKT(value)
    }
}

actual interface StylePropertyValue {
    actual interface String: StylePropertyValue
    actual interface Number: StylePropertyValue
    actual interface StyleValue: StylePropertyValue, CSSStyleValue
    actual companion object {
       actual inline operator fun invoke(value: kotlin.String): String = value.unsafeCast<String>()

       actual inline operator fun invoke(value: kotlin.Number): Number = value.unsafeCast<Number>()

       actual inline operator fun invoke(value: CSSStyleValue): StyleValue = value.unsafeCast<StyleValue>()
    }
}

// type CSSNumeric = number | CSSNumericValue
actual interface CSSNumeric {
    actual interface Number : CSSNumeric
    actual interface NumericValue : CSSNumeric, CSSNumericValue
    actual companion object {
       actual inline operator fun invoke(value: kotlin.Number): Number = value.unsafeCast<Number>()

       actual inline operator fun invoke(value: CSSNumericValue): NumericValue = value.unsafeCast<NumericValue>()
    }
}

actual external interface CSSMathValue : CSSNumericValue {
    @JsName("operator")
    actual val strOperator: String
}

actual val CSSMathValue.operator: CSSMathOperator
    get() = CSSMathOperator.valueOf(this.strOperator)

actual external class CSSNumericArray {
    actual fun forEach(handler: (CSSNumericValue) -> Unit)

    actual val length: Int

    actual operator fun get(index: Int): CSSNumericValue
}

actual external interface CSSMathSum : CSSMathValue {
    actual val values: CSSNumericArray
}

actual interface CSSNumericType {
    actual val length: Number
    actual val angle: Number
    actual val time: Number
    actual val frequency: Number
    actual val resolution: Number
    actual val flex: Number
    actual val percent: Number
    @JsName("percentHint")
    actual val strPercentHint: String
}

actual val CSSNumericType.percentHint: CSSNumericBaseType
    get() = CSSNumericBaseType.valueOf(this.strPercentHint)

actual external interface CSSNumericValue : CSSStyleValue

actual external interface CSSUnitValue : CSSNumericValue {
    actual val value: Number
    actual val unit: String
}

actual external interface CSSTypedOM {
    actual fun number(value: Number): CSSnumberValue
    actual fun percent(value: Number): CSSpercentValue
    actual fun em(value: Number): CSSemValue
    actual fun ex(value: Number): CSSexValue
    actual fun ch(value: Number): CSSchValue
    actual fun ic(value: Number): CSSicValue
    actual fun rem(value: Number): CSSremValue
    actual fun lh(value: Number): CSSlhValue
    actual fun rlh(value: Number): CSSrlhValue
    actual fun vw(value: Number): CSSvwValue
    actual fun vh(value: Number): CSSvhValue
    actual fun vi(value: Number): CSSviValue
    actual fun vb(value: Number): CSSvbValue
    actual fun vmin(value: Number): CSSvminValue
    actual fun vmax(value: Number): CSSvmaxValue
    actual fun cm(value: Number): CSScmValue
    actual fun mm(value: Number): CSSmmValue
    actual fun Q(value: Number): CSSQValue
    actual fun pt(value: Number): CSSptValue
    actual fun pc(value: Number): CSSpcValue
    actual fun px(value: Number): CSSpxValue
    actual fun deg(value: Number): CSSdegValue
    actual fun grad(value: Number): CSSgradValue
    actual fun rad(value: Number): CSSradValue
    actual fun turn(value: Number): CSSturnValue
    actual fun s(value: Number): CSSsValue
    actual fun ms(value: Number): CSSmsValue
    actual fun Hz(value: Number): CSSHzValue
    actual fun kHz(value: Number): CSSkHzValue
    actual fun dpi(value: Number): CSSdpiValue
    actual fun dpcm(value: Number): CSSdpcmValue
    actual fun dppx(value: Number): CSSdppxValue
    actual fun fr(value: Number): CSSfrValue
}


class StylePropertyMapKT(private val style: CSSStyleDeclaration, private val clearFn: () -> Unit): StylePropertyMap {
    override fun set(property: String, value: StylePropertyValue) {
        style.setProperty(property, value.toString())
    }

    override fun append(property: String, value: StylePropertyValue) {
        val oldValues = style.getPropertyValue(property)
        if (oldValues == undefined) {
            style.setProperty(property, value.toString())
        } else {
            val newValues = listOf(oldValues, value)
            style.setProperty(property, newValues.joinToString(", "))
        }
    }

    override fun delete(property: String) {
        style.removeProperty(property)
    }

    override fun clear() {
        clearFn()
    }

    override fun has(property: String): Boolean {
        return style.getPropertyValue(property) as String? != null
    }
}

fun Any.polyfillStylePropertyMap(jsFieldName: String, style: CSSStyleDeclaration, clearFn: () -> Unit): StylePropertyMap {
    val styleMap = this.asDynamic()[jsFieldName]
    if (styleMap == undefined) {
        val newStyleMap = StylePropertyMapKT(style, clearFn).also {
            this.asDynamic()[jsFieldName] = it
        }
        this.asDynamic()[jsFieldName] = newStyleMap
        return newStyleMap
    }
    return styleMap.unsafeCast<StylePropertyMap>()
}


inline val StyleSheet.cssRules
    get() = this.asDynamic().cssRules.unsafeCast<CSSRuleList>()


inline fun StyleSheet.deleteRule(index: Int) {
    this.asDynamic().deleteRule(index)
}

fun StyleSheet.insertRule(cssRule: String, index: Int? = null): Int {
    return if (index != null) {
        this.asDynamic().insertRule(cssRule, index).unsafeCast<Int>()
    } else {
        this.asDynamic().insertRule(cssRule).unsafeCast<Int>()
    }
}


inline operator fun CSSRuleList.get(index: Int): CSSRule {
    return this.asDynamic()[index].unsafeCast<CSSRule>()
}


external interface StylePropertyMapReadOnly {
    fun has(property: String): Boolean
}

fun StylePropertyMapReadOnly.forEach(handler: (String, Array<CSSStyleValue>) -> Unit) {
    this.asDynamic().forEach { entry: Array<dynamic> ->
        handler(
            entry[0].unsafeCast<String>(),
            entry[1].unsafeCast<Array<CSSStyleValue>>()
        )
    }
}

external interface StylePropertyMap : StylePropertyMapReadOnly {
    fun set(property: String, value: StylePropertyValue)
    fun append(property: String, value: StylePropertyValue)
    fun delete(property: String)
    fun clear()
}

@JsName("CSS")
external val CSSJS: CSSTypedOM

const val useNativeCSSTypedOM = true

actual val CSS: CSSTypedOM = if (useNativeCSSTypedOM && CSSJS != undefined && CSSJS.asDynamic().px != undefined) {
    CSSJS
} else CSSKT

val CSSStyleRule.styleMap: StylePropertyMap
    get() = polyfillStylePropertyMap(if (useNativeCSSTypedOM) "styleMap" else "styleMapKT", this.style) { cssText = "" }

val HTMLElement.attributeStyleMap: StylePropertyMap
    get() = polyfillStylePropertyMap(if (useNativeCSSTypedOM) "attributeStyleMap" else "attributeStyleMapKT", this.style) { removeAttribute("style") }