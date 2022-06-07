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

package androidx.compose.animation.graphics.vector

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.util.lerp

internal const val RepeatCountInfinite = -1

internal sealed class Animator {
    abstract val totalDuration: Int

    @Composable
    fun createVectorConfig(
        transition: Transition<Boolean>,
        overallDuration: Int
    ): VectorConfig {
        return remember { StateVectorConfig() }.also { config ->
            Configure(transition, config, overallDuration)
        }
    }

    @Composable
    fun Configure(
        transition: Transition<Boolean>,
        config: StateVectorConfig,
        overallDuration: Int
    ) {
        val propertyValuesMap = remember(overallDuration) {
            mutableMapOf<String, PropertyValues<*>>().also {
                collectPropertyValues(it, overallDuration, 0)
            }
        }
        for ((propertyName, values) in propertyValuesMap) {
            values.timestamps.sortBy { it.timeMillis }
            val state = values.createState(transition, propertyName, overallDuration)
            @Suppress("UNCHECKED_CAST")
            when (propertyName) {
                "rotation" -> config.rotationState = state as State<Float>
                "pivotX" -> config.pivotXState = state as State<Float>
                "pivotY" -> config.pivotYState = state as State<Float>
                "scaleX" -> config.scaleXState = state as State<Float>
                "scaleY" -> config.scaleYState = state as State<Float>
                "translateX" -> config.translateXState = state as State<Float>
                "translateY" -> config.translateYState = state as State<Float>
                "fillAlpha" -> config.fillAlphaState = state as State<Float>
                "strokeWidth" -> config.strokeWidthState = state as State<Float>
                "strokeAlpha" -> config.strokeAlphaState = state as State<Float>
                "trimPathStart" -> config.trimPathStartState = state as State<Float>
                "trimPathEnd" -> config.trimPathEndState = state as State<Float>
                "trimPathOffset" -> config.trimPathOffsetState = state as State<Float>
                "fillColor" -> config.fillColorState = state as State<Color>
                "strokeColor" -> config.strokeColorState = state as State<Color>
                "pathData" -> config.pathDataState = state as State<List<PathNode>>
                else -> throw IllegalStateException("Unknown propertyName: $propertyName")
            }
        }
    }

    abstract fun collectPropertyValues(
        propertyValuesMap: MutableMap<String, PropertyValues<*>>,
        overallDuration: Int,
        parentDelay: Int
    )
}

internal class Timestamp<T>(
    val timeMillis: Int,
    val durationMillis: Int,
    val repeatCount: Int,
    val repeatMode: RepeatMode,
    val holder: PropertyValuesHolder<T>
) {
    fun asAnimationSpec(): FiniteAnimationSpec<T> {
        @Suppress("UNCHECKED_CAST")
        val spec = when (holder) {
            is PropertyValuesHolderFloat -> holder.asKeyframeSpec(durationMillis)
            is PropertyValuesHolderColor -> holder.asKeyframeSpec(durationMillis)
            else -> throw RuntimeException("Unexpected value type: $holder")
        } as KeyframesSpec<T>
        return if (repeatCount == 0) {
            spec
        } else {
            repeatable(
                iterations = if (repeatCount == RepeatCountInfinite) {
                    Int.MAX_VALUE
                } else {
                    repeatCount + 1
                },
                animation = spec,
                repeatMode = repeatMode
            )
        }
    }
}

internal sealed class PropertyValues<T> {

    val timestamps = mutableListOf<Timestamp<T>>()

    @Composable
    abstract fun createState(
        transition: Transition<Boolean>,
        propertyName: String,
        overallDuration: Int
    ): State<T>

    protected fun createAnimationSpec(
        overallDuration: Int
    ): @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<T> {
        return {
            @Suppress("UNCHECKED_CAST")
            val spec = combined(timestamps.map { timestamp ->
                timestamp.timeMillis to timestamp.asAnimationSpec()
            })
            if (targetState) spec else spec.reversed(overallDuration)
        }
    }
}

private class FloatPropertyValues : PropertyValues<Float>() {

    @Composable
    override fun createState(
        transition: Transition<Boolean>,
        propertyName: String,
        overallDuration: Int
    ): State<Float> {
        return transition.animateFloat(
            transitionSpec = createAnimationSpec(overallDuration),
            label = propertyName,
            targetValueByState = { atEnd ->
                if (atEnd) {
                    (timestamps.last().holder as PropertyValuesHolderFloat)
                        .animatorKeyframes.last().value
                } else {
                    (timestamps.first().holder as PropertyValuesHolderFloat)
                        .animatorKeyframes.first().value
                }
            }
        )
    }
}

private class ColorPropertyValues : PropertyValues<Color>() {

    @Composable
    override fun createState(
        transition: Transition<Boolean>,
        propertyName: String,
        overallDuration: Int
    ): State<Color> {
        return transition.animateColor(
            transitionSpec = createAnimationSpec(overallDuration),
            label = propertyName,
            targetValueByState = { atEnd ->
                if (atEnd) {
                    (timestamps.last().holder as PropertyValuesHolderColor)
                        .animatorKeyframes.last().value
                } else {
                    (timestamps.first().holder as PropertyValuesHolderColor)
                        .animatorKeyframes.first().value
                }
            }
        )
    }
}

private class PathPropertyValues : PropertyValues<List<PathNode>>() {

    @Composable
    override fun createState(
        transition: Transition<Boolean>,
        propertyName: String,
        overallDuration: Int
    ): State<List<PathNode>> {
        val timeState = transition.animateFloat(
            transitionSpec = {
                val spec = tween<Float>(durationMillis = overallDuration, easing = LinearEasing)
                if (targetState) spec else spec.reversed(overallDuration)
            },
            label = propertyName
        ) { atEnd ->
            if (atEnd) overallDuration.toFloat() else 0f
        }
        return derivedStateOf { interpolate(timeState.value) }
    }

    private fun interpolate(timeMillis: Float): List<PathNode> {
        val timestamp = timestamps.lastOrNull { it.timeMillis <= timeMillis } ?: timestamps.first()
        var fraction = (timeMillis - timestamp.timeMillis) / timestamp.durationMillis
        if (timestamp.repeatCount != 0) {
            var count = 0
            while (fraction > 1f) {
                fraction -= 1f
                count++
            }
            if (timestamp.repeatMode == RepeatMode.Reverse && count % 2 != 0) {
                fraction = 1f - fraction
            }
        }
        return (timestamp.holder as PropertyValuesHolderPath).interpolate(fraction)
    }
}

internal data class ObjectAnimator(
    val duration: Int,
    val startDelay: Int,
    val repeatCount: Int,
    val repeatMode: RepeatMode,
    val holders: List<PropertyValuesHolder<*>>
) : Animator() {

    override val totalDuration = if (repeatCount == RepeatCountInfinite) {
        Int.MAX_VALUE
    } else {
        startDelay + duration * (repeatCount + 1)
    }

    override fun collectPropertyValues(
        propertyValuesMap: MutableMap<String, PropertyValues<*>>,
        overallDuration: Int,
        parentDelay: Int
    ) {
        holders.fastForEach { holder ->
            when (holder) {
                is PropertyValuesHolder2D -> {
                    // TODO(b/178978971): Implement path animation
                }
                is PropertyValuesHolderFloat -> {
                    val values =
                        propertyValuesMap[holder.propertyName] as FloatPropertyValues?
                            ?: FloatPropertyValues()
                    values.timestamps.add(
                        Timestamp(
                            parentDelay + startDelay,
                            duration,
                            repeatCount,
                            repeatMode,
                            holder
                        )
                    )
                    propertyValuesMap[holder.propertyName] = values
                }
                is PropertyValuesHolderColor -> {
                    val values =
                        propertyValuesMap[holder.propertyName] as ColorPropertyValues?
                            ?: ColorPropertyValues()
                    values.timestamps.add(
                        Timestamp(
                            parentDelay + startDelay,
                            duration,
                            repeatCount,
                            repeatMode,
                            holder
                        )
                    )
                    propertyValuesMap[holder.propertyName] = values
                }
                is PropertyValuesHolderPath -> {
                    val values =
                        propertyValuesMap[holder.propertyName] as PathPropertyValues?
                            ?: PathPropertyValues()
                    values.timestamps.add(
                        Timestamp(
                            parentDelay + startDelay,
                            duration,
                            repeatCount,
                            repeatMode,
                            holder
                        )
                    )
                    propertyValuesMap[holder.propertyName] = values
                }
                is PropertyValuesHolderInt -> {
                    // Not implemented since AVD does not use any Int property.
                }
            }
        }
    }
}

internal data class AnimatorSet(
    val animators: List<Animator>,
    val ordering: Ordering
) : Animator() {

    override val totalDuration = when (ordering) {
        Ordering.Together -> animators.fastMaxBy { it.totalDuration }?.totalDuration ?: 0
        Ordering.Sequentially -> animators.fastSumBy { it.totalDuration }
    }

    override fun collectPropertyValues(
        propertyValuesMap: MutableMap<String, PropertyValues<*>>,
        overallDuration: Int,
        parentDelay: Int
    ) {
        when (ordering) {
            Ordering.Together -> {
                animators.fastForEach { animator ->
                    animator.collectPropertyValues(
                        propertyValuesMap,
                        overallDuration,
                        parentDelay
                    )
                }
            }
            Ordering.Sequentially -> {
                var accumulatedDelay = parentDelay
                animators.fastForEach { animator ->
                    animator.collectPropertyValues(
                        propertyValuesMap,
                        overallDuration,
                        accumulatedDelay
                    )
                    accumulatedDelay += animator.totalDuration
                }
            }
        }
    }
}

internal sealed class PropertyValuesHolder<T>

internal data class PropertyValuesHolder2D(
    val xPropertyName: String,
    val yPropertyName: String,
    val pathData: List<PathNode>,
    val interpolator: Easing
) : PropertyValuesHolder<Pair<Float, Float>>()

internal sealed class PropertyValuesHolder1D<T>(
    val propertyName: String
) : PropertyValuesHolder<T>() {

    abstract val animatorKeyframes: List<Keyframe<T>>
}

internal class PropertyValuesHolderFloat(
    propertyName: String,
    override val animatorKeyframes: List<Keyframe<Float>>
) : PropertyValuesHolder1D<Float>(propertyName) {

    fun asKeyframeSpec(duration: Int): KeyframesSpec<Float> {
        return keyframes {
            durationMillis = duration
            for (keyframe in animatorKeyframes) {
                keyframe.value at (duration * keyframe.fraction).toInt() with keyframe.interpolator
            }
        }
    }
}

internal class PropertyValuesHolderInt(
    propertyName: String,
    override val animatorKeyframes: List<Keyframe<Int>>
) : PropertyValuesHolder1D<Int>(propertyName)

internal class PropertyValuesHolderColor(
    propertyName: String,
    override val animatorKeyframes: List<Keyframe<Color>>
) : PropertyValuesHolder1D<Color>(propertyName) {

    fun asKeyframeSpec(duration: Int): KeyframesSpec<Color> {
        return keyframes {
            durationMillis = duration
            for (keyframe in animatorKeyframes) {
                keyframe.value at (duration * keyframe.fraction).toInt() with keyframe.interpolator
            }
        }
    }
}

internal class PropertyValuesHolderPath(
    propertyName: String,
    override val animatorKeyframes: List<Keyframe<List<PathNode>>>
) : PropertyValuesHolder1D<List<PathNode>>(propertyName) {

    fun interpolate(fraction: Float): List<PathNode> {
        val index = (animatorKeyframes.indexOfFirst { it.fraction >= fraction } - 1)
            .coerceAtLeast(0)
        val easing = animatorKeyframes[index + 1].interpolator
        val innerFraction = easing.transform(
            ((fraction - animatorKeyframes[index].fraction) /
                (animatorKeyframes[index + 1].fraction - animatorKeyframes[index].fraction))
                .coerceIn(0f, 1f)
        )
        return lerp(
            animatorKeyframes[index].value,
            animatorKeyframes[index + 1].value,
            innerFraction
        )
    }
}

internal data class Keyframe<T>(
    val fraction: Float,
    val value: T,
    val interpolator: Easing
)

internal enum class Ordering {
    Together,
    Sequentially
}

internal class StateVectorConfig : VectorConfig {

    var rotationState: State<Float>? = null
    var pivotXState: State<Float>? = null
    var pivotYState: State<Float>? = null
    var scaleXState: State<Float>? = null
    var scaleYState: State<Float>? = null
    var translateXState: State<Float>? = null
    var translateYState: State<Float>? = null
    var pathDataState: State<List<PathNode>>? = null
    var fillColorState: State<Color>? = null
    var strokeColorState: State<Color>? = null
    var strokeWidthState: State<Float>? = null
    var strokeAlphaState: State<Float>? = null
    var fillAlphaState: State<Float>? = null
    var trimPathStartState: State<Float>? = null
    var trimPathEndState: State<Float>? = null
    var trimPathOffsetState: State<Float>? = null

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(property: VectorProperty<T>, defaultValue: T): T {
        return when (property) {
            is VectorProperty.Rotation -> rotationState?.value ?: defaultValue
            is VectorProperty.PivotX -> pivotXState?.value ?: defaultValue
            is VectorProperty.PivotY -> pivotYState?.value ?: defaultValue
            is VectorProperty.ScaleX -> scaleXState?.value ?: defaultValue
            is VectorProperty.ScaleY -> scaleYState?.value ?: defaultValue
            is VectorProperty.TranslateX -> translateXState?.value ?: defaultValue
            is VectorProperty.TranslateY -> translateYState?.value ?: defaultValue
            is VectorProperty.PathData -> pathDataState?.value ?: defaultValue
            is VectorProperty.Fill -> fillColorState?.let { state ->
                SolidColor(state.value)
            } ?: defaultValue
            is VectorProperty.FillAlpha -> fillAlphaState?.value ?: defaultValue
            is VectorProperty.Stroke -> strokeColorState?.let { state ->
                SolidColor(state.value)
            } ?: defaultValue
            is VectorProperty.StrokeLineWidth -> strokeWidthState?.value ?: defaultValue
            is VectorProperty.StrokeAlpha -> strokeAlphaState?.value ?: defaultValue
            is VectorProperty.TrimPathStart -> trimPathStartState?.value ?: defaultValue
            is VectorProperty.TrimPathEnd -> trimPathEndState?.value ?: defaultValue
            is VectorProperty.TrimPathOffset -> trimPathOffsetState?.value ?: defaultValue
        } as T
    }
}

private fun lerp(start: List<PathNode>, stop: List<PathNode>, fraction: Float): List<PathNode> {
    return start.zip(stop) { a, b -> lerp(a, b, fraction) }
}

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
private fun lerp(start: PathNode, stop: PathNode, fraction: Float): PathNode {
    return when (start) {
        is PathNode.RelativeMoveTo -> {
            require(stop is PathNode.RelativeMoveTo)
            PathNode.RelativeMoveTo(
                lerp(start.dx, stop.dx, fraction),
                lerp(start.dy, stop.dy, fraction)
            )
        }
        is PathNode.MoveTo -> {
            require(stop is PathNode.MoveTo)
            PathNode.MoveTo(
                lerp(start.x, stop.x, fraction),
                lerp(start.y, stop.y, fraction)
            )
        }
        is PathNode.RelativeLineTo -> {
            require(stop is PathNode.RelativeLineTo)
            PathNode.RelativeLineTo(
                lerp(start.dx, stop.dx, fraction),
                lerp(start.dy, stop.dy, fraction)
            )
        }
        is PathNode.LineTo -> {
            require(stop is PathNode.LineTo)
            PathNode.LineTo(
                lerp(start.x, stop.x, fraction),
                lerp(start.y, stop.y, fraction)
            )
        }
        is PathNode.RelativeHorizontalTo -> {
            require(stop is PathNode.RelativeHorizontalTo)
            PathNode.RelativeHorizontalTo(
                lerp(start.dx, stop.dx, fraction)
            )
        }
        is PathNode.HorizontalTo -> {
            require(stop is PathNode.HorizontalTo)
            PathNode.HorizontalTo(
                lerp(start.x, stop.x, fraction)
            )
        }
        is PathNode.RelativeVerticalTo -> {
            require(stop is PathNode.RelativeVerticalTo)
            PathNode.RelativeVerticalTo(
                lerp(start.dy, stop.dy, fraction)
            )
        }
        is PathNode.VerticalTo -> {
            require(stop is PathNode.VerticalTo)
            PathNode.VerticalTo(
                lerp(start.y, stop.y, fraction)
            )
        }
        is PathNode.RelativeCurveTo -> {
            require(stop is PathNode.RelativeCurveTo)
            PathNode.RelativeCurveTo(
                lerp(start.dx1, stop.dx1, fraction),
                lerp(start.dy1, stop.dy1, fraction),
                lerp(start.dx2, stop.dx2, fraction),
                lerp(start.dy2, stop.dy2, fraction),
                lerp(start.dx3, stop.dx3, fraction),
                lerp(start.dy3, stop.dy3, fraction)
            )
        }
        is PathNode.CurveTo -> {
            require(stop is PathNode.CurveTo)
            PathNode.CurveTo(
                lerp(start.x1, stop.x1, fraction),
                lerp(start.y1, stop.y1, fraction),
                lerp(start.x2, stop.x2, fraction),
                lerp(start.y2, stop.y2, fraction),
                lerp(start.x3, stop.x3, fraction),
                lerp(start.y3, stop.y3, fraction)
            )
        }
        is PathNode.RelativeReflectiveCurveTo -> {
            require(stop is PathNode.RelativeReflectiveCurveTo)
            PathNode.RelativeReflectiveCurveTo(
                lerp(start.dx1, stop.dx1, fraction),
                lerp(start.dy1, stop.dy1, fraction),
                lerp(start.dx2, stop.dx2, fraction),
                lerp(start.dy2, stop.dy2, fraction)
            )
        }
        is PathNode.ReflectiveCurveTo -> {
            require(stop is PathNode.ReflectiveCurveTo)
            PathNode.ReflectiveCurveTo(
                lerp(start.x1, stop.x1, fraction),
                lerp(start.y1, stop.y1, fraction),
                lerp(start.x2, stop.x2, fraction),
                lerp(start.y2, stop.y2, fraction)
            )
        }
        is PathNode.RelativeQuadTo -> {
            require(stop is PathNode.RelativeQuadTo)
            PathNode.RelativeQuadTo(
                lerp(start.dx1, stop.dx1, fraction),
                lerp(start.dy1, stop.dy1, fraction),
                lerp(start.dx2, stop.dx2, fraction),
                lerp(start.dy2, stop.dy2, fraction)
            )
        }
        is PathNode.QuadTo -> {
            require(stop is PathNode.QuadTo)
            PathNode.QuadTo(
                lerp(start.x1, stop.x1, fraction),
                lerp(start.y1, stop.y1, fraction),
                lerp(start.x2, stop.x2, fraction),
                lerp(start.y2, stop.y2, fraction)
            )
        }
        is PathNode.RelativeReflectiveQuadTo -> {
            require(stop is PathNode.RelativeReflectiveQuadTo)
            PathNode.RelativeReflectiveQuadTo(
                lerp(start.dx, stop.dx, fraction),
                lerp(start.dy, stop.dy, fraction)
            )
        }
        is PathNode.ReflectiveQuadTo -> {
            require(stop is PathNode.ReflectiveQuadTo)
            PathNode.ReflectiveQuadTo(
                lerp(start.x, stop.x, fraction),
                lerp(start.y, stop.y, fraction)
            )
        }
        is PathNode.RelativeArcTo -> {
            require(stop is PathNode.RelativeArcTo)
            PathNode.RelativeArcTo(
                lerp(start.horizontalEllipseRadius, stop.horizontalEllipseRadius, fraction),
                lerp(start.verticalEllipseRadius, stop.verticalEllipseRadius, fraction),
                lerp(start.theta, stop.theta, fraction),
                start.isMoreThanHalf,
                start.isPositiveArc,
                lerp(start.arcStartDx, stop.arcStartDx, fraction),
                lerp(start.arcStartDy, stop.arcStartDy, fraction)
            )
        }
        is PathNode.ArcTo -> {
            require(stop is PathNode.ArcTo)
            PathNode.ArcTo(
                lerp(start.horizontalEllipseRadius, stop.horizontalEllipseRadius, fraction),
                lerp(start.verticalEllipseRadius, stop.verticalEllipseRadius, fraction),
                lerp(start.theta, stop.theta, fraction),
                start.isMoreThanHalf,
                start.isPositiveArc,
                lerp(start.arcStartX, stop.arcStartX, fraction),
                lerp(start.arcStartY, stop.arcStartY, fraction)
            )
        }
        PathNode.Close -> PathNode.Close
    }
}
