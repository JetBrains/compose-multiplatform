/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

private const val SvgNamespace = "http://www.w3.org/2000/svg"
private const val MathMlNamespace = "http://www.w3.org/1998/Math/MathML"

// These are DOM local names: parser-adjusted SVG names remain case-sensitive.
private val SvgHtmlIntegrationPointNames = setOf("desc", "foreignObject", "title")
private val MathMlTextIntegrationPointExceptions = setOf("mglyph", "malignmark")
private val MathMlTextIntegrationPointNames = setOf("mi", "mo", "mn", "ms", "mtext")
private val SvgForeignContentBreakoutTagNames = setOf(
    "b", "big", "blockquote", "body", "br", "center", "code", "dd", "div", "dl", "dt",
    "em", "embed", "h1", "h2", "h3", "h4", "h5", "h6", "head", "hr", "i", "img",
    "li", "listing", "menu", "meta", "nobr", "ol", "p", "pre", "ruby", "s", "small",
    "span", "strong", "strike", "sub", "sup", "table", "tt", "u", "ul", "var",
)
private val SvgFontBreakoutAttributeNames = setOf("color", "face", "size")

/**
 * Rejects names that the HTML tokenizer would collapse before SVG/MathML case adjustments.
 */
internal fun requireDistinctHtmlParserAttributeNames(attributeNames: Iterable<String>) {
    val sourceNamesByParserName = mutableMapOf<String, String>()
    attributeNames.forEach { sourceName ->
        val previousSourceName = sourceNamesByParserName.put(sourceName.asciiLowercase(), sourceName)
        require(previousSourceName == null) {
            "Duplicate HTML attribute names \"$previousSourceName\" and \"$sourceName\""
        }
    }
}

/**
 * Namespace declarations in the in-memory tree are not present in HTML syntax. Validates the
 * namespace that an HTML parser will infer from the parent and tag before serialization.
 */
internal fun requireHtmlParserStableChild(
    parentTagName: String?,
    parentNamespace: String?,
    childTagName: String,
    childNamespace: String,
    childAttributeNames: Iterable<String>,
) {
    requireHtmlParserStableNames(childTagName, childNamespace, childAttributeNames)
    if (parentNamespace == SvgNamespace && parentTagName !in SvgHtmlIntegrationPointNames) {
        requireStableSvgForeignContentChild(
            parentTagName = requireNotNull(parentTagName),
            childTagName = childTagName,
            childNamespace = childNamespace,
            childAttributeNames = childAttributeNames,
        )
        return
    }

    val parserNamespace = when (parentNamespace) {
        null, HtmlNamespace, SvgNamespace -> parserNamespaceInHtmlContext(childTagName)
        MathMlNamespace -> when {
            parentTagName in MathMlTextIntegrationPointNames &&
                childTagName.asciiLowercase() !in MathMlTextIntegrationPointExceptions ->
                parserNamespaceInHtmlContext(childTagName)
            // An SVG root is allowed here independently of annotation-xml's encoding.
            parentTagName == "annotation-xml" && childTagName.asciiLowercase() == "svg" ->
                SvgNamespace
            else -> MathMlNamespace
        }
        else -> parentNamespace
    }
    if (childNamespace == parserNamespace) return

    val location = parentTagName?.let { "inside <$it>" } ?: "at the string-rendering root"
    val suggestion = if (childNamespace == SvgNamespace) {
        "; wrap SVG fragments in an SVG <svg> root"
    } else {
        ""
    }
    throw IllegalArgumentException(
        "Element <$childTagName> in namespace \"$childNamespace\" cannot be serialized " +
            "$location; the HTML parser creates it in namespace \"$parserNamespace\"" +
            suggestion,
    )
}

private fun requireStableSvgForeignContentChild(
    parentTagName: String,
    childTagName: String,
    childNamespace: String,
    childAttributeNames: Iterable<String>,
) {
    if (childNamespace == HtmlNamespace) {
        if (childTagName == "style" || childTagName == "script") {
            throw IllegalArgumentException(
                "HTML <$childTagName> cannot be serialized directly inside SVG <$parentTagName>; " +
                    "the HTML parser creates an SVG <$childTagName> here. " +
                    "Use SvgElement<SVGElement>(\"$childTagName\") { Text(content) } for SVG content",
            )
        }
        throw IllegalArgumentException(
            "HTML element <$childTagName> cannot be serialized directly inside " +
                "SVG <$parentTagName>; wrap HTML content in an SVG <foreignObject>",
        )
    }
    if (exitsSvgForeignContent(childTagName, childAttributeNames)) {
        throw IllegalArgumentException(
            "Element <$childTagName> cannot be serialized directly inside SVG <$parentTagName>; " +
                "the HTML parser moves it out of SVG foreign content",
        )
    }
    if (childNamespace == SvgNamespace) return

    throw IllegalArgumentException(
        "Element <$childTagName> in namespace \"$childNamespace\" cannot be serialized " +
            "directly inside SVG <$parentTagName>; wrap HTML content in an SVG <foreignObject>",
    )
}

private fun parserNamespaceInHtmlContext(tagName: String): String =
    when (tagName.asciiLowercase()) {
        "svg" -> SvgNamespace
        "math" -> MathMlNamespace
        else -> HtmlNamespace
    }

private fun exitsSvgForeignContent(
    tagName: String,
    attributeNames: Iterable<String>,
): Boolean {
    val parserTagName = tagName.asciiLowercase()
    return parserTagName in SvgForeignContentBreakoutTagNames ||
        parserTagName == "font" && attributeNames.any { attributeName ->
            attributeName.asciiLowercase() in SvgFontBreakoutAttributeNames
        }
}

// https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-inforeign
private val SvgAdjustedTagNames = listOf(
    "altGlyph", "altGlyphDef", "altGlyphItem", "animateColor", "animateMotion", "animateTransform",
    "clipPath", "feBlend", "feColorMatrix", "feComponentTransfer", "feComposite", "feConvolveMatrix",
    "feDiffuseLighting", "feDisplacementMap", "feDistantLight", "feDropShadow", "feFlood",
    "feFuncA", "feFuncB", "feFuncG", "feFuncR", "feGaussianBlur", "feImage", "feMerge", "feMergeNode",
    "feMorphology", "feOffset", "fePointLight", "feSpecularLighting", "feSpotLight", "feTile",
    "feTurbulence", "foreignObject", "glyphRef", "linearGradient", "radialGradient", "textPath",
).associateBy(String::asciiLowercase)

// https://html.spec.whatwg.org/multipage/parsing.html#adjust-svg-attributes
private val SvgAdjustedAttributeNames = listOf(
    "attributeName", "attributeType", "baseFrequency", "baseProfile", "calcMode", "clipPathUnits",
    "diffuseConstant", "edgeMode", "filterUnits", "glyphRef", "gradientTransform", "gradientUnits",
    "kernelMatrix", "kernelUnitLength", "keyPoints", "keySplines", "keyTimes", "lengthAdjust",
    "limitingConeAngle", "markerHeight", "markerUnits", "markerWidth", "maskContentUnits", "maskUnits",
    "numOctaves", "pathLength", "patternContentUnits", "patternTransform", "patternUnits",
    "pointsAtX", "pointsAtY", "pointsAtZ", "preserveAlpha", "preserveAspectRatio", "primitiveUnits",
    "refX", "refY", "repeatCount", "repeatDur", "requiredExtensions", "requiredFeatures",
    "specularConstant", "specularExponent", "spreadMethod", "startOffset", "stdDeviation",
    "stitchTiles", "surfaceScale", "systemLanguage", "tableValues", "targetX", "targetY", "textLength",
    "viewBox", "viewTarget", "xChannelSelector", "yChannelSelector", "zoomAndPan",
).associateBy(String::asciiLowercase)

/**
 * Rejects SVG and MathML tag or attribute names whose casing would change when parsed as HTML.
 */
private fun requireHtmlParserStableNames(
    tagName: String,
    namespace: String,
    attributeNames: Iterable<String>,
) {
    if (namespace != SvgNamespace && namespace != MathMlNamespace) return
    val lowerTagName = tagName.asciiLowercase()
    val parserTagName = if (namespace == SvgNamespace) {
        SvgAdjustedTagNames[lowerTagName] ?: lowerTagName
    } else {
        lowerTagName
    }
    require(tagName == parserTagName) {
        "Element <$tagName> cannot be serialized without changing its name; " +
            "the HTML parser creates <$parserTagName>. Use <$parserTagName> instead"
    }
    attributeNames.forEach { name ->
        val lowerName = name.asciiLowercase()
        val parserName = when (namespace) {
            SvgNamespace -> SvgAdjustedAttributeNames[lowerName] ?: lowerName
            MathMlNamespace -> if (lowerName == "definitionurl") "definitionURL" else lowerName
            else -> lowerName
        }
        require(name == parserName) {
            "Attribute \"$name\" on <$tagName> cannot be serialized without changing its name; " +
                "the HTML parser creates \"$parserName\". Use \"$parserName\" instead"
        }
    }
}
