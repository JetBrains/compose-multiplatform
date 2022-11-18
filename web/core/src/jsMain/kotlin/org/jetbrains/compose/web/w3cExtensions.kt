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

@PublishedApi
internal actual fun <T> SyntheticEvent<*>.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> CSSRule.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> CSSStyleValue.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> Event.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> EventTarget.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> String.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> Number.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> HTMLElement.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> SVGElement.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> StylePropertyValue.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
@ExperimentalComposeWebApi
internal actual fun <T> RadioGroupScope<*>.unsafeCast(): T = asDynamic().unsafeCast<T>()
@PublishedApi
internal actual fun <T> ElementBuilder<*>.unsafeCast(): T = asDynamic().unsafeCast<T>()

@PublishedApi
internal actual fun Any.unsafeCastBoolean(): Boolean = asDynamic().unsafeCast<Boolean>()
@PublishedApi
internal actual fun Any.unsafeCastString(): String = asDynamic().unsafeCast<String>()

@PublishedApi
internal actual fun Any.getStringProperty(name: String): String? = asDynamic()[name] as String?
@PublishedApi
internal actual fun Any.getBooleanProperty(name: String): Boolean? = asDynamic()[name] as Boolean?
@PublishedApi
internal actual fun Any.getIntProperty(name: String): Int? = asDynamic()[name] as Int?
@PublishedApi
internal actual fun <T> Any.getAnyProperty(name: String): T = asDynamic()[name].unsafeCast<T>()

internal actual fun Element.getAttributeNamesWorkaround(): Array<String> = getAttributeNames()