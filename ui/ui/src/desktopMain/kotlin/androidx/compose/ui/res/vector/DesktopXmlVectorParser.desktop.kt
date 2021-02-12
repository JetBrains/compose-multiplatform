/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.res.vector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.DefaultPivotX
import androidx.compose.ui.graphics.vector.DefaultPivotY
import androidx.compose.ui.graphics.vector.DefaultRotation
import androidx.compose.ui.graphics.vector.DefaultScaleX
import androidx.compose.ui.graphics.vector.DefaultScaleY
import androidx.compose.ui.graphics.vector.DefaultTranslationX
import androidx.compose.ui.graphics.vector.DefaultTranslationY
import androidx.compose.ui.graphics.vector.EmptyPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.vector.BuildContext.Group
import androidx.compose.ui.unit.Density
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.util.LinkedList

//  Parsing logic is the same as in Android implementation
//  (compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/graphics/vector/compat/XmlVectorParser.kt)
//
//  Except there is no support for linking with external resources
//  (for example, we can't reference to color defined in another file)
//
//  Specification:
//  https://developer.android.com/reference/android/graphics/drawable/VectorDrawable

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val AAPT_NS = "http://schemas.android.com/aapt"

private class BuildContext {
    val currentGroups = LinkedList<Group>()

    enum class Group {
        /**
         * Group that exists in xml file
         */
        Real,

        /**
         * Group that doesn't exist in xml file. We add it manually when we see <clip-path> node.
         * It will be automatically popped when the real group will be popped.
         */
        Virtual
    }
}

internal fun Element.parseVectorRoot(density: Density): ImageVector {
    val context = BuildContext()
    val builder = ImageVector.Builder(
        defaultWidth = attributeOrNull(ANDROID_NS, "width").parseDp(density),
        defaultHeight = attributeOrNull(ANDROID_NS, "height").parseDp(density),
        viewportWidth = attributeOrNull(ANDROID_NS, "viewportWidth")?.toFloat() ?: 0f,
        viewportHeight = attributeOrNull(ANDROID_NS, "viewportHeight")?.toFloat() ?: 0f
    )
    parseVectorNodes(builder, context)
    return builder.build()
}

private fun Element.parseVectorNodes(builder: ImageVector.Builder, context: BuildContext) {
    childrenSequence
        .filterIsInstance<Element>()
        .forEach {
            it.parseVectorNode(builder, context)
        }
}

private fun Element.parseVectorNode(builder: ImageVector.Builder, context: BuildContext) {
    when (nodeName) {
        "path" -> parsePath(builder)
        "clip-path" -> parseClipPath(builder, context)
        "group" -> parseGroup(builder, context)
    }
}

private fun Element.parsePath(builder: ImageVector.Builder) {
    builder.addPath(
        pathData = addPathNodes(attributeOrNull(ANDROID_NS, "pathData")),
        pathFillType = attributeOrNull(ANDROID_NS, "fillType")
            ?.let(::parseFillType) ?: PathFillType.NonZero,
        name = attributeOrNull(ANDROID_NS, "name") ?: "",
        fill = attributeOrNull(ANDROID_NS, "fillColor")?.let(::parseStringBrush)
            ?: apptAttr(ANDROID_NS, "fillColor")?.let(Element::parseElementBrush),
        fillAlpha = attributeOrNull(ANDROID_NS, "fillAlpha")?.toFloat() ?: 1.0f,
        stroke = attributeOrNull(ANDROID_NS, "strokeColor")?.let(::parseStringBrush)
            ?: apptAttr(ANDROID_NS, "strokeColor")?.let(Element::parseElementBrush),
        strokeAlpha = attributeOrNull(ANDROID_NS, "strokeAlpha")?.toFloat() ?: 1.0f,
        strokeLineWidth = attributeOrNull(ANDROID_NS, "strokeWidth")?.toFloat() ?: 1.0f,
        strokeLineCap = attributeOrNull(ANDROID_NS, "strokeLineCap")
            ?.let(::parseStrokeCap) ?: StrokeCap.Butt,
        strokeLineJoin = attributeOrNull(ANDROID_NS, "strokeLineJoin")
            ?.let(::parseStrokeJoin) ?: StrokeJoin.Miter,
        strokeLineMiter = attributeOrNull(ANDROID_NS, "strokeMiterLimit")?.toFloat() ?: 1.0f,
        trimPathStart = attributeOrNull(ANDROID_NS, "trimPathStart")?.toFloat() ?: 0.0f,
        trimPathEnd = attributeOrNull(ANDROID_NS, "trimPathEnd")?.toFloat() ?: 1.0f,
        trimPathOffset = attributeOrNull(ANDROID_NS, "trimPathOffset")?.toFloat() ?: 0.0f
    )
}

private fun Element.parseClipPath(builder: ImageVector.Builder, context: BuildContext) {
    builder.addGroup(
        name = attributeOrNull(ANDROID_NS, "name") ?: "",
        clipPathData = addPathNodes(attributeOrNull(ANDROID_NS, "pathData"))
    )
    context.currentGroups.addLast(Group.Virtual)
}

private fun Element.parseGroup(builder: ImageVector.Builder, context: BuildContext) {
    builder.addGroup(
        attributeOrNull(ANDROID_NS, "name") ?: "",
        attributeOrNull(ANDROID_NS, "rotation")?.toFloat() ?: DefaultRotation,
        attributeOrNull(ANDROID_NS, "pivotX")?.toFloat() ?: DefaultPivotX,
        attributeOrNull(ANDROID_NS, "pivotY")?.toFloat() ?: DefaultPivotY,
        attributeOrNull(ANDROID_NS, "scaleX")?.toFloat() ?: DefaultScaleX,
        attributeOrNull(ANDROID_NS, "scaleY")?.toFloat() ?: DefaultScaleY,
        attributeOrNull(ANDROID_NS, "translateX")?.toFloat() ?: DefaultTranslationX,
        attributeOrNull(ANDROID_NS, "translateY")?.toFloat() ?: DefaultTranslationY,
        EmptyPath
    )
    context.currentGroups.addLast(Group.Real)

    parseVectorNodes(builder, context)

    do {
        val removedGroup = context.currentGroups.removeLastOrNull()
        builder.clearGroup()
    } while (removedGroup == Group.Virtual)
}

private fun parseStringBrush(str: String) = SolidColor(Color(parseColorValue(str)))

private fun Element.parseElementBrush(): Brush? =
    childrenSequence
        .filterIsInstance<Element>()
        .find { it.nodeName == "gradient" }
        ?.parseGradient()

private fun Element.parseGradient(): Brush? {
    return when (attributeOrNull(ANDROID_NS, "type")) {
        "linear" -> parseLinearGradient()
        "radial" -> parseRadialGradient()
        "sweep" -> parseSweepGradient()
        else -> null
    }
}

@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
private fun Element.parseLinearGradient() = Brush.linearGradient(
    colorStops = parseColorStops(),
    start = Offset(
        attributeOrNull(ANDROID_NS, "startX")?.toFloat() ?: 0f,
        attributeOrNull(ANDROID_NS, "startY")?.toFloat() ?: 0f
    ),
    end = Offset(
        attributeOrNull(ANDROID_NS, "endX")?.toFloat() ?: 0f,
        attributeOrNull(ANDROID_NS, "endY")?.toFloat() ?: 0f
    ),
    tileMode = attributeOrNull(ANDROID_NS, "tileMode")?.let(::parseTileMode) ?: TileMode.Clamp
)

@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
private fun Element.parseRadialGradient() = Brush.radialGradient(
    colorStops = parseColorStops(),
    center = Offset(
        attributeOrNull(ANDROID_NS, "centerX")?.toFloat() ?: 0f,
        attributeOrNull(ANDROID_NS, "centerY")?.toFloat() ?: 0f
    ),
    radius = attributeOrNull(ANDROID_NS, "gradientRadius")?.toFloat() ?: 0f,
    tileMode = attributeOrNull(ANDROID_NS, "tileMode")?.let(::parseTileMode) ?: TileMode.Clamp
)

@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
private fun Element.parseSweepGradient() = Brush.sweepGradient(
    colorStops = parseColorStops(),
    center = Offset(
        attributeOrNull(ANDROID_NS, "centerX")?.toFloat() ?: 0f,
        attributeOrNull(ANDROID_NS, "centerY")?.toFloat() ?: 0f,
    )
)

private fun Element.parseColorStops(): Array<Pair<Float, Color>> {
    val items = childrenSequence
        .filterIsInstance<Element>()
        .filter { it.nodeName == "item" }
        .toList()

    val colorStops = items.mapIndexedNotNullTo(mutableListOf()) { index, item ->
        item.parseColorStop(defaultOffset = index.toFloat() / items.lastIndex.coerceAtLeast(1))
    }

    if (colorStops.isEmpty()) {
        val startColor = attributeOrNull(ANDROID_NS, "startColor")?.let(::parseColorValue)
        val centerColor = attributeOrNull(ANDROID_NS, "centerColor")?.let(::parseColorValue)
        val endColor = attributeOrNull(ANDROID_NS, "endColor")?.let(::parseColorValue)

        if (startColor != null) {
            colorStops.add(0f to Color(startColor))
        }
        if (centerColor != null) {
            colorStops.add(0.5f to Color(centerColor))
        }
        if (endColor != null) {
            colorStops.add(1f to Color(endColor))
        }
    }

    return colorStops.toTypedArray()
}

private fun Element.parseColorStop(defaultOffset: Float): Pair<Float, Color>? {
    val offset = attributeOrNull(ANDROID_NS, "offset")?.toFloat() ?: defaultOffset
    val color = attributeOrNull(ANDROID_NS, "color")?.let(::parseColorValue) ?: return null
    return offset to Color(color)
}

private fun Element.attributeOrNull(namespace: String, name: String): String? {
    val value = getAttributeNS(namespace, name)
    return if (value.isNotBlank()) value else null
}

/**
 * Attribute of an element can be represented as a separate child:
 *
 *  <path ...>
 *    <aapt:attr name="android:fillColor">
 *      <gradient ...
 *        ...
 *      </gradient>
 *    </aapt:attr>
 *  </path>
 *
 * instead of:
 *
 *  <path android:fillColor="red" ... />
 */
private fun Element.apptAttr(
    namespace: String,
    name: String
): Element? {
    val prefix = lookupPrefix(namespace) ?: return null
    return childrenSequence
        .filterIsInstance<Element>()
        .find {
            it.namespaceURI == AAPT_NS && it.localName == "attr" &&
                it.getAttribute("name") == "$prefix:$name"
        }
}

private val Element.childrenSequence get() = sequence<Node> {
    for (i in 0 until childNodes.length) {
        yield(childNodes.item(i))
    }
}