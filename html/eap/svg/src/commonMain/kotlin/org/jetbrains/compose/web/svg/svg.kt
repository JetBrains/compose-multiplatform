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
private fun <TElement : SVGElement> TagElementSvgNS(
    tagName: String,
    applyAttrs: AttrBuilderContext<TElement>?,
    content: ContentBuilder<TElement>?,
) {
    TagElementNS(
        tagName = tagName,
        namespace = SVG_NS,
        applyAttrs = applyAttrs,
        content = content,
    )
}

/**
 * Creates an SVG root element.
 *
 * String rendering produces HTML syntax and does not add an `xmlns` declaration. It is intended
 * for embedding in HTML, rather than serializing standalone XML `.svg` documents.
 *
 * For numeric view-box components, prefer `attrs = { viewBox(minX, minY, width, height) }` so
 * values are formatted consistently across platforms.
 */
@Composable
@ExperimentalComposeWebSvgApi
fun Svg(
  viewBox: String? = null,
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElementSvgNS<SVGElement>(
        tagName = "svg",
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
    TagElementSvgNS<SVGAElement>(
        tagName = "a",
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
    TagElementSvgNS<SVGCircleElement>(
        tagName = "circle",
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
    TagElementSvgNS<SVGCircleElement>(
        tagName = "circle",
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
    TagElementSvgNS<SVGTextElement>(
        tagName = "text",
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
    TagElementSvgNS<SVGViewElement>(
        tagName = "view",
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
    TagElementSvgNS<SVGRectElement>(
        tagName = "rect",
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
    TagElementSvgNS<SVGRectElement>(
        tagName = "rect",
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
    TagElementSvgNS<SVGRectElement>(
        tagName = "rect",
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
    TagElementSvgNS<SVGEllipseElement>(
        tagName = "ellipse",
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
    TagElementSvgNS<SVGEllipseElement>(
        tagName = "ellipse",
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
    TagElementSvgNS<SVGSymbolElement>(
        tagName = "symbol",
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
    TagElementSvgNS<SVGUseElement>(
        tagName = "use",
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
    TagElementSvgNS<SVGLineElement>(
        tagName = "line",
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
    TagElementSvgNS<SVGLineElement>(
        tagName = "line",
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
    TagElementSvgNS<SVGClipPathElement>(
        tagName = "clipPath",
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
    TagElementSvgNS<SVGPathElement>(
        tagName = "path",
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
    TagElementSvgNS<SVGElement>(
        tagName = "g",
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
    TagElementSvgNS<SVGImageElement>(
        tagName = "image",
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
    TagElementSvgNS<SVGMaskElement>(
        tagName = "mask",
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
    TagElementSvgNS<SVGDefsElement>(
        tagName = "defs",
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
    TagElementSvgNS<SVGPatternElement>(
        tagName = "pattern",
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
    TagElementSvgNS<SVGPolygonElement>(
        tagName = "polygon",
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
    TagElementSvgNS<SVGPolylineElement>(
        tagName = "polyline",
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
    TagElementSvgNS<SVGTextPathElement>(
        tagName = "textPath",
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
    TagElementSvgNS<SVGElement>(
        tagName = "animate",
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
    TagElementSvgNS<SVGElement>(
        tagName = "animateMotion",
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
    TagElementSvgNS<SVGElement>(
        tagName = "animateTransform",
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
    TagElementSvgNS<SVGLinearGradientElement>(
        tagName = "linearGradient",
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
    TagElementSvgNS<SVGRadialGradientElement>(
        tagName = "radialGradient",
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
    TagElementSvgNS<SVGStopElement>(
        tagName = "stop",
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
    TagElementSvgNS<SVGSwitchElement>(
        tagName = "switch",
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
    TagElementSvgNS<SVGTitleElement>(
        tagName = "title",
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
    TagElementSvgNS<SVGTSpanElement>(
        tagName = "tspan",
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
    TagElementSvgNS<SVGDescElement>(
        tagName = "desc",
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
    TagElementSvgNS<SVGMarkerElement>(
        tagName = "marker",
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
    TagElementSvgNS<SVGElement>(
        tagName = "mpath",
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
    TagElementSvgNS<SVGElement>(
        tagName = "filter",
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
    TagElementSvgNS<SVGElement>(
        tagName = "set",
        applyAttrs = {
            attributeName(attributeName)
            to(to)
            attrs?.invoke(this)
        },
        content = content
    )
}

/**
 * Creates a custom SVG element.
 *
 * When the result is string-rendered and parsed as HTML, custom tag and attribute names are
 * ASCII-lowercased unless they are part of the HTML parser's fixed SVG name-adjustment tables.
 * String rendering rejects names whose casing would change. Use lowercase names for custom
 * elements and custom attributes that must hydrate from HTML.
 * To embed HTML content, use `SvgElement<SVGElement>("foreignObject") { Div { ... } }` inside [Svg].
 * For SVG styles or scripts, use `SvgElement<SVGElement>("style") { Text(css) }` or
 * `SvgElement<SVGElement>("script") { Text(code) }`. These emit text content; the core [Style] and
 * [Script] composables create HTML elements, and the [Style] CSSOM API does not apply here.
 * String-rendered SVG elements must be descendants of an [Svg] root; standalone SVG fragments do
 * not acquire the SVG namespace when parsed as HTML.
 */
@Composable
@ExperimentalComposeWebSvgApi
fun <T : SVGElement> SvgElement(
  name: String,
  attrs: AttrBuilderContext<T>? = null,
  content: ContentBuilder<T>? = null
) {
    TagElementSvgNS<T>(
        tagName = name,
        applyAttrs = attrs,
        content = content
    )
}
