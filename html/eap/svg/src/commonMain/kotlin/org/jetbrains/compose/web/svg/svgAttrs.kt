/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.svg

import kotlinx.browser.dom.svg.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.internal.formatNumber

private fun AttrsScope<SVGElement>.numberAttr(name: String, value: Number) {
    attr(name, formatNumber(value))
}

fun AttrsScope<SVGElement>.width(px: Number) {
    numberAttr("width", px)
}

fun AttrsScope<SVGElement>.height(px: Number) {
    numberAttr("height", px)
}

fun AttrsScope<SVGElement>.width(value: CSSLengthOrPercentageValue) {
    attr("width", value.toString())
}

fun AttrsScope<SVGElement>.height(value: CSSLengthOrPercentageValue) {
    attr("height", value.toString())
}

fun AttrsScope<SVGElement>.xmlns(nameSpace: String) {
    attr("xmlns", nameSpace)
}

fun AttrsScope<SVGElement>.attributeName(attributeName: String) {
    attr("attributeName", attributeName)
}

fun AttrsScope<SVGElement>.fill(color: String) {
    attr("fill", color)
}

fun AttrsScope<SVGElement>.fillRule(fill: String) {
    attr("fill-rule", fill)
}

fun AttrsScope<SVGElement>.fillOpacity(fill: Number) {
    numberAttr("fill-opacity", fill)
}

fun AttrsScope<SVGElement>.fillOpacity(fill: CSSPercentageValue) {
    attr("fill-opacity", fill.toString())
}

fun AttrsScope<SVGElement>.href(href: String) {
    attr("href", href)
}

/**
 * Writes [viewBox] unchanged. When its components are numeric, prefer the number overload so JVM
 * and JavaScript rendering use the same representation.
 */
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
fun AttrsScope<SVGElement>.transform(transform: String) {
    attr("transform", transform)
}

/** Writes [d] unchanged; dynamically constructed values must be identical across targets. */
fun AttrsScope<SVGElement>.d(d: String) {
    attr("d", d)
}

/** Writes [points] unchanged; prefer the number overload for dynamically constructed point lists. */
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

fun AttrsScope<SVGElement>.cx(cx: Number) {
    numberAttr("cx", cx)
}

fun AttrsScope<SVGElement>.cy(cy: Number) {
    numberAttr("cy", cy)
}

fun AttrsScope<SVGElement>.r(r: Number) {
    numberAttr("r", r)
}

fun AttrsScope<SVGElement>.rx(rx: Number) {
    numberAttr("rx", rx)
}

fun AttrsScope<SVGElement>.ry(ry: Number) {
    numberAttr("ry", ry)
}

fun AttrsScope<SVGElement>.x(x: Number) {
    numberAttr("x", x)
}

fun AttrsScope<SVGElement>.y(y: Number) {
    numberAttr("y", y)
}

fun AttrsScope<SVGElement>.x1(x1: Number) {
    numberAttr("x1", x1)
}

fun AttrsScope<SVGElement>.y1(y1: Number) {
    numberAttr("y1", y1)
}

fun AttrsScope<SVGElement>.x2(x2: Number) {
    numberAttr("x2", x2)
}

fun AttrsScope<SVGElement>.y2(y2: Number) {
    numberAttr("y2", y2)
}

fun AttrsScope<SVGElement>.cx(cx: CSSLengthOrPercentageValue) {
    attr("cx", cx.toString())
}

fun AttrsScope<SVGElement>.cy(cy: CSSLengthOrPercentageValue) {
    attr("cy", cy.toString())
}

fun AttrsScope<SVGElement>.r(r: CSSLengthOrPercentageValue) {
    attr("r", r.toString())
}

fun AttrsScope<SVGElement>.rx(rx: CSSLengthOrPercentageValue) {
    attr("rx", rx.toString())
}

fun AttrsScope<SVGElement>.ry(ry: CSSLengthOrPercentageValue) {
    attr("ry", ry.toString())
}

fun AttrsScope<SVGElement>.x(x: CSSLengthOrPercentageValue) {
    attr("x", x.toString())
}

fun AttrsScope<SVGElement>.y(y: CSSLengthOrPercentageValue) {
    attr("y", y.toString())
}

fun AttrsScope<SVGElement>.x1(x1: CSSLengthOrPercentageValue) {
    attr("x1", x1.toString())
}

fun AttrsScope<SVGElement>.y1(y1: CSSLengthOrPercentageValue) {
    attr("y1", y1.toString())
}

fun AttrsScope<SVGElement>.x2(x2: CSSLengthOrPercentageValue) {
    attr("x2", x2.toString())
}

fun AttrsScope<SVGElement>.y2(y2: CSSLengthOrPercentageValue) {
    attr("y2", y2.toString())
}

fun AttrsScope<SVGElement>.to(to: String) {
    attr("to", to)
}
