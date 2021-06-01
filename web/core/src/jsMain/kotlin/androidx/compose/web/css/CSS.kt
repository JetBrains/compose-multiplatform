@file:Suppress("UNUSED", "NOTHING_TO_INLINE")
package org.jetbrains.compose.web.css

import kotlinx.browser.window
import org.w3c.dom.DOMMatrix
import org.w3c.dom.DOMMatrixReadOnly
import org.w3c.dom.Element
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSRuleList
import org.w3c.dom.css.CSSStyleRule
import org.w3c.dom.css.ElementCSSInlineStyle
import org.w3c.dom.css.StyleSheet

inline val StyleSheet.cssRules
    get() = this.asDynamic().cssRules.unsafeCast<CSSRuleList>()

inline fun StyleSheet.deleteRule(index: Int) {
    this.asDynamic().deleteRule(index)
}

inline val CSSStyleRule.styleMap
    get() = this.asDynamic().styleMap.unsafeCast<StylePropertyMap>()

inline operator fun CSSRuleList.get(index: Int): CSSRule {
    return this.asDynamic()[index].unsafeCast<CSSRule>()
}

fun StyleSheet.insertRule(cssRule: String, index: Int? = null): Int {
    return if (index != null) {
        this.asDynamic().insertRule(cssRule, index).unsafeCast<Int>()
    } else {
        this.asDynamic().insertRule(cssRule).unsafeCast<Int>()
    }
}

val ElementCSSInlineStyle.attributeStyleMap
    get() = this.asDynamic().attributeStyleMap.unsafeCast<StylePropertyMap>()

external interface CSSStyleValue {
    // toString() : string
}

@JsName("CSSStyleValue")
open external class CSSStyleValueJS : CSSStyleValue {
    companion object {
        fun parse(property: String, cssText: String): CSSStyleValue
        fun parseAll(property: String, cssText: String): Array<CSSStyleValue>
    }
}

external class CSSVariableReferenceValue(
    variable: String,
    fallback: CSSUnparsedValue? = definedExternally
) {
    val variable: String
    val fallback: CSSUnparsedValue?
}

// type CSSUnparsedSegment = String | CSSVariableReferenceValue
interface CSSUnparsedSegment {
    companion object {
        operator fun invoke(value: String) = value.unsafeCast<CSSUnparsedSegment>()
        operator fun invoke(value: CSSVariableReferenceValue) =
            value.unsafeCast<CSSUnparsedSegment>()
    }
}

fun CSSUnparsedSegment.asString() = this.asDynamic() as? String
fun CSSUnparsedSegment.asCSSVariableReferenceValue() =
    this.asDynamic() as? CSSVariableReferenceValue

external class CSSUnparsedValue(members: Array<CSSUnparsedSegment>) : CSSStyleValue {
    // TODO: [Symbol.iterator]() : IterableIterator<CSSUnparsedSegment>
    fun forEach(handler: (CSSUnparsedSegment) -> Unit)
    val length: Int

    // readonly [index: number]: CSSUnparsedSegment
    operator fun get(index: Int): CSSUnparsedSegment
    operator fun set(index: Int, value: CSSUnparsedSegment)
}

external interface CSSKeywordValue : CSSStyleValue {
    val value: String
}

@JsName("CSSKeywordValue")
external class CSSKeywordValueJS(value: String) : CSSKeywordValue {
    override val value: String
}

// type CSSNumberish = number | CSSNumericValue
interface CSSNumberish {
    companion object {
        operator fun invoke(value: Number) = value.unsafeCast<CSSNumberish>()
        operator fun invoke(value: CSSNumericValue) =
            value.unsafeCast<CSSNumberish>()
    }
}

fun CSSNumberish.asNumber() = this.asDynamic() as? Number
fun CSSNumberish.asCSSNumericValue(): CSSNumericValue? = this.asDynamic() as? CSSNumericValueJS

// declare enum CSSNumericBaseType {
//     'length',
//     'angle',
//     'time',
//     'frequency',
//     'resolution',
//     'flex',
//     'percent',
// }
enum class CSSNumericBaseType(val value: String) {
    @JsName("_length")
    length("length"),
    angle("angle"),
    time("time"),
    frequency("frequency"),
    resolution("resolution"),
    flex("flex"),
    percent("percent")
}

external interface CSSNumericType {
    val length: Number
    val angle: Number
    val time: Number
    val frequency: Number
    val resolution: Number
    val flex: Number
    val percent: Number
    // percentHint: CSSNumericBaseType
}

val CSSNumericType.percentHint
    get() = CSSNumericBaseType.valueOf(this.asDynamic().percentHint)
//    set(value) { this.asDynamic().percentHint = value.value }

external interface CSSNumericValue : CSSStyleValue {
    fun add(vararg values: CSSNumberish): CSSNumericValue
    fun sub(vararg values: CSSNumberish): CSSNumericValue
    fun mul(vararg values: CSSNumberish): CSSNumericValue
    fun div(vararg values: CSSNumberish): CSSNumericValue
    fun min(vararg values: CSSNumberish): CSSNumericValue
    fun max(vararg values: CSSNumberish): CSSNumericValue

    fun equals(vararg values: CSSNumberish): Boolean

    fun to(unit: String): CSSUnitValue
    fun toSum(vararg units: String): CSSMathSum
    fun type(): CSSNumericType
}

abstract external class CSSNumericValueJS : CSSNumericValue {
    override fun add(vararg values: CSSNumberish): CSSNumericValue
    override fun sub(vararg values: CSSNumberish): CSSNumericValue
    override fun mul(vararg values: CSSNumberish): CSSNumericValue
    override fun div(vararg values: CSSNumberish): CSSNumericValue
    override fun min(vararg values: CSSNumberish): CSSNumericValue
    override fun max(vararg values: CSSNumberish): CSSNumericValue

    override fun equals(vararg values: CSSNumberish): Boolean

    override fun to(unit: String): CSSUnitValue
    override fun toSum(vararg units: String): CSSMathSum
    override fun type(): CSSNumericType

    companion object {
        fun parse(cssText: String): CSSNumericValue
    }
}

external interface CSSUnitValue : CSSNumericValue {
    val value: Number
    val unit: String
}

external interface CSSTypedUnitValue<T> : CSSNumericValue {
    val value: Number
    val unit: T
}

@JsName("CSSUnitValue")
external class CSSUnitValueJS(value: Number, unit: String) : CSSNumericValueJS, CSSUnitValue {
    override val value: Number
    override val unit: String
}

// declare enum CSSMathOperator {
//     'sum',
//     'product',
//     'negate',
//     'invert',
//     'min',
//     'max',
//     'clamp',
// }
enum class CSSMathOperator(val value: String) {
    sum("sum"),
    product("product"),
    negate("negate"),
    invert("invert"),
    min("min"),
    max("max"),
    clamp("clamp")
}

open external class CSSMathValue : CSSNumericValueJS {
    // readonly operator: CSSMathOperator
}

val CSSMathValue.operator
    get() = CSSMathOperator.valueOf(this.asDynamic().operator)
//    set(value) { this.asDynamic().operator = value.value }

external class CSSMathSum(vararg args: CSSNumberish) : CSSMathValue {
    val values: CSSNumericArray
}

external class CSSMathProduct(vararg args: CSSNumberish) : CSSMathValue {
    val values: CSSNumericArray
}

external class CSSMathNegate(arg: CSSNumberish) : CSSMathValue {
    val value: CSSNumericValue
}

external class CSSMathInvert(arg: CSSNumberish) : CSSMathValue {
    val value: CSSNumericValue
}

external class CSSMathMin(vararg args: CSSNumberish) : CSSMathValue {
    val values: CSSNumericArray
}

external class CSSMathMax(vararg args: CSSNumberish) : CSSMathValue {
    val values: CSSNumericArray
}

// TODO(yavanosta) : conflict with base class properties
// Since there is no support for this class in any browser, it's better
// wait for the implementation.
// declare class CSSMathClamp extends CSSMathValue {
// constructor(min: CSSNumberish, val: CSSNumberish, max: CSSNumberish)
//     readonly min: CSSNumericValue
//     readonly val: CSSNumericValue
//     readonly max: CSSNumericValue
// }

external class CSSNumericArray {
    // TODO: [Symbol.iterator]() : IterableIterator<CSSNumericValue>
    fun forEach(handler: (CSSNumericValue) -> Unit)
    val length: Int

    // readonly [index: number]: CSSNumericValue
    operator fun get(index: Int): CSSNumericValue
}

external class CSSTransformValue(transforms: Array<CSSTransformComponent>) : CSSStyleValue {
    // [Symbol.iterator]() : IterableIterator<CSSTransformComponent>
    fun forEach(handler: (CSSTransformComponent) -> Unit)
    val length: Int

    // [index: number]: CSSTransformComponent
    operator fun get(index: Int): CSSTransformComponent
    operator fun set(index: Int, value: CSSTransformComponent)
    val is2D: Boolean
    fun toMatrix(): DOMMatrix
}

open external class CSSTransformComponent {
    val is2D: Boolean
    fun toMatrix(): DOMMatrix
    // toString() : string
}

external class CSSTranslate(
    x: CSSNumericValue,
    y: CSSNumericValue,
    z: CSSNumericValue? = definedExternally
) : CSSTransformComponent {
    val x: CSSNumericValue
    val y: CSSNumericValue
    val z: CSSNumericValue
}

external class CSSRotate(angle: CSSNumericValue) : CSSTransformComponent {
    constructor(x: CSSNumberish, y: CSSNumberish, z: CSSNumberish, angle: CSSNumericValue)

    val x: CSSNumberish
    val y: CSSNumberish
    val z: CSSNumberish
    val angle: CSSNumericValue
}

external class CSSScale(
    x: CSSNumberish,
    y: CSSNumberish,
    z: CSSNumberish? = definedExternally
) : CSSTransformComponent {
    val x: CSSNumberish
    val y: CSSNumberish
    val z: CSSNumberish
}

external class CSSSkew(ax: CSSNumericValue, ay: CSSNumericValue) : CSSTransformComponent {
    val ax: CSSNumericValue
    val ay: CSSNumericValue
}

external class CSSSkewX(ax: CSSNumericValue) : CSSTransformComponent {
    val ax: CSSNumericValue
}

external class CSSSkewY(ay: CSSNumericValue) : CSSTransformComponent {
    val ay: CSSNumericValue
}

/* Note that skew(x,y) is *not* the same as skewX(x) skewY(y),
     thus the separate interfaces for all three. */

external class CSSPerspective(length: CSSNumericValue) : CSSTransformComponent {
    val length: CSSNumericValue
}

external class CSSMatrixComponent(
    matrix: DOMMatrixReadOnly,
    options: CSSMatrixComponentOptions? = definedExternally
) : CSSTransformComponent {
    val matrix: DOMMatrix
}

external interface CSSMatrixComponentOptions {
    val is2D: Boolean
}

external class CSSImageValue : CSSStyleValue

open external class StylePropertyMapReadOnly {
    // TODO: [Symbol.iterator]() : IterableIterator<[string, CSSStyleValue[]]>

    fun get(property: String): CSSStyleValue? // CSSStyleValue | undefined
    fun getAll(property: String): Array<CSSStyleValue>
    fun has(property: String): Boolean
    val size: Number
}

fun StylePropertyMapReadOnly.forEach(handler: (String, Array<CSSStyleValue>) -> Unit) {
    this.asDynamic().forEach { entry: Array<dynamic> ->
        handler(
            entry[0].unsafeCast<String>(),
            entry[1].unsafeCast<Array<CSSStyleValue>>()
        )
    }
}

// CSSStyleValue | string
interface StylePropertyValue {
    companion object {
        operator fun invoke(value: String) = value.unsafeCast<StylePropertyValue>()
        operator fun invoke(value: Number) = value.unsafeCast<StylePropertyValue>()
        operator fun invoke(value: CSSStyleValue) = value.unsafeCast<StylePropertyValue>()
    }
}

fun StylePropertyValue.asString() = this.asDynamic() as? String
fun StylePropertyValue.asNumber() = this.asDynamic() as? Number
fun StylePropertyValue.asCSSStyleValue(): CSSStyleValue? = this.asDynamic() as? CSSStyleValueJS

external class StylePropertyMap : StylePropertyMapReadOnly {
    fun set(property: String, vararg values: StylePropertyValue)
    fun append(property: String, vararg values: StylePropertyValue)
    fun delete(property: String)
    fun clear()
}

inline fun Element.computedStyleMap(): StylePropertyMapReadOnly =
    this.asDynamic().computedStyleMap().unsafeCast<StylePropertyMapReadOnly>()

external class CSS {
    companion object {
        fun number(value: Number): CSSUnitValue
        fun percent(value: Number): CSSUnitValue

        // <length>
        fun em(value: Number): CSSUnitValue
        fun ex(value: Number): CSSUnitValue
        fun ch(value: Number): CSSUnitValue
        fun ic(value: Number): CSSUnitValue
        fun rem(value: Number): CSSUnitValue
        fun lh(value: Number): CSSUnitValue
        fun rlh(value: Number): CSSUnitValue
        fun vw(value: Number): CSSUnitValue
        fun vh(value: Number): CSSUnitValue
        fun vi(value: Number): CSSUnitValue
        fun vb(value: Number): CSSUnitValue
        fun vmin(value: Number): CSSUnitValue
        fun vmax(value: Number): CSSUnitValue
        fun cm(value: Number): CSSUnitValue
        fun mm(value: Number): CSSUnitValue
        fun Q(value: Number): CSSUnitValue

        //        function _in(value: Number) : CSSUnitValue
//        export
//        { _in as in }
        fun pt(value: Number): CSSUnitValue
        fun pc(value: Number): CSSUnitValue
        fun px(value: Number): CSSUnitValue

        // <angle>
        fun deg(value: Number): CSSUnitValue
        fun grad(value: Number): CSSUnitValue
        fun rad(value: Number): CSSUnitValue
        fun turn(value: Number): CSSUnitValue

        // <time>
        fun s(value: Number): CSSUnitValue
        fun ms(value: Number): CSSUnitValue

        // <frequency>
        fun Hz(value: Number): CSSUnitValue
        fun kHz(value: Number): CSSUnitValue

        // <resolution>
        fun dpi(value: Number): CSSUnitValue
        fun dpcm(value: Number): CSSUnitValue
        fun dppx(value: Number): CSSUnitValue

        // <flex>
        fun fr(value: Number): CSSUnitValue
    }
}

@Suppress("unused")
val cssTypedOMPolyfill = CSSTypedOMPolyfill.default(window)
