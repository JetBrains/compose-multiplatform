/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics.vector.compat

import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultPivotX
import androidx.compose.ui.graphics.vector.DefaultPivotY
import androidx.compose.ui.graphics.vector.DefaultRotation
import androidx.compose.ui.graphics.vector.DefaultScaleX
import androidx.compose.ui.graphics.vector.DefaultScaleY
import androidx.compose.ui.graphics.vector.DefaultTranslationX
import androidx.compose.ui.graphics.vector.DefaultTranslationY
import androidx.compose.ui.graphics.vector.EmptyPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ComplexColorCompat
import androidx.core.content.res.TypedArrayUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

private const val LINECAP_BUTT = 0
private const val LINECAP_ROUND = 1
private const val LINECAP_SQUARE = 2

private const val LINEJOIN_MITER = 0
private const val LINEJOIN_ROUND = 1
private const val LINEJOIN_BEVEL = 2

private val FILL_TYPE_WINDING = 0

private const val SHAPE_CLIP_PATH = "clip-path"
private const val SHAPE_GROUP = "group"
private const val SHAPE_PATH = "path"

private fun getStrokeLineCap(id: Int, defValue: StrokeCap = StrokeCap.Butt): StrokeCap =
    when (id) {
        LINECAP_BUTT -> StrokeCap.Butt
        LINECAP_ROUND -> StrokeCap.Round
        LINECAP_SQUARE -> StrokeCap.Square
        else -> defValue
    }

private fun getStrokeLineJoin(id: Int, defValue: StrokeJoin = StrokeJoin.Miter): StrokeJoin =
    when (id) {
        LINEJOIN_MITER -> StrokeJoin.Miter
        LINEJOIN_ROUND -> StrokeJoin.Round
        LINEJOIN_BEVEL -> StrokeJoin.Bevel
        else -> defValue
    }

internal fun XmlPullParser.isAtEnd(): Boolean =
    eventType == XmlPullParser.END_DOCUMENT ||
        (depth < 1 && eventType == XmlPullParser.END_TAG)

/**
 * @param nestedGroups The number of additionally nested VectorGroups to represent clip paths.
 * @return The number of nested VectorGroups that are not `<group>` in XML, but represented as
 * VectorGroup in the [builder]. These are also popped when this function sees `</group>`.
 */
internal fun XmlPullParser.parseCurrentVectorNode(
    res: Resources,
    attrs: AttributeSet,
    theme: Resources.Theme? = null,
    builder: ImageVector.Builder,
    nestedGroups: Int
): Int {
    when (eventType) {
        XmlPullParser.START_TAG -> {
            when (name) {
                SHAPE_PATH -> {
                    parsePath(res, theme, attrs, builder)
                }
                SHAPE_CLIP_PATH -> {
                    parseClipPath(res, theme, attrs, builder)
                    return nestedGroups + 1
                }
                SHAPE_GROUP -> {
                    parseGroup(res, theme, attrs, builder)
                }
            }
        }
        XmlPullParser.END_TAG -> {
            if (SHAPE_GROUP == name) {
                repeat(nestedGroups + 1) {
                    builder.clearGroup()
                }
                return 0
            }
        }
    }
    return nestedGroups
}

/**
 * Helper method to seek to the first tag within the VectorDrawable xml asset
 */
@Throws(XmlPullParserException::class)
internal fun XmlPullParser.seekToStartTag(): XmlPullParser {
    var type = next()
    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        // Empty loop
        type = next()
    }
    if (type != XmlPullParser.START_TAG) {
        throw XmlPullParserException("No start tag found")
    }
    return this
}

@SuppressWarnings("RestrictedApi")
internal fun XmlPullParser.createVectorImageBuilder(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): ImageVector.Builder {
    val vectorAttrs = TypedArrayUtils.obtainAttributes(
        res,
        theme,
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY
    )

    // TODO (njawad) handle mirroring here
//        state.mAutoMirrored = TypedArrayUtils.getNamedBoolean(a, parser, "autoMirrored",
//                AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_AUTO_MIRRORED, state.mAutoMirrored)

    val viewportWidth = TypedArrayUtils.getNamedFloat(
        vectorAttrs,
        this,
        "viewportWidth",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_WIDTH,
        0.0f
    )

    val viewportHeight = TypedArrayUtils.getNamedFloat(
        vectorAttrs,
        this,
        "viewportHeight",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_HEIGHT,
        0.0f
    )

    if (viewportWidth <= 0) {
        throw XmlPullParserException(
            vectorAttrs.positionDescription + "<VectorGraphic> tag requires viewportWidth > 0"
        )
    } else if (viewportHeight <= 0) {
        throw XmlPullParserException(
            vectorAttrs.positionDescription + "<VectorGraphic> tag requires viewportHeight > 0"
        )
    }

    val defaultWidth = vectorAttrs.getDimension(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_WIDTH, 0.0f
    )
    val defaultHeight = vectorAttrs.getDimension(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_HEIGHT, 0.0f
    )

    val tintColor = if (
        vectorAttrs.hasValue(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT)
    ) {
        val value = TypedValue()
        vectorAttrs.getValue(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT, value)
        // Unable to parse theme attributes outside of the framework here.
        // This is a similar limitation to VectorDrawableCompat's parsing logic within
        // updateStateFromTypedArray as TypedArray#extractThemeAttrs is not a public API
        // ignore tint colors provided from the theme itself.
        if (value.type == TypedValue.TYPE_ATTRIBUTE) {
            Color.Unspecified
        } else {
            val tintColorStateList = TypedArrayUtils.getNamedColorStateList(
                vectorAttrs, this, theme, "tint",
                AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT
            )
            if (tintColorStateList != null) {
                Color(tintColorStateList.defaultColor)
            } else {
                Color.Unspecified
            }
        }
    } else {
        Color.Unspecified
    }

    val blendModeValue = vectorAttrs.getInt(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT_MODE, -1
    )
    val tintBlendMode = if (blendModeValue != -1) {
        when (blendModeValue) {
            3 -> BlendMode.SrcOver
            5 -> BlendMode.SrcIn
            9 -> BlendMode.SrcAtop
            // b/73224934 PorterDuff Multiply maps to Skia Modulate so actually
            // return BlendMode.MODULATE here
            14 -> BlendMode.Modulate
            15 -> BlendMode.Screen
            16 -> BlendMode.Plus
            else -> BlendMode.SrcIn
        }
    } else {
        BlendMode.SrcIn
    }

    val defaultWidthDp = (defaultWidth / res.displayMetrics.density).dp
    val defaultHeightDp = (defaultHeight / res.displayMetrics.density).dp

    vectorAttrs.recycle()

    return ImageVector.Builder(
        defaultWidth = defaultWidthDp,
        defaultHeight = defaultHeightDp,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        tintColor = tintColor,
        tintBlendMode = tintBlendMode
    )
}

@Throws(IllegalArgumentException::class)
@SuppressWarnings("RestrictedApi")
internal fun XmlPullParser.parsePath(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: ImageVector.Builder
) {
    val a = TypedArrayUtils.obtainAttributes(
        res,
        theme,
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH
    )

    val hasPathData = TypedArrayUtils.hasAttribute(this, "pathData")
    if (!hasPathData) {
        // If there is no pathData in the VPath tag, then this is an empty VPath,
        // nothing need to be drawn.
        throw IllegalArgumentException("No path data available")
    }

    val name: String = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_NAME) ?: ""

    val pathStr = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_PATH_DATA)

    val pathData: List<PathNode> = addPathNodes(pathStr)

    val fillColor = TypedArrayUtils.getNamedComplexColor(
        a,
        this,
        theme,
        "fillColor",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_COLOR, 0
    )
    val fillAlpha = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "fillAlpha",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_ALPHA, 1.0f
    )
    val lineCap = TypedArrayUtils.getNamedInt(
        a,
        this,
        "strokeLineCap",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_CAP, -1
    )
    val strokeLineCap = getStrokeLineCap(lineCap, StrokeCap.Butt)
    val lineJoin = TypedArrayUtils.getNamedInt(
        a,
        this,
        "strokeLineJoin",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_JOIN, -1
    )
    val strokeLineJoin =
        getStrokeLineJoin(lineJoin, StrokeJoin.Bevel)
    val strokeMiterLimit = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "strokeMiterLimit",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_MITER_LIMIT,
        1.0f
    )
    val strokeColor = TypedArrayUtils.getNamedComplexColor(
        a,
        this,
        theme,
        "strokeColor",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_COLOR, 0
    )
    val strokeAlpha = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "strokeAlpha",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_ALPHA, 1.0f
    )
    val strokeLineWidth = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "strokeWidth",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_WIDTH, 1.0f
    )

    val trimPathEnd = TypedArrayUtils.getNamedFloat(
        a, this, "trimPathEnd",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_END, 1.0f
    )
    val trimPathOffset = TypedArrayUtils.getNamedFloat(
        a, this, "trimPathOffset",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_OFFSET,
        0.0f
    )
    val trimPathStart = TypedArrayUtils.getNamedFloat(
        a, this, "trimPathStart",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_START,
        0.0f
    )

    val fillRule = TypedArrayUtils.getNamedInt(
        a, this, "fillType",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_FILLTYPE,
        FILL_TYPE_WINDING
    )

    a.recycle()

    val fillBrush = obtainBrushFromComplexColor(fillColor)
    val strokeBrush = obtainBrushFromComplexColor(strokeColor)
    val fillPathType = if (fillRule == 0) PathFillType.NonZero else PathFillType.EvenOdd

    builder.addPath(
        pathData,
        fillPathType,
        name,
        fillBrush,
        fillAlpha,
        strokeBrush,
        strokeAlpha,
        strokeLineWidth,
        strokeLineCap,
        strokeLineJoin,
        strokeMiterLimit,
        trimPathStart,
        trimPathEnd,
        trimPathOffset
    )
}

@SuppressWarnings("RestrictedApi")
private fun obtainBrushFromComplexColor(complexColor: ComplexColorCompat): Brush? =
    if (complexColor.willDraw()) {
        val shader = complexColor.shader
        if (shader != null) {
            ShaderBrush(shader)
        } else {
            SolidColor(Color(complexColor.color))
        }
    } else {
        null
    }

internal fun XmlPullParser.parseClipPath(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: ImageVector.Builder
) {
    val a = theme?.obtainStyledAttributes(
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH,
        0,
        0
    ) ?: res.obtainAttributes(attrs, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH)

    val name: String = a.getString(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_NAME
    ) ?: ""
    val pathData = addPathNodes(
        a.getString(
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_PATH_DATA
        )
    )
    a.recycle()

    // <clip-path> is parsed out as an additional VectorGroup.
    // This allows us to replicate the behavior of VectorDrawable where <clip-path> only affects
    // <path> that comes after it in <group>.
    builder.addGroup(
        name = name,
        clipPathData = pathData
    )
}

@SuppressWarnings("RestrictedApi")
internal fun XmlPullParser.parseGroup(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: ImageVector.Builder
) {
    val a = TypedArrayUtils.obtainAttributes(
        res,
        theme,
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP
    )

    // Account for any configuration changes.
    // mChangingConfigurations |= Utils.getChangingConfigurations(a);

    // Extract the theme attributes, if any.
    // mThemeAttrs = null // TODO TINT THEME Not supported yet a.extractThemeAttrs();

    // This is added in API 11
    val rotate = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "rotation",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_ROTATION,
        DefaultRotation
    )

    val pivotX = a.getFloat(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_X,
        DefaultPivotX
    )
    val pivotY = a.getFloat(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_Y,
        DefaultPivotY
    )

    // This is added in API 11
    val scaleX = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "scaleX",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_X,
        DefaultScaleX
    )

    // This is added in API 11
    val scaleY = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "scaleY",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_Y,
        DefaultScaleY
    )

    val translateX = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "translateX",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_X,
        DefaultTranslationX
    )
    val translateY = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "translateY",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_Y,
        DefaultTranslationY
    )

    val name: String =
        a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_NAME) ?: ""

    a.recycle()

    builder.addGroup(
        name,
        rotate,
        pivotX,
        pivotY,
        scaleX,
        scaleY,
        translateX,
        translateY,
        EmptyPath
    )
}
