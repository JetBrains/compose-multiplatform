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
internal expect fun <T> SyntheticEvent<*>.unsafeCast(): T
@PublishedApi
internal expect fun <T> CSSRule.unsafeCast(): T
@PublishedApi
internal expect fun <T> CSSStyleValue.unsafeCast(): T
@PublishedApi
internal expect fun <T> Event.unsafeCast(): T
@PublishedApi
internal expect fun <T> EventTarget.unsafeCast(): T
@PublishedApi
internal expect fun <T> String.unsafeCast(): T
@PublishedApi
internal expect fun <T> Number.unsafeCast(): T
@PublishedApi
internal expect fun <T> SVGElement.unsafeCast(): T
@PublishedApi
internal expect fun <T> HTMLElement.unsafeCast(): T
@PublishedApi
internal expect fun <T> StylePropertyValue.unsafeCast(): T
@PublishedApi
@ExperimentalComposeWebApi
internal expect fun <T> RadioGroupScope<*>.unsafeCast(): T
@PublishedApi
internal expect fun <T> ElementBuilder<*>.unsafeCast(): T

@PublishedApi
internal expect fun Any.unsafeCastBoolean(): Boolean
@PublishedApi
internal expect fun Any.unsafeCastString(): String

@PublishedApi
internal expect fun Any.getStringProperty(name: String): String?
@PublishedApi
internal expect fun Any.getBooleanProperty(name: String): Boolean?
@PublishedApi
internal expect fun Any.getIntProperty(name: String): Int?
@PublishedApi
internal expect fun <T> Any.getAnyProperty(name: String): T

internal expect fun Element.getAttributeNamesWorkaround(): Array<String>