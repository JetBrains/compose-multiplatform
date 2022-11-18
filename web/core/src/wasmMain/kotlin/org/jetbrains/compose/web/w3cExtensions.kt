package org.jetbrains.compose.web

import org.w3c.dom.events.*
import org.w3c.dom.css.CSSRule
import androidx.compose.web.events.*
import org.jetbrains.compose.web.css.CSSStyleValue
import org.w3c.dom.HTMLElement
import org.w3c.dom.svg.SVGElement
import org.jetbrains.compose.web.css.StylePropertyValue
import org.jetbrains.compose.web.dom.RadioGroupScope
import org.jetbrains.compose.web.dom.ElementBuilder
import org.w3c.dom.Element

@Suppress("UNCHECKED_CAST")
private fun <T> jsUnsafeCast(obj: Any): T = obj as T

@JsFun("x => x")
private external fun jsUnsafeCastBoolean(obj: Any): Boolean

@JsFun("x => x")
private external fun jsUnsafeCastString(obj: Any): String

@JsFun("(obj, name) => obj['name']")
private external fun jsGetStringProperty(obj: Any, name: String): String?

@JsFun("(obj, name) => obj['name']")
private external fun jsGetBooleanProperty(obj: Any, name: String): Boolean?

@JsFun("(obj, name) => obj['name']")
private external fun jsGetIntProperty(obj: Any, name: String): Int?

@JsFun("(obj, name) => obj['name']")
private external fun <T> jsGetAnyProperty(obj: Any, name: String): T

@PublishedApi
internal actual fun <T> SyntheticEvent<*>.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> CSSRule.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> CSSStyleValue.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> Event.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> EventTarget.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> String.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> Number.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> HTMLElement.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> SVGElement.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> StylePropertyValue.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
@ExperimentalComposeWebApi
internal actual fun <T> RadioGroupScope<*>.unsafeCast(): T = jsUnsafeCast(this)
@PublishedApi
internal actual fun <T> ElementBuilder<*>.unsafeCast(): T = jsUnsafeCast(this)

@PublishedApi
internal actual fun Any.unsafeCastBoolean(): Boolean = jsUnsafeCastBoolean(this)
@PublishedApi
internal actual fun Any.unsafeCastString(): String = jsUnsafeCastString(this)

@PublishedApi
internal actual fun Any.getStringProperty(name: String): String? = jsGetStringProperty(this, name)
@PublishedApi
internal actual fun Any.getBooleanProperty(name: String): Boolean? = jsGetBooleanProperty(this, name)
@PublishedApi
internal actual fun Any.getIntProperty(name: String): Int? = jsGetIntProperty(this, name)
@PublishedApi
internal actual fun <T> Any.getAnyProperty(name: String): T = jsGetAnyProperty(this, name)

@JsFun("(node) => node.getAttributeNames()")
private external fun jsGetAttributeNamesArray(node: Element): Dynamic

@JsFun("(array) => array.length")
private external fun jsGetArrayLength(array: Dynamic): Int

@JsFun("(array, index) => array[index]")
private external fun jsGetArrayElementAsString(array: Dynamic, index: Int): String

internal actual fun Element.getAttributeNamesWorkaround(): Array<String> {
    val attributesArray = jsGetAttributeNamesArray(this)
    return Array(jsGetArrayLength(attributesArray)) {
        jsGetArrayElementAsString(attributesArray, it)
    }
}