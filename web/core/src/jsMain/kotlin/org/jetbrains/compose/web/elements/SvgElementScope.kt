/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.elements

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.ElementScopeImpl
import org.jetbrains.compose.web.dom.SVGElement as ComposeSvgElement
import org.w3c.dom.svg.SVGElement

/**
 * Questions:
 *
 * 1) What attributes do we want/need to add to elements' signatures?
 * (For now, signatures contain only coordinates and size related attrs)
 *
 * 2) Maybe we need to rename CSSUnit into something more unified,
 * so the same units can be reused for SvgLength for example. Otherwise, there is a conflict in units definitions.
 *
 * 3) We need something like SvgAttrsBuilder to cover svg specific attrs.
 * https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute
 *
 * 4) It's not always clear where to set a property. Sometimes it's possible to do by setting attr() and style {}.
 * Also, not all style properties work (like color: red; doesn't work Attr `fill: red;` need to be used instead)
 */
class SvgElementScope : ElementScope<SVGElement> by ElementScopeImpl() {

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/circle
     */
    @Composable
    fun Circle(
        // TODO: add Length & Percentage type and use it instead of Number https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length
        cx: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        cy: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        r: Number, // Value type: <length> ; Default value: 0; Animatable: yes
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "circle",
            applyAttrs = {
                if (attrs != null) attrs()
                attr("cx", cx.toString())
                attr("cy", cy.toString())
                attr("r", r.toString())
            }
        )
    }

    /**
     * https://developer.mozilla.org/en/docs/Web/SVG/Element/ellipse
     */
    @Composable
    fun Ellipse(
        cx: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        cy: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        rx: Number, // Value type: auto|<length>|<percentage> ; Default value: auto; Animatable: yes
        ry: Number, // Value type: auto|<length>|<percentage> ; Default value: auto; Animatable: yes
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "ellipse",
            applyAttrs = {
                if (attrs != null) attrs()
                attr("cx", cx.toString())
                attr("cy", cy.toString())
                attr("rx", rx.toString())
                attr("ry", ry.toString())
            }
        )
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/line
     */
    @Composable
    fun Line(
        x1: Number, // Value type: <length>|<percentage>|<number> ; Default value: 0; Animatable: yes
        x2: Number, //Value type: <length>|<percentage>|<number> ; Default value: 0; Animatable: yes
        y1: Number, // Value type: <length>|<percentage>|<number> ; Default value: 0; Animatable: yes
        y2: Number, // Value type: <length>|<percentage>|<number> ; Default value: 0; Animatable: yes
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "line",
            applyAttrs = {
                if (attrs != null) attrs()
                attr("x1", x1.toString())
                attr("x2", x2.toString())
                attr("y1", y1.toString())
                attr("y2", y2.toString())
            }
        )
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/polygon
     */
    @Composable
    fun Polygon(
        points: List<Pair<Number, Number>>,
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "polygon",
            applyAttrs = {
                if (attrs != null) attrs()
                attr(
                    attr = "points",
                    value = points.joinToString(separator = " ") {
                        "${it.first},${it.second}"
                    }
                )
            }
        )
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/polyline
     */
    @Composable
    fun Polyline(
        points: List<Pair<Number, Number>>,
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "polyline",
            applyAttrs = {
                if (attrs != null) attrs()
                attr(
                    attr = "points",
                    value = points.joinToString(separator = " ") {
                        "${it.first},${it.second}"
                    }
                )
            }
        )
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/rect
     */
    @Composable
    fun Rect(
        x: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        y: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        width: Number, // Value type: auto|<length>|<percentage> ; Default value: auto; Animatable: yes
        height: Number, // Value type: auto|<length>|<percentage> ; Default value: auto; Animatable: yes
        rx: Number? = null, // Value type: auto|<length>|<percentage> ; Default value: auto; Animatable: yes
        ry: Number? = null, // Value type: auto|<length>|<percentage> ; Default value: auto; Animatable: yes
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "rect",
            applyAttrs = {
                if (attrs != null) attrs()
                attr("x", x.toString())
                attr("y", y.toString())
                attr("width", width.toString())
                attr("height", height.toString())
                if (rx != null) attr("rx", rx.toString())
                if (ry != null) attr("ry", ry.toString())
            }
        )
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/a
     */
    @Composable
    fun A(
        href: String? = null,
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null,
        content: @Composable (SvgElementScope.() -> Unit)? = null
    ) {
        ComposeSvgElement(
            name = "a",
            applyAttrs = {
                if (attrs != null) attrs()
                attr("href", href ?: "")
            },
            content = content
        )
    }

    class SvgViewBox(val minX: Number, val minY: Number, val width: Number, val height: Number) {
        override fun toString(): String {
            return "$minX $minY $width $height"
        }
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element/text
     */
    @Composable
    fun Text(
        text: String,
        x: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        y: Number, // Value type: <length>|<percentage> ; Default value: 0; Animatable: yes
        attrs: (AttrsBuilder<SVGElement>.() -> Unit)? = null,
    ) {
        ComposeSvgElement(
            name = "text",
            applyAttrs = {
                if (attrs != null) attrs()
                attr("x", x.toString())
                attr("y", y.toString())
                prop(setSvgInnerText, text)
            },
            content = null
        )
    }

    private val setSvgInnerText: (SVGElement, String) -> Unit = { e, v ->
        e.innerHTML = v
    }
}

