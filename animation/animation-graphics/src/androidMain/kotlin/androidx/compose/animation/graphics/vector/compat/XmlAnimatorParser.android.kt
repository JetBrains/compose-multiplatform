/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.animation.graphics.vector.compat

import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.PathInterpolator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.graphics.res.AccelerateDecelerateEasing
import androidx.compose.animation.graphics.res.AccelerateEasing
import androidx.compose.animation.graphics.res.AnticipateEasing
import androidx.compose.animation.graphics.res.AnticipateOvershootEasing
import androidx.compose.animation.graphics.res.BounceEasing
import androidx.compose.animation.graphics.res.CycleEasing
import androidx.compose.animation.graphics.res.DecelerateEasing
import androidx.compose.animation.graphics.res.OvershootEasing
import androidx.compose.animation.graphics.res.loadInterpolatorResource
import androidx.compose.animation.graphics.res.toEasing
import androidx.compose.animation.graphics.vector.Animator
import androidx.compose.animation.graphics.vector.AnimatorSet
import androidx.compose.animation.graphics.vector.Keyframe
import androidx.compose.animation.graphics.vector.ObjectAnimator
import androidx.compose.animation.graphics.vector.Ordering
import androidx.compose.animation.graphics.vector.PropertyValuesHolder
import androidx.compose.animation.graphics.vector.PropertyValuesHolder1D
import androidx.compose.animation.graphics.vector.PropertyValuesHolder2D
import androidx.compose.animation.graphics.vector.PropertyValuesHolderColor
import androidx.compose.animation.graphics.vector.PropertyValuesHolderFloat
import androidx.compose.animation.graphics.vector.PropertyValuesHolderInt
import androidx.compose.animation.graphics.vector.PropertyValuesHolderPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser

internal const val TagSet = "set"
internal const val TagObjectAnimator = "objectAnimator"
private const val TagPropertyValuesHolder = "propertyValuesHolder"
private const val TagKeyframe = "keyframe"

private const val ValueTypeFloat = 0
private const val ValueTypeInt = 1
private const val ValueTypePath = 2
private const val ValueTypeColor = 3
private const val ValueTypeUndefined = 4

private const val RepeatModeReverse = 2

private enum class ValueType {
    Float,
    Int,
    Color,
    Path
}

private val FallbackValueType = ValueType.Float

private fun TypedArray.getInterpolator(
    res: Resources,
    theme: Resources.Theme?,
    index: Int,
    defaultValue: Easing
): Easing {
    val id = getResourceId(index, 0)
    return if (id == 0) {
        defaultValue
    } else {
        loadInterpolatorResource(theme, res, id)
    }
}

private fun parseKeyframe(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    holderValueType: ValueType?,
    defaultInterpolator: Easing
): Pair<Keyframe<Any>, ValueType> {
    return attrs.attrs(res, theme, AndroidVectorResources.STYLEABLE_KEYFRAME) { a ->
        val inferredValueType =
            // The type is specified in <propertyValuesHolder>.
            holderValueType
                ?: inferValueType( // Identify the type from our attribute values.
                    a.getInt(
                        AndroidVectorResources.STYLEABLE_KEYFRAME_VALUE_TYPE,
                        ValueTypeUndefined
                    ),
                    a.peekValue(AndroidVectorResources.STYLEABLE_KEYFRAME_VALUE).type
                )
                // We didn't have any clue until the end.
                ?: FallbackValueType
        a.getKeyframe(
            a.getFloat(AndroidVectorResources.STYLEABLE_KEYFRAME_FRACTION, 0f),
            a.getInterpolator(
                res,
                theme,
                AndroidVectorResources.STYLEABLE_KEYFRAME_INTERPOLATOR,
                defaultInterpolator
            ),
            inferredValueType,
            AndroidVectorResources.STYLEABLE_KEYFRAME_VALUE
        ) to inferredValueType // Report back the type to <propertyValuesHolder>.
    }
}

/**
 * Extracts a [Keyframe] value from this [TypedArray]. This [TypedArray] can come from either
 * `<propertyValuesHolder>` or `<keyframe>`
 */
private fun TypedArray.getKeyframe(
    fraction: Float,
    interpolator: Easing,
    valueType: ValueType,
    valueIndex: Int
): Keyframe<Any> {
    return when (valueType) {
        ValueType.Float -> Keyframe(
            fraction,
            getFloat(valueIndex, 0f),
            interpolator
        )
        ValueType.Int -> Keyframe(
            fraction,
            getInt(valueIndex, 0),
            interpolator
        )
        ValueType.Color -> Keyframe(
            fraction,
            Color(getColor(valueIndex, 0)),
            interpolator
        )
        ValueType.Path -> Keyframe(
            fraction,
            addPathNodes(getString(valueIndex)),
            interpolator
        )
    }
}

private fun XmlPullParser.parsePropertyValuesHolder(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    interpolator: Easing
): PropertyValuesHolder<*> {
    return attrs.attrs(
        res,
        theme,
        AndroidVectorResources.STYLEABLE_PROPERTY_VALUES_HOLDER
    ) { a ->
        a.getPropertyValuesHolder1D(
            a.getString(
                AndroidVectorResources.STYLEABLE_PROPERTY_VALUES_HOLDER_PROPERTY_NAME
            )!!,
            AndroidVectorResources.STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TYPE,
            AndroidVectorResources.STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_FROM,
            AndroidVectorResources.STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TO,
            interpolator
        ) { valueType, keyframes ->
            var vt: ValueType? = null
            forEachChildOf(TagPropertyValuesHolder) {
                if (eventType == XmlPullParser.START_TAG && name == TagKeyframe) {
                    val (keyframe, keyframeValueType) =
                        parseKeyframe(res, theme, attrs, valueType, interpolator)
                    if (vt == null) vt = keyframeValueType
                    keyframes.add(keyframe)
                }
            }
            // This determines the final ValueType of the PropertyValuesHolder.
            vt ?: valueType ?: FallbackValueType
        }
    }
}

/**
 * Infers a [ValueType] from various information from XML.
 *
 * @param valueType The `valueType` attribute specified in the XML.
 * @param typedValueTypes [TypedValue.type] values taken from multiple [TypedValue]s.
 * @return A [ValueType] identified by the information so far, or `null` if it is uncertain.
 */
private fun inferValueType(valueType: Int, vararg typedValueTypes: Int): ValueType? {
    return when (valueType) {
        ValueTypeFloat -> ValueType.Float
        ValueTypeInt -> ValueType.Int
        ValueTypeColor -> ValueType.Color
        ValueTypePath -> ValueType.Path
        else ->
            if (
                typedValueTypes
                    .all {
                        it in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT
                    }
            ) {
                ValueType.Color
            } else {
                null
            }
    }
}

/**
 * Extracts attribute values related to [PropertyValuesHolder]. This [TypedArray] can be taken from
 * either `<objectAnimator>` or `<propertyValuesHolder>`.
 *
 * @param parseKeyframes The caller should parse `<keyframe>`s inside of this
 * `<propertyValuesHolder>` and store them in the `keyframes` [MutableList]. The lambda receives
 * a [ValueType] if it has been identified so far. The lambda has to return [ValueType] in case it
 * is first identified while parsing keyframes.
 */
private fun TypedArray.getPropertyValuesHolder1D(
    propertyName: String,
    valueTypeIndex: Int,
    valueFromIndex: Int,
    valueToIndex: Int,
    interpolator: Easing,
    parseKeyframes: (
        valueType: ValueType?,
        keyframes: MutableList<Keyframe<Any>>
    ) -> ValueType = { vt, _ -> vt ?: FallbackValueType }
): PropertyValuesHolder1D<*> {
    val valueType = getInt(
        valueTypeIndex,
        ValueTypeUndefined
    )

    val valueFrom = peekValue(valueFromIndex)
    val hasFrom = valueFrom != null
    val typeFrom = valueFrom?.type ?: ValueTypeUndefined

    val valueTo = peekValue(valueToIndex)
    val hasTo = valueTo != null
    val typeTo = valueTo?.type ?: ValueTypeUndefined

    var inferredValueType =
        inferValueType(
            valueType,
            typeFrom,
            typeTo
        )
    val keyframes = mutableListOf<Keyframe<Any>>()
    if (inferredValueType == null && (hasFrom || hasTo)) {
        inferredValueType =
            ValueType.Float
    }
    if (hasFrom) {
        keyframes.add(getKeyframe(0f, interpolator, inferredValueType!!, valueFromIndex))
    }
    if (hasTo) {
        keyframes.add(getKeyframe(1f, interpolator, inferredValueType!!, valueToIndex))
    }
    inferredValueType = parseKeyframes(inferredValueType, keyframes)
    keyframes.sortBy { it.fraction }
    @Suppress("UNCHECKED_CAST")
    return when (inferredValueType) {
        ValueType.Float -> PropertyValuesHolderFloat(
            propertyName,
            keyframes as List<Keyframe<Float>>
        )
        ValueType.Int -> PropertyValuesHolderInt(
            propertyName,
            keyframes as List<Keyframe<Int>>
        )
        ValueType.Color -> PropertyValuesHolderColor(
            propertyName,
            keyframes as List<Keyframe<Color>>
        )
        ValueType.Path -> PropertyValuesHolderPath(
            propertyName,
            keyframes as List<Keyframe<List<PathNode>>>
        )
    }
}

private fun convertRepeatMode(repeatMode: Int) = when (repeatMode) {
    RepeatModeReverse -> RepeatMode.Reverse
    else -> RepeatMode.Restart
}

internal fun XmlPullParser.parseObjectAnimator(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): ObjectAnimator {
    return attrs.attrs(res, theme, AndroidVectorResources.STYLEABLE_ANIMATOR) { a ->
        attrs.attrs(res, theme, AndroidVectorResources.STYLEABLE_PROPERTY_ANIMATOR) { oa ->
            val interpolator = a.getInterpolator(
                res,
                theme,
                AndroidVectorResources.STYLEABLE_ANIMATOR_INTERPOLATOR,
                AccelerateDecelerateEasing
            )
            val holders = mutableListOf<PropertyValuesHolder<*>>()
            val pathData = oa.getString(
                AndroidVectorResources.STYLEABLE_PROPERTY_ANIMATOR_PATH_DATA
            )
            if (pathData != null) {
                // 2D; This <objectAnimator> has `pathData`. It should also have `propertyXName`
                // and `propertyYName`.
                holders.add(
                    PropertyValuesHolder2D(
                        oa.getString(
                            AndroidVectorResources.STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_X_NAME
                        )!!,
                        oa.getString(
                            AndroidVectorResources.STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_Y_NAME
                        )!!,
                        addPathNodes(pathData),
                        interpolator
                    )
                )
            } else {
                // 1D; This <objectAnimator> has `propertyName`, `valueFrom`, and `valueTo`.
                oa.getString(
                    AndroidVectorResources.STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_NAME
                )?.let { propertyName ->
                    holders.add(
                        a.getPropertyValuesHolder1D(
                            propertyName,
                            AndroidVectorResources.STYLEABLE_ANIMATOR_VALUE_TYPE,
                            AndroidVectorResources.STYLEABLE_ANIMATOR_VALUE_FROM,
                            AndroidVectorResources.STYLEABLE_ANIMATOR_VALUE_TO,
                            interpolator
                        )
                    )
                }
                // This <objectAnimator> has <propertyValuesHolder> inside.
                forEachChildOf(TagObjectAnimator) {
                    if (eventType == XmlPullParser.START_TAG && name == TagPropertyValuesHolder) {
                        holders.add(parsePropertyValuesHolder(res, theme, attrs, interpolator))
                    }
                }
            }

            ObjectAnimator(
                duration = a.getInt(
                    AndroidVectorResources.STYLEABLE_ANIMATOR_DURATION,
                    300
                ),
                startDelay = a.getInt(
                    AndroidVectorResources.STYLEABLE_ANIMATOR_START_OFFSET,
                    0
                ),
                repeatCount = a.getInt(
                    AndroidVectorResources.STYLEABLE_ANIMATOR_REPEAT_COUNT,
                    0
                ),
                repeatMode = convertRepeatMode(
                    a.getInt(AndroidVectorResources.STYLEABLE_ANIMATOR_REPEAT_MODE, 0)
                ),
                holders = holders
            )
        }
    }
}

internal fun XmlPullParser.parseAnimatorSet(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): AnimatorSet {
    return attrs.attrs(res, theme, AndroidVectorResources.STYLEABLE_ANIMATOR_SET) { a ->
        val ordering = a.getInt(AndroidVectorResources.STYLEABLE_ANIMATOR_SET_ORDERING, 0)
        val animators = mutableListOf<Animator>()
        forEachChildOf(TagSet) {
            if (eventType == XmlPullParser.START_TAG) {
                when (name) {
                    TagSet -> animators.add(parseAnimatorSet(res, theme, attrs))
                    TagObjectAnimator -> animators.add(parseObjectAnimator(res, theme, attrs))
                }
            }
        }
        AnimatorSet(
            animators,
            if (ordering != 0) Ordering.Sequentially else Ordering.Together
        )
    }
}

internal fun XmlPullParser.parseInterpolator(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): Easing {
    return when (name) {
        "linearInterpolator" -> LinearEasing
        "accelerateInterpolator" ->
            attrs.attrs(
                res, theme, AndroidVectorResources.STYLEABLE_ACCELERATE_INTERPOLATOR
            ) { a ->
                val factor = a.getFloat(
                    AndroidVectorResources.STYLEABLE_ACCELERATE_INTERPOLATOR_FACTOR, 1.0f
                )
                if (factor == 1.0f) AccelerateEasing else AccelerateEasing(factor)
            }
        "decelerateInterpolator" ->
            attrs.attrs(
                res, theme, AndroidVectorResources.STYLEABLE_DECELERATE_INTERPOLATOR
            ) { a ->
                val factor = a.getFloat(
                    AndroidVectorResources.STYLEABLE_DECELERATE_INTERPOLATOR_FACTOR, 1.0f
                )
                if (factor == 1.0f) DecelerateEasing else DecelerateEasing(factor)
            }
        "accelerateDecelerateInterpolator" -> AccelerateDecelerateEasing
        "cycleInterpolator" ->
            attrs.attrs(
                res, theme, AndroidVectorResources.STYLEABLE_CYCLE_INTERPOLATOR
            ) { a ->
                CycleEasing(
                    a.getFloat(
                        AndroidVectorResources.STYLEABLE_CYCLE_INTERPOLATOR_CYCLES, 1.0f
                    )
                )
            }
        "anticipateInterpolator" ->
            attrs.attrs(
                res,
                theme,
                AndroidVectorResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR
            ) { a ->
                AnticipateEasing(
                    a.getFloat(
                        AndroidVectorResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_TENSION,
                        2.0f
                    )
                )
            }
        "overshootInterpolator" ->
            attrs.attrs(
                res, theme, AndroidVectorResources.STYLEABLE_OVERSHOOT_INTERPOLATOR
            ) { a ->
                OvershootEasing(
                    a.getFloat(
                        AndroidVectorResources.STYLEABLE_OVERSHOOT_INTERPOLATOR_TENSION,
                        2.0f
                    )
                )
            }
        "anticipateOvershootInterpolator" ->
            attrs.attrs(
                res,
                theme,
                AndroidVectorResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR
            ) { a ->
                AnticipateOvershootEasing(
                    a.getFloat(
                        AndroidVectorResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_TENSION,
                        2.0f
                    ),
                    a.getFloat(
                        AndroidVectorResources
                            .STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_EXTRA_TENSION,
                        1.5f
                    )
                )
            }
        "bounceInterpolator" -> BounceEasing
        "pathInterpolator" ->
            attrs.attrs(
                res, theme, AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR
            ) { a ->
                val pathData =
                    a.getString(AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_PATH_DATA)
                if (pathData != null) {
                    PathInterpolator(PathParser.createPathFromPathData(pathData)).toEasing()
                } else if (
                    !a.hasValue(AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_2) ||
                    !a.hasValue(AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_2)
                ) {
                    PathInterpolator(
                        a.getFloat(
                            AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_1,
                            0f
                        ),
                        a.getFloat(
                            AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_1,
                            0f
                        )
                    ).toEasing()
                } else {
                    CubicBezierEasing(
                        a.getFloat(
                            AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_1,
                            0f
                        ),
                        a.getFloat(
                            AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_1,
                            0f
                        ),
                        a.getFloat(
                            AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_2,
                            1f
                        ),
                        a.getFloat(
                            AndroidVectorResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_2,
                            1f
                        )
                    )
                }
            }
        else -> throw RuntimeException("Unknown interpolator: $name")
    }
}
