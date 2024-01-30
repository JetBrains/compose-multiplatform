/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.w3c.dom.svg.*

fun AttrsScope<SVGElement>.width(px: Number) {
    attr("width", px.toString())
}

fun AttrsScope<SVGElement>.height(px: Number) {
    attr("height", px.toString())
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
    attr("fill-opacity", fill.toString())
}

fun AttrsScope<SVGElement>.fillOpacity(fill: CSSPercentageValue) {
    attr("fill-opacity", fill.toString())
}

fun AttrsScope<SVGElement>.href(href: String) {
    attr("href", href)
}

fun AttrsScope<SVGElement>.viewBox(viewBox: String) {
    attr("viewBox", viewBox)
}

fun AttrsScope<SVGElement>.transform(transform: String) {
    attr("transform", transform)
}

fun AttrsScope<SVGElement>.d(d: String) {
    attr("d", d)
}

fun AttrsScope<SVGElement>.points(points: String) {
    attr("points", points)
}

fun AttrsScope<SVGElement>.points(vararg points: Number) {
    attr("points", points.toList().chunked(2).joinToString(" ") { it.joinToString(",") })
}

fun AttrsScope<SVGElement>.cx(cx: Number) {
    attr("cx", cx.toString())
}

fun AttrsScope<SVGElement>.cy(cy: Number) {
    attr("cy", cy.toString())
}

fun AttrsScope<SVGElement>.r(r: Number) {
    attr("r", r.toString())
}

fun AttrsScope<SVGElement>.rx(rx: Number) {
    attr("rx", rx.toString())
}

fun AttrsScope<SVGElement>.ry(ry: Number) {
    attr("ry", ry.toString())
}

fun AttrsScope<SVGElement>.x(x: Number) {
    attr("x", x.toString())
}

fun AttrsScope<SVGElement>.y(y: Number) {
    attr("y", y.toString())
}

fun AttrsScope<SVGElement>.x1(x1: Number) {
    attr("x1", x1.toString())
}

fun AttrsScope<SVGElement>.y1(y1: Number) {
    attr("y1", y1.toString())
}

fun AttrsScope<SVGElement>.x2(x2: Number) {
    attr("x2", x2.toString())
}

fun AttrsScope<SVGElement>.y2(y2: Number) {
    attr("y2", y2.toString())
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
