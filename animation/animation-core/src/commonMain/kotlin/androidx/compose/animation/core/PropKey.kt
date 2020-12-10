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

package androidx.compose.animation.core

/**
 * [TwoWayConverter] class contains the definition on how to convert from an arbitrary type [T]
 * to a [AnimationVector], and convert the [AnimationVector] back to the type [T]. This allows
 * animations to run on any type of objects, e.g. position, rectangle, color, etc.
 */
interface TwoWayConverter<T, V : AnimationVector> {
    /**
     * Defines how a type [T] should be converted to a Vector type (i.e. [AnimationVector1D],
     * [AnimationVector2D], [AnimationVector3D] or [AnimationVector4D], depends on the dimensions of
     * type T).
     */
    val convertToVector: (T) -> V
    /**
     * Defines how to convert a Vector type (i.e. [AnimationVector1D], [AnimationVector2D],
     * [AnimationVector3D] or [AnimationVector4D], depends on the dimensions of type T) back to type
     * [T].
     */
    val convertFromVector: (V) -> T
}

/**
 * Factory method to create a [TwoWayConverter] that converts a type [T] from and to an
 * [AnimationVector] type.
 *
 * @param convertToVector converts from type [T] to [AnimationVector]
 * @param convertFromVector converts from [AnimationVector] to type [T]
 */
fun <T, V : AnimationVector> TwoWayConverter(
    convertToVector: (T) -> V,
    convertFromVector: (V) -> T
): TwoWayConverter<T, V> = TwoWayConverterImpl(convertToVector, convertFromVector)

/**
 * Type converter to convert type [T] to and from a [AnimationVector1D].
 */
private class TwoWayConverterImpl<T, V : AnimationVector>(
    override val convertToVector: (T) -> V,
    override val convertFromVector: (V) -> T
) : TwoWayConverter<T, V>

/**
 * Property key of [T] type.
 *
 * [PropKey]s are used when defining a [TransitionDefinition], as a part of creating a
 * state-based animation.  Each property (to be animated) needs to be associated with a [PropKey] of
 * the type of the property. For example, this creates an alpha property:
 * `val alpha = FloatPropKey()`. [PropKey]s for common data types are
 * provided out of the box: such as [androidx.compose.animation.DpPropKey],
 * etc. To create a custom [PropKey] of type [T], a
 * [typeConverter] needs to be supplied to define how the animated data type can be converted to
 * and from an [AnimationVector], so that animations can support multi-dimensional data types.
 */
interface PropKey<T, V : AnimationVector> {
    /**
     * A [TwoWayConverter] that converts [T] (the data type to be animated) to and from [V] (any
     * subclass of [AnimationVector]).
     */
    val typeConverter: TwoWayConverter<T, V>

    /**
     * A label for distinguishing different prop keys in Tools (i.e. Android Studio).
     */
    val label: String
        get() = "PropKey"
}

internal fun lerp(start: Float, stop: Float, fraction: Float) =
    (start * (1 - fraction) + stop * fraction)

internal fun lerp(start: Int, stop: Int, fraction: Float) =
    (start * (1 - fraction) + stop * fraction).toInt()

/**
 * Built-in property key for [Float] properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
class FloatPropKey(
    override val label: String = "FloatPropKey"
) : PropKey<Float, AnimationVector1D> {
    override val typeConverter = Float.VectorConverter
}

/**
 * Built-in property key for [Int] properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
class IntPropKey(override val label: String = "IntPropKey") : PropKey<Int, AnimationVector1D> {
    override val typeConverter = Int.VectorConverter
}

/**
 * A [TwoWayConverter] that converts [Float] from and to [AnimationVector1D]
 */
val Float.Companion.VectorConverter: TwoWayConverter<Float, AnimationVector1D>
    get() = FloatToVector

/**
 * A [TwoWayConverter] that converts [Int] from and to [AnimationVector1D]
 */
val Int.Companion.VectorConverter: TwoWayConverter<Int, AnimationVector1D>
    get() = IntToVector

private val FloatToVector: TwoWayConverter<Float, AnimationVector1D> =
    TwoWayConverter({ AnimationVector1D(it) }, { it.value })

private val IntToVector: TwoWayConverter<Int, AnimationVector1D> =
    TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() })
