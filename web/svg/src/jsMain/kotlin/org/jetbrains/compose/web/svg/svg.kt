/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.css.CSSLengthOrPercentageValue
import org.jetbrains.compose.web.dom.*
import org.w3c.css.masking.SVGClipPathElement
import org.w3c.css.masking.SVGMaskElement
import org.w3c.dom.Element
import org.w3c.dom.svg.*

private open class ElementBuilderNS<TElement : Element>(private val tagName: String, private val namespace: String) :
    ElementBuilder<TElement> {
    private val el: Element by lazy { document.createElementNS(namespace, tagName) }
    override fun create(): TElement = el.cloneNode().unsafeCast<TElement>()
}

const val SVG_NS = "http://www.w3.org/2000/svg"

private val A = ElementBuilderNS<SVGAElement>("a", SVG_NS)
private val Animate = ElementBuilderNS<SVGElement>("animate", SVG_NS)
private val AnimateMotion = ElementBuilderNS<SVGElement>("animateMotion", SVG_NS)
private val AnimateTransform = ElementBuilderNS<SVGElement>("animateTransform", SVG_NS)
private val Circle = ElementBuilderNS<SVGCircleElement>("circle", SVG_NS)
private val ClipPath = ElementBuilderNS<SVGClipPathElement>("clipPath", SVG_NS)
private val Defs = ElementBuilderNS<SVGDefsElement>("defs", SVG_NS)
private val Desc = ElementBuilderNS<SVGDescElement>("desc", SVG_NS)
private val Ellipse = ElementBuilderNS<SVGEllipseElement>("ellipse", SVG_NS)
private val Filter = ElementBuilderNS<SVGElement>("filter", SVG_NS)
private val G = ElementBuilderNS<SVGElement>("g", SVG_NS)
private val Image = ElementBuilderNS<SVGImageElement>("image", SVG_NS)
private val Line = ElementBuilderNS<SVGLineElement>("line", SVG_NS)
private val LinearGradient = ElementBuilderNS<SVGLinearGradientElement>("linearGradient", SVG_NS)
private val Marker = ElementBuilderNS<SVGMarkerElement>("marker", SVG_NS)
private val Mask = ElementBuilderNS<SVGMaskElement>("mask", SVG_NS)
private val Mpath = ElementBuilderNS<SVGElement>("mpath", SVG_NS)
private val Path = ElementBuilderNS<SVGPathElement>("path", SVG_NS)
private val Pattern = ElementBuilderNS<SVGPatternElement>("pattern", SVG_NS)
private val Polygon = ElementBuilderNS<SVGPolygonElement>("polygon", SVG_NS)
private val Polyline = ElementBuilderNS<SVGPolylineElement>("polyline", SVG_NS)
private val RadialGradient = ElementBuilderNS<SVGRadialGradientElement>("radialGradient", SVG_NS)
private val Rect = ElementBuilderNS<SVGRectElement>("rect", SVG_NS)
private val Set = ElementBuilderNS<SVGElement>("set", SVG_NS)
private val Stop = ElementBuilderNS<SVGStopElement>("stop", SVG_NS)
private val Svg = ElementBuilderNS<SVGElement>("svg", SVG_NS)
private val Switch = ElementBuilderNS<SVGSwitchElement>("switch", SVG_NS)
private val Symbol = ElementBuilderNS<SVGSymbolElement>("symbol", SVG_NS)
private val Text = ElementBuilderNS<SVGTextElement>("text", SVG_NS)
private val TextPath = ElementBuilderNS<SVGTextPathElement>("textPath", SVG_NS)
private val Title = ElementBuilderNS<SVGTitleElement>("title", SVG_NS)
private val Tspan = ElementBuilderNS<SVGTSpanElement>("tspan", SVG_NS)
private val Use = ElementBuilderNS<SVGUseElement>("use", SVG_NS)
private val View = ElementBuilderNS<SVGViewElement>("view", SVG_NS)

@Composable
@ExperimentalComposeWebSvgApi
fun Svg(
  viewBox: String? = null,
  attrs: AttrBuilderContext<SVGElement>? = null,
  content: ContentBuilder<SVGElement>? = null
) {
    TagElement(
        elementBuilder = Svg,
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
    TagElement(
        elementBuilder = A,
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
    TagElement(
        elementBuilder = Circle,
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
    TagElement(
        elementBuilder = Circle,
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
    TagElement(
        elementBuilder = Text,
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
    TagElement(
        elementBuilder = View,
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
    TagElement(
        elementBuilder = Rect,
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
    TagElement(
        elementBuilder = Rect,
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
    TagElement(
        elementBuilder = Rect,
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
    TagElement(
        elementBuilder = Ellipse,
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
    TagElement(
        elementBuilder = Ellipse,
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
    TagElement(
        elementBuilder = Symbol,
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
    TagElement(
        elementBuilder = Use,
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
    TagElement(
        elementBuilder = Line,
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
    TagElement(
        elementBuilder = Line,
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
    TagElement(
        elementBuilder = ClipPath,
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
    TagElement(
        elementBuilder = Path,
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
    TagElement(
        elementBuilder = G,
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
    TagElement(
        elementBuilder = Image,
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
    TagElement(
        elementBuilder = Mask,
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
    TagElement(
        elementBuilder = Defs,
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
    TagElement(
        elementBuilder = Pattern,
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
    TagElement(
        elementBuilder = Polygon,
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
    TagElement(
        elementBuilder = Polyline,
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
    TagElement(
        elementBuilder = TextPath,
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
    TagElement(
        elementBuilder = Animate,
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
    TagElement(
        elementBuilder = AnimateMotion,
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
    TagElement(
        elementBuilder = AnimateTransform,
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
    TagElement(
        elementBuilder = LinearGradient,
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
    TagElement(
        elementBuilder = RadialGradient,
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
    TagElement(
        elementBuilder = Stop,
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
    TagElement(
        elementBuilder = Switch,
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
    TagElement(
        elementBuilder = Title,
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
    TagElement(
        elementBuilder = Tspan,
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
    TagElement(
        elementBuilder = Desc,
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
    TagElement(
        elementBuilder = Marker,
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
    TagElement(
        elementBuilder = Mpath,
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
    TagElement(
        elementBuilder = Filter,
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
    TagElement(
        elementBuilder = Set,
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
    TagElement(
        elementBuilder = ElementBuilderNS(name, SVG_NS),
        applyAttrs = attrs,
        content = content
    )
}
