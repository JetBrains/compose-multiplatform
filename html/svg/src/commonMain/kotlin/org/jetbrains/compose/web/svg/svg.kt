/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.svg

import androidx.compose.runtime.Composable
import kotlinx.browser.css.masking.SVGClipPathElement
import kotlinx.browser.css.masking.SVGMaskElement
import kotlinx.browser.dom.svg.*
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.css.CSSLengthOrPercentageValue
import org.jetbrains.compose.web.dom.*

const val SVG_NS = "http://www.w3.org/2000/svg"

@Composable
@ExperimentalComposeWebSvgApi
fun Svg(
  viewBox: String? = null,
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "svg",
        namespace = SVG_NS,
        applyAttrs = {
            viewBox?.let { viewBox(it) }
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.SvgA(
  href: String,
  attrs: AttrBuilderContext<SVGAElement>? = null,
  content: ContentBuilder<SVGAElement>? = null
) {
    TagElementNS<SVGAElement>(
        tagName = "a",
        namespace = SVG_NS,
        applyAttrs = {
            href(href)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Circle(
  cx: CSSLengthOrPercentageValue,
  cy: CSSLengthOrPercentageValue,
  r: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGCircleElement>? = null,
  content: ContentBuilder<SVGCircleElement>? = null
) {
    TagElementNS<SVGCircleElement>(
        tagName = "circle",
        namespace = SVG_NS,
        applyAttrs = {
            cx(cx)
            cy(cy)
            r(r)
            attrs?.invoke(this)
        },
        content = content
    )
}


@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Circle(
  cx: Number,
  cy: Number,
  r: Number,
  attrs: AttrBuilderContext<SVGCircleElement>? = null,
  content: ContentBuilder<SVGCircleElement>? = null
) {
    TagElementNS<SVGCircleElement>(
        tagName = "circle",
        namespace = SVG_NS,
        applyAttrs = {
            cx(cx)
            cy(cy)
            r(r)
            attrs?.invoke(this)
        },
        content = content
    )
}


@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.SvgText(
  text: String,
  x: Number = 0,
  y: Number = 0,
  attrs: AttrBuilderContext<SVGTextElement>? = null,
) {
    TagElementNS<SVGTextElement>(
        tagName = "text",
        namespace = SVG_NS,
        applyAttrs = {
            x(x)
            y(y)
            attrs?.invoke(this)
        },
        content = {
            Text(text)
        }
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.View(
  id: String,
  viewBox: String,
  attrs: AttrBuilderContext<SVGViewElement>? = null,
) {
    TagElementNS<SVGViewElement>(
        tagName = "view",
        namespace = SVG_NS,
        applyAttrs = {
            id(id)
            viewBox(viewBox)
            attrs?.invoke(this)
        },
        content = null
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Rect(
  x: Number,
  y: Number,
  width: Number,
  height: Number,
  attrs: AttrBuilderContext<SVGRectElement>? = null,
  content: ContentBuilder<SVGRectElement>? = null
) {
    TagElementNS<SVGRectElement>(
        tagName = "rect",
        namespace = SVG_NS,
        applyAttrs = {
            x(x)
            y(y)
            width(width)
            height(height)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Rect(
  x: CSSLengthOrPercentageValue,
  y: CSSLengthOrPercentageValue,
  width: CSSLengthOrPercentageValue,
  height: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGRectElement>? = null,
  content: ContentBuilder<SVGRectElement>? = null
) {
    TagElementNS<SVGRectElement>(
        tagName = "rect",
        namespace = SVG_NS,
        applyAttrs = {
            x(x)
            y(y)
            width(width)
            height(height)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Rect(
    width: Number,
    height: Number,
    rx: Number,
    ry: Number = rx,
    transform: String,
    attrs: AttrBuilderContext<SVGRectElement>? = null,
    content: ContentBuilder<SVGRectElement>? = null
) {
    TagElementNS<SVGRectElement>(
        tagName = "rect",
        namespace = SVG_NS,
        applyAttrs = {
            width(width)
            height(height)
            rx(rx)
            ry(ry)
            transform(transform)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Ellipse(
  cx: CSSLengthOrPercentageValue,
  cy: CSSLengthOrPercentageValue,
  rx: CSSLengthOrPercentageValue,
  ry: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGEllipseElement>? = null,
  content: ContentBuilder<SVGEllipseElement>? = null
) {
    TagElementNS<SVGEllipseElement>(
        tagName = "ellipse",
        namespace = SVG_NS,
        applyAttrs = {
            cx(cx)
            cy(cy)
            rx(rx)
            ry(ry)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Ellipse(
  cx: Number,
  cy: Number,
  rx: Number,
  ry: Number,
  attrs: AttrBuilderContext<SVGEllipseElement>? = null,
  content: ContentBuilder<SVGEllipseElement>? = null
) {
    TagElementNS<SVGEllipseElement>(
        tagName = "ellipse",
        namespace = SVG_NS,
        applyAttrs = {
            cx(cx)
            cy(cy)
            rx(rx)
            ry(ry)
            attrs?.invoke(this)
        },
        content = content
    )
}


@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Symbol(
  id: String? = null,
  attrs: AttrBuilderContext<SVGSymbolElement>? = null,
  content: ContentBuilder<SVGSymbolElement>? = null
) {
    TagElementNS<SVGSymbolElement>(
        tagName = "symbol",
        namespace = SVG_NS,
        applyAttrs = {
            id?.let { id(it) }
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Use(
  href: String,
  attrs: AttrBuilderContext<SVGUseElement>? = null,
  content: ContentBuilder<SVGUseElement>? = null
) {
    TagElementNS<SVGUseElement>(
        tagName = "use",
        namespace = SVG_NS,
        applyAttrs = {
            href(href)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Line(
  x1: CSSLengthOrPercentageValue,
  y1: CSSLengthOrPercentageValue,
  x2: CSSLengthOrPercentageValue,
  y2: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGLineElement>? = null,
  content: ContentBuilder<SVGLineElement>? = null
) {
    TagElementNS<SVGLineElement>(
        tagName = "line",
        namespace = SVG_NS,
        applyAttrs = {
            x1(x1)
            y1(y1)
            x2(x2)
            y2(y2)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Line(
  x1: Number,
  y1: Number,
  x2: Number,
  y2: Number,
  attrs: AttrBuilderContext<SVGLineElement>? = null,
  content: ContentBuilder<SVGLineElement>? = null
) {
    TagElementNS<SVGLineElement>(
        tagName = "line",
        namespace = SVG_NS,
        applyAttrs = {
            x1(x1)
            y1(y1)
            x2(x2)
            y2(y2)
            attrs?.invoke(this)
        },
        content = content
    )
}


@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.ClipPath(
  id: String,
  attrs: AttrBuilderContext<SVGClipPathElement>? = null,
  content: ContentBuilder<SVGClipPathElement>? = null
) {
    TagElementNS<SVGClipPathElement>(
        tagName = "clipPath",
        namespace = SVG_NS,
        applyAttrs = {
            id(id)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Path(
  d: String,
  attrs: AttrBuilderContext<SVGPathElement>? = null,
  content: ContentBuilder<SVGPathElement>? = null
) {
    TagElementNS<SVGPathElement>(
        tagName = "path",
        namespace = SVG_NS,
        applyAttrs = {
            d(d)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.G(
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "g",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Image(
  href: String,
  attrs: AttrBuilderContext<SVGImageElement>? = null,
  content: ContentBuilder<SVGImageElement>? = null
) {
    TagElementNS<SVGImageElement>(
        tagName = "image",
        namespace = SVG_NS,
        applyAttrs = {
            href(href)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Mask(
  id: String? = null,
  attrs: AttrBuilderContext<SVGMaskElement>? = null,
  content: ContentBuilder<SVGMaskElement>? = null
) {
    TagElementNS<SVGMaskElement>(
        tagName = "mask",
        namespace = SVG_NS,
        applyAttrs = {
           id?.let { id(it) }
           attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Defs(
  attrs: AttrBuilderContext<SVGDefsElement>? = null,
  content: ContentBuilder<SVGDefsElement>? = null
) {
    TagElementNS<SVGDefsElement>(
        tagName = "defs",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Pattern(
  id: String,
  attrs: AttrBuilderContext<SVGPatternElement>? = null,
  content: ContentBuilder<SVGPatternElement>? = null
) {
    TagElementNS<SVGPatternElement>(
        tagName = "pattern",
        namespace = SVG_NS,
        applyAttrs = {
            id(id)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Polygon(
  vararg points: Number,
  attrs: AttrBuilderContext<SVGPolygonElement>? = null,
  content: ContentBuilder<SVGPolygonElement>? = null
) {
    TagElementNS<SVGPolygonElement>(
        tagName = "polygon",
        namespace = SVG_NS,
        applyAttrs = {
            points(points = points)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Polyline(
  vararg points: Number,
  attrs: AttrBuilderContext<SVGPolylineElement>? = null,
  content: ContentBuilder<SVGPolylineElement>? = null
) {
    TagElementNS<SVGPolylineElement>(
        tagName = "polyline",
        namespace = SVG_NS,
        applyAttrs = {
            points(points = points)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.TextPath(
  href: String,
  text: String,
  attrs: AttrBuilderContext<SVGTextPathElement>? = null,
) {
    TagElementNS<SVGTextPathElement>(
        tagName = "textPath",
        namespace = SVG_NS,
        applyAttrs = {
            href(href)
            attrs?.invoke(this)
        },
        content = {
            Text(text)
        }
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Animate(
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "animate",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.AnimateMotion(
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "animateMotion",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.AnimateTransform(
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "animateTransform",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.LinearGradient(
  id: String? = null,
  attrs: AttrBuilderContext<SVGLinearGradientElement>? = null,
  content: ContentBuilder<SVGLinearGradientElement>? = null
) {
    TagElementNS<SVGLinearGradientElement>(
        tagName = "linearGradient",
        namespace = SVG_NS,
        applyAttrs = {
            id?.let { id(it) }
            attrs?.invoke(this)
        },
        content = content
    )
}


@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.RadialGradient(
  id: String? = null,
  attrs: AttrBuilderContext<SVGRadialGradientElement>? = null,
  content: ContentBuilder<SVGRadialGradientElement>? = null
) {
    TagElementNS<SVGRadialGradientElement>(
        tagName = "radialGradient",
        namespace = SVG_NS,
        applyAttrs = {
            id?.let { id(it) }
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Stop(
  attrs: AttrBuilderContext<SVGStopElement>? = null,
  content: ContentBuilder<SVGStopElement>? = null
) {
    TagElementNS<SVGStopElement>(
        tagName = "stop",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Switch(
  attrs: AttrBuilderContext<SVGSwitchElement>? = null,
  content: ContentBuilder<SVGSwitchElement>? = null
) {
    TagElementNS<SVGSwitchElement>(
        tagName = "switch",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Title(
  text: String,
  attrs: AttrBuilderContext<SVGTitleElement>? = null,
) {
    TagElementNS<SVGTitleElement>(
        tagName = "title",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = {
            Text(text)
        }
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Tspan(
  attrs: AttrBuilderContext<SVGTSpanElement>? = null,
  content: ContentBuilder<SVGTSpanElement>? = null
) {
    TagElementNS<SVGTSpanElement>(
        tagName = "tspan",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Desc(
  content: String,
  attrs: AttrBuilderContext<SVGDescElement>? = null,
) {
    TagElementNS<SVGDescElement>(
        tagName = "desc",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = {
            Text(content)
        }
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Marker(
  attrs: AttrBuilderContext<SVGMarkerElement>? = null,
  content: ContentBuilder<SVGMarkerElement>? = null
) {
    TagElementNS<SVGMarkerElement>(
        tagName = "marker",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Mpath(
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "mpath",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Filter(
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "filter",
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Set(
  attributeName: String,
  to: String,
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementNS<SVGElement>(
        tagName = "set",
        namespace = SVG_NS,
        applyAttrs = {
            attributeName(attributeName)
            to(to)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun <T : SVGElement> SvgElement(
  name: String,
  attrs: AttrBuilderContext<T>? = null,
  content: ContentBuilder<T>? = null
) {
    TagElementNS<T>(
        tagName = name,
        namespace = SVG_NS,
        applyAttrs = attrs,
        content = content
    )
}
