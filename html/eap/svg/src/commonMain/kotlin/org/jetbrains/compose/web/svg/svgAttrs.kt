/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.dom.LowerableAttribute
import kotlinx.browser.dom.svg.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.internal.formatNumber

private fun AttrsScope<SVGElement>.numberAttr(name: String, value: Number) {
    attr(name, formatNumber(value))
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.width(px: Number) {
    numberAttr("width", px)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.height(px: Number) {
    numberAttr("height", px)
}

@LowerableAttribute
fun AttrsScope<SVGElement>.width(value: CSSLengthOrPercentageValue) {
    attr("width", value.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.height(value: CSSLengthOrPercentageValue) {
    attr("height", value.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.xmlns(nameSpace: String) {
    attr("xmlns", nameSpace)
}

@LowerableAttribute
fun AttrsScope<SVGElement>.attributeName(attributeName: String) {
    attr("attributeName", attributeName)
}

@LowerableAttribute
fun AttrsScope<SVGElement>.fill(color: String) {
    attr("fill", color)
}

@LowerableAttribute("fill-rule")
fun AttrsScope<SVGElement>.fillRule(fill: String) {
    attr("fill-rule", fill)
}

@LowerableAttribute("fill-opacity", conversion = "formatNumber")
fun AttrsScope<SVGElement>.fillOpacity(fill: Number) {
    numberAttr("fill-opacity", fill)
}

@LowerableAttribute("fill-opacity")
fun AttrsScope<SVGElement>.fillOpacity(fill: CSSPercentageValue) {
    attr("fill-opacity", fill.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.href(href: String) {
    attr("href", href)
}

/**
 * Writes [viewBox] unchanged. When its components are numeric, prefer the number overload so JVM
 * and JavaScript rendering use the same representation.
 */
@LowerableAttribute
fun AttrsScope<SVGElement>.viewBox(viewBox: String) {
    attr("viewBox", viewBox)
}

fun AttrsScope<SVGElement>.viewBox(
    minX: Number,
    minY: Number,
    width: Number,
    height: Number,
) {
    attr("viewBox", listOf(minX, minY, width, height).joinToString(" ", transform = ::formatNumber))
}

/** Writes [transform] unchanged; dynamically constructed values must be identical across targets. */
@LowerableAttribute
fun AttrsScope<SVGElement>.transform(transform: String) {
    attr("transform", transform)
}

/** Writes [d] unchanged; dynamically constructed values must be identical across targets. */
@LowerableAttribute
fun AttrsScope<SVGElement>.d(d: String) {
    attr("d", d)
}

/** Writes [points] unchanged; prefer the number overload for dynamically constructed point lists. */
@LowerableAttribute
fun AttrsScope<SVGElement>.points(points: String) {
    attr("points", points)
}

fun AttrsScope<SVGElement>.points(vararg points: Number) {
    attr(
        "points",
        points.toList().chunked(2).joinToString(" ") {
            it.joinToString(",", transform = ::formatNumber)
        }
    )
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.cx(cx: Number) {
    numberAttr("cx", cx)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.cy(cy: Number) {
    numberAttr("cy", cy)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.r(r: Number) {
    numberAttr("r", r)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.rx(rx: Number) {
    numberAttr("rx", rx)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.ry(ry: Number) {
    numberAttr("ry", ry)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.x(x: Number) {
    numberAttr("x", x)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.y(y: Number) {
    numberAttr("y", y)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.x1(x1: Number) {
    numberAttr("x1", x1)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.y1(y1: Number) {
    numberAttr("y1", y1)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.x2(x2: Number) {
    numberAttr("x2", x2)
}

@LowerableAttribute(conversion = "formatNumber")
fun AttrsScope<SVGElement>.y2(y2: Number) {
    numberAttr("y2", y2)
}

@LowerableAttribute
fun AttrsScope<SVGElement>.cx(cx: CSSLengthOrPercentageValue) {
    attr("cx", cx.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.cy(cy: CSSLengthOrPercentageValue) {
    attr("cy", cy.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.r(r: CSSLengthOrPercentageValue) {
    attr("r", r.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.rx(rx: CSSLengthOrPercentageValue) {
    attr("rx", rx.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.ry(ry: CSSLengthOrPercentageValue) {
    attr("ry", ry.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.x(x: CSSLengthOrPercentageValue) {
    attr("x", x.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.y(y: CSSLengthOrPercentageValue) {
    attr("y", y.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.x1(x1: CSSLengthOrPercentageValue) {
    attr("x1", x1.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.y1(y1: CSSLengthOrPercentageValue) {
    attr("y1", y1.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.x2(x2: CSSLengthOrPercentageValue) {
    attr("x2", x2.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.y2(y2: CSSLengthOrPercentageValue) {
    attr("y2", y2.toString())
}

@LowerableAttribute
fun AttrsScope<SVGElement>.to(to: String) {
    attr("to", to)
}
