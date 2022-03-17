/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A representation of window insets that tracks access to enable recomposition,
 * relayout, and redrawing when values change. These values should not be read during composition
 * to avoid doing composition for every frame of an animation. Use methods like
 * [Modifier.windowInsetsPadding], [Modifier.systemBarsPadding], and
 * [Modifier.windowInsetsTopHeight] for Modifiers that will not cause recomposition when values
 * change.
 *
 * Use the [WindowInsets.Companion] extensions to retrieve [WindowInsets] for the current
 * window.
 */
@Stable
interface WindowInsets {
    /**
     * The space, in pixels, at the left of the window that the inset represents.
     */
    fun getLeft(density: Density, layoutDirection: LayoutDirection): Int

    /**
     * The space, in pixels, at the top of the window that the inset represents.
     */
    fun getTop(density: Density): Int

    /**
     * The space, in pixels, at the right of the window that the inset represents.
     */
    fun getRight(density: Density, layoutDirection: LayoutDirection): Int

    /**
     * The space, in pixels, at the bottom of the window that the inset represents.
     */
    fun getBottom(density: Density): Int

    companion object
}

/**
 * [WindowInsetsSides] is used in [WindowInsets.only] to define which sides of the
 * [WindowInsets] should apply.
 */
@kotlin.jvm.JvmInline
value class WindowInsetsSides private constructor(private val value: Int) {
    /**
     * Returns a [WindowInsetsSides] containing sides defied in [sides] and the
     * sides in `this`.
     */
    operator fun plus(sides: WindowInsetsSides): WindowInsetsSides =
        WindowInsetsSides(value or sides.value)

    internal fun hasAny(sides: WindowInsetsSides): Boolean =
        (value and sides.value) != 0

    override fun toString(): String = "WindowInsetsSides(${valueToString()})"

    private fun valueToString(): String = buildString {
        fun appendPlus(text: String) {
            if (isNotEmpty()) append('+')
            append(text)
        }

        if (value and Start.value == Start.value) appendPlus("Start")
        if (value and Left.value == Left.value) appendPlus("Left")
        if (value and Top.value == Top.value) appendPlus("Top")
        if (value and End.value == End.value) appendPlus("End")
        if (value and Right.value == Right.value) appendPlus("Right")
        if (value and Bottom.value == Bottom.value) appendPlus("Bottom")
    }

    companion object {
        //     _---- allowLeft  in ltr
        //    /
        //    | _--- allowRight in ltr
        //    |/
        //    || _-- allowLeft  in rtl
        //    ||/
        //    ||| _- allowRight in rtl
        //    |||/
        //    VVVV
        //    Mask   = ----
        //
        //    Left   = 1010
        //    Right  = 0101
        //    Start  = 1001
        //    End    = 0110

        internal val AllowLeftInLtr = WindowInsetsSides(1 shl 3)
        internal val AllowRightInLtr = WindowInsetsSides(1 shl 2)
        internal val AllowLeftInRtl = WindowInsetsSides(1 shl 1)
        internal val AllowRightInRtl = WindowInsetsSides(1 shl 0)

        /**
         * Indicates a [WindowInsets] start side, which is left or right
         * depending on [LayoutDirection]. If [LayoutDirection.Ltr], [Start]
         * is the left side. If [LayoutDirection.Rtl], [Start] is the right side.
         *
         * Use [Left] or [Right] if the physical direction is required.
         */
        val Start = AllowLeftInLtr + AllowRightInRtl

        /**
         * Indicates a [WindowInsets] end side, which is left or right
         * depending on [LayoutDirection]. If [LayoutDirection.Ltr], [End]
         * is the right side. If [LayoutDirection.Rtl], [End] is the left side.
         *
         * Use [Left] or [Right] if the physical direction is required.
         */
        val End = AllowRightInLtr + AllowLeftInRtl

        /**
         * Indicates a [WindowInsets] top side.
         */
        val Top = WindowInsetsSides(1 shl 4)

        /**
         * Indicates a [WindowInsets] bottom side.
         */
        val Bottom = WindowInsetsSides(1 shl 5)

        /**
         * Indicates a [WindowInsets] left side. Most layouts will prefer using
         * [Start] or [End] to account for [LayoutDirection].
         */
        val Left = AllowLeftInLtr + AllowLeftInRtl

        /**
         * Indicates a [WindowInsets] right side. Most layouts will prefer using
         * [Start] or [End] to account for [LayoutDirection].
         */
        val Right = AllowRightInLtr + AllowRightInRtl

        /**
         * Indicates a [WindowInsets] horizontal sides. This is a combination of
         * [Left] and [Right] sides, or [Start] and [End] sides.
         */
        val Horizontal = Left + Right

        /**
         * Indicates a [WindowInsets] [Top] and [Bottom] sides.
         */
        val Vertical = Top + Bottom
    }
}

/**
 * Returns a [WindowInsets] that has the maximum values of this [WindowInsets] and [insets].
 */
fun WindowInsets.union(insets: WindowInsets): WindowInsets = UnionInsets(this, insets)

/**
 * Returns the values in this [WindowInsets] that are not also in [insets]. For example, if this
 * [WindowInsets] has a [WindowInsets.getTop] value of `10` and [insets] has a
 * [WindowInsets.getTop] value of `8`, the returned [WindowInsets] will have a
 * [WindowInsets.getTop] value of `2`.
 *
 * Negative values are never returned. For example if [insets] has a [WindowInsets.getTop] of `10`
 * and this has a [WindowInsets.getTop] of `0`, the returned [WindowInsets] will have a
 * [WindowInsets.getTop] value of `0`.
 */
fun WindowInsets.exclude(insets: WindowInsets): WindowInsets = ExcludeInsets(this, insets)

/**
 * Returns a [WindowInsets] that has values of this, added to the values of [insets].
 * For example, if this has a top of 10 and insets has a top of 5, the returned [WindowInsets]
 * will have a top of 15.
 */
fun WindowInsets.add(insets: WindowInsets): WindowInsets = AddedInsets(this, insets)

/**
 * Returns a [WindowInsets] that eliminates all dimensions except the ones that are enabled.
 * For example, to have a [WindowInsets] at the bottom of the screen, pass
 * [WindowInsetsSides.Bottom].
 */
fun WindowInsets.only(sides: WindowInsetsSides): WindowInsets = LimitInsets(this, sides)

/**
 * Convert a [WindowInsets] to a [PaddingValues] and uses [LocalDensity] for DP to pixel
 * conversion. [PaddingValues] can be passed to some containers to pad internal content so that
 * it doesn't overlap the insets when fully scrolled. Ensure that the insets are
 * [consumed][consumedWindowInsets] after the padding is applied if insets are to be used further
 * down the hierarchy.
 *
 * @sample androidx.compose.foundation.layout.samples.paddingValuesSample
 */
@ReadOnlyComposable
@Composable
fun WindowInsets.asPaddingValues(): PaddingValues = InsetsPaddingValues(this, LocalDensity.current)

/**
 * Convert a [WindowInsets] to a [PaddingValues] and uses [density] for DP to pixel conversion.
 * [PaddingValues] can be passed to some containers to pad internal content so that it doesn't
 * overlap the insets when fully scrolled. Ensure that the insets are
 * [consumed][consumedWindowInsets] after the padding is applied if insets are to be used further
 * down the hierarchy.
 *
 * @sample androidx.compose.foundation.layout.samples.paddingValuesSample
 */
fun WindowInsets.asPaddingValues(density: Density): PaddingValues =
    InsetsPaddingValues(this, density)

/**
 * Convert a [PaddingValues] to a [WindowInsets].
 */
internal fun PaddingValues.asInsets(): WindowInsets = PaddingValuesInsets(this)

/**
 * Create a [WindowInsets] with fixed dimensions.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsInt
 */
fun WindowInsets(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): WindowInsets =
    FixedIntInsets(left, top, right, bottom)

/**
 * Create a [WindowInsets] with fixed dimensions, using [Dp] values.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsDp
 */
fun WindowInsets(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp
): WindowInsets = FixedDpInsets(left, top, right, bottom)

@Immutable
private class FixedIntInsets(
    private val leftVal: Int,
    private val topVal: Int,
    private val rightVal: Int,
    private val bottomVal: Int
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = leftVal
    override fun getTop(density: Density): Int = topVal
    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = rightVal
    override fun getBottom(density: Density): Int = bottomVal

    override fun toString(): String {
        return "Insets(left=$leftVal, top=$topVal, right=$rightVal, bottom=$bottomVal)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FixedIntInsets) {
            return false
        }

        return leftVal == other.leftVal && topVal == other.topVal &&
            rightVal == other.rightVal && bottomVal == other.bottomVal
    }

    override fun hashCode(): Int {
        var result = leftVal
        result = 31 * result + topVal
        result = 31 * result + rightVal
        result = 31 * result + bottomVal
        return result
    }
}

@Immutable
private class FixedDpInsets(
    private val leftDp: Dp,
    private val topDp: Dp,
    private val rightDp: Dp,
    private val bottomDp: Dp
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        with(density) { leftDp.roundToPx() }

    override fun getTop(density: Density) = with(density) { topDp.roundToPx() }
    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        with(density) { rightDp.roundToPx() }
    override fun getBottom(density: Density) = with(density) { bottomDp.roundToPx() }

    override fun toString(): String {
        return "Insets(left=$leftDp, top=$topDp, right=$rightDp, bottom=$bottomDp)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FixedDpInsets) {
            return false
        }

        return leftDp == other.leftDp && topDp == other.topDp &&
            rightDp == other.rightDp && bottomDp == other.bottomDp
    }

    override fun hashCode(): Int {
        var result = leftDp.hashCode()
        result = 31 * result + topDp.hashCode()
        result = 31 * result + rightDp.hashCode()
        result = 31 * result + bottomDp.hashCode()
        return result
    }
}

/**
 * An [WindowInsets] that comes straight from [androidx.core.graphics.Insets], whose value can
 * be updated.
 */
@Stable
internal class ValueInsets(insets: InsetsValues, val name: String) : WindowInsets {
    internal var value by mutableStateOf(insets)

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = value.left
    override fun getTop(density: Density) = value.top
    override fun getRight(density: Density, layoutDirection: LayoutDirection) = value.right
    override fun getBottom(density: Density) = value.bottom

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is ValueInsets) {
            return false
        }
        return value == other.value
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "$name(left=${value.left}, top=${value.top}, " +
            "right=${value.right}, bottom=${value.bottom})"
    }
}

@Immutable
internal class InsetsValues(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InsetsValues) {
            return false
        }

        return left == other.left &&
            top == other.top &&
            right == other.right &&
            bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return result
    }

    override fun toString(): String =
        "InsetsValues(left=$left, top=$top, right=$right, bottom=$bottom)"
}

/**
 * An [WindowInsets] that includes the maximum value of [first] and [second] as returned from
 * [WindowInsets.union].
 */
@Stable
private class UnionInsets(
    private val first: WindowInsets,
    private val second: WindowInsets
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        maxOf(first.getLeft(density, layoutDirection), second.getLeft(density, layoutDirection))

    override fun getTop(density: Density) =
        maxOf(first.getTop(density), second.getTop(density))

    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        maxOf(first.getRight(density, layoutDirection), second.getRight(density, layoutDirection))

    override fun getBottom(density: Density) =
        maxOf(first.getBottom(density), second.getBottom(density))

    override fun hashCode(): Int = first.hashCode() + second.hashCode() * 31

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is UnionInsets) {
            return false
        }
        return other.first == first && other.second == second
    }

    override fun toString(): String = "($first ∪ $second)"
}

/**
 * An [WindowInsets] that includes the added value of [first] to [second].
 */
@Stable
private class AddedInsets(
    private val first: WindowInsets,
    private val second: WindowInsets
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        first.getLeft(density, layoutDirection) + second.getLeft(density, layoutDirection)

    override fun getTop(density: Density) =
        first.getTop(density) + second.getTop(density)

    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        first.getRight(density, layoutDirection) + second.getRight(density, layoutDirection)

    override fun getBottom(density: Density) =
        first.getBottom(density) + second.getBottom(density)

    override fun hashCode(): Int = first.hashCode() + second.hashCode() * 31

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AddedInsets) {
            return false
        }
        return other.first == first && other.second == second
    }

    override fun toString(): String = "($first + $second)"
}

/**
 * An [WindowInsets] that includes the value of [included] that is not included in [excluded] as
 * returned from [WindowInsets.exclude].
 */
@Stable
private class ExcludeInsets(
    private val included: WindowInsets,
    private val excluded: WindowInsets
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        (included.getLeft(density, layoutDirection) - excluded.getLeft(density, layoutDirection))
            .coerceAtLeast(0)

    override fun getTop(density: Density) =
        (included.getTop(density) - excluded.getTop(density)).coerceAtLeast(0)

    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        (included.getRight(density, layoutDirection) - excluded.getRight(density, layoutDirection))
            .coerceAtLeast(0)

    override fun getBottom(density: Density) =
        (included.getBottom(density) - excluded.getBottom(density)).coerceAtLeast(0)

    override fun toString(): String = "($included - $excluded)"

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ExcludeInsets) {
            return false
        }

        return (other.included == included && other.excluded == excluded)
    }

    override fun hashCode(): Int = 31 * included.hashCode() + excluded.hashCode()
}

/**
 * An [WindowInsets] calculated from [paddingValues].
 */
@Stable
private class PaddingValuesInsets(private val paddingValues: PaddingValues) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    override fun getTop(density: Density) = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection) = with(density) {
        paddingValues.calculateRightPadding(layoutDirection).roundToPx()
    }

    override fun getBottom(density: Density) = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    override fun toString(): String {
        val layoutDirection = LayoutDirection.Ltr
        val start = paddingValues.calculateLeftPadding(layoutDirection)
        val top = paddingValues.calculateTopPadding()
        val end = paddingValues.calculateRightPadding(layoutDirection)
        val bottom = paddingValues.calculateBottomPadding()
        return "PaddingValues($start, $top, $end, $bottom)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PaddingValuesInsets) {
            return false
        }

        return other.paddingValues == paddingValues
    }

    override fun hashCode(): Int = paddingValues.hashCode()
}

@Stable
private class LimitInsets(
    val insets: WindowInsets,
    val sides: WindowInsetsSides
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
        val layoutDirectionSide = if (layoutDirection == LayoutDirection.Ltr) {
            WindowInsetsSides.AllowLeftInLtr
        } else {
            WindowInsetsSides.AllowLeftInRtl
        }
        val allowLeft = sides.hasAny(layoutDirectionSide)
        return if (allowLeft) {
            insets.getLeft(density, layoutDirection)
        } else {
            0
        }
    }

    override fun getTop(density: Density): Int =
        if (sides.hasAny(WindowInsetsSides.Top)) insets.getTop(density) else 0

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
        val layoutDirectionSide = if (layoutDirection == LayoutDirection.Ltr) {
            WindowInsetsSides.AllowRightInLtr
        } else {
            WindowInsetsSides.AllowRightInRtl
        }
        val allowRight = sides.hasAny(layoutDirectionSide)
        return if (allowRight) {
            insets.getRight(density, layoutDirection)
        } else {
            0
        }
    }

    override fun getBottom(density: Density): Int =
        if (sides.hasAny(WindowInsetsSides.Bottom)) insets.getBottom(density) else 0

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is LimitInsets) {
            return false
        }
        return insets == other.insets &&
            sides == other.sides
    }

    override fun hashCode(): Int {
        var result = insets.hashCode()
        result = 31 * result + sides.hashCode()
        return result
    }

    override fun toString(): String = "($insets only $sides)"
}

@Stable
private class InsetsPaddingValues(
    val insets: WindowInsets,
    private val density: Density
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = with(density) {
        insets.getLeft(this, layoutDirection).toDp()
    }

    override fun calculateTopPadding() = with(density) {
        insets.getTop(this).toDp()
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection) = with(density) {
        insets.getRight(this, layoutDirection).toDp()
    }

    override fun calculateBottomPadding() = with(density) {
        insets.getBottom(this).toDp()
    }

    override fun toString(): String {
        return "InsetsPaddingValues(insets=$insets, density=$density)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InsetsPaddingValues) {
            return false
        }
        return insets == other.insets && density == other.density
    }

    override fun hashCode(): Int {
        var result = insets.hashCode()
        result = 31 * result + density.hashCode()
        return result
    }
}
