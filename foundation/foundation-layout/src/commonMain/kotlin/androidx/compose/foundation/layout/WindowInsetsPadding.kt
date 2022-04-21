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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

/**
 * Adds padding so that the content doesn't enter [insets] space.
 *
 * Any insets consumed by other insets padding modifiers or [consumedWindowInsets] on a parent
 * layout will be excluded from [insets]. [insets] will be [consumed][consumedWindowInsets] for
 * child layouts as well.
 *
 * For example, if an ancestor uses [statusBarsPadding] and this modifier uses
 * [WindowInsets.Companion.systemBars], the portion of the system bars that the status bars uses
 * will not be padded again by this modifier.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsPaddingSample
 * @see WindowInsets
 */
@Stable
fun Modifier.windowInsetsPadding(insets: WindowInsets): Modifier = this.then(
    InsetsPaddingModifier(insets, debugInspectorInfo {
        name = "windowInsetsPadding"
        properties["insets"] = insets
    })
)

/**
 * Consume insets that haven't been consumed yet by other insets Modifiers similar to
 * [windowInsetsPadding] without adding any padding.
 *
 * This can be useful when content offsets are provided by [WindowInsets.asPaddingValues].
 * This should be used further down the hierarchy than the [PaddingValues] is used so
 * that the values aren't consumed before the padding is added.
 *
 * @sample androidx.compose.foundation.layout.samples.consumedInsetsSample
 */
@ExperimentalLayoutApi
@Stable
fun Modifier.consumedWindowInsets(insets: WindowInsets): Modifier = this.then(
    UnionInsetsConsumingModifier(insets, debugInspectorInfo {
        name = "consumedWindowInsets"
        properties["insets"] = insets
    })
)

/**
 * Consume [paddingValues] as insets as if the padding was added irrespective of insets.
 * Layouts further down the hierarchy that use [windowInsetsPadding], [safeContentPadding],
 * and other insets padding Modifiers won't pad for the values that [paddingValues] provides.
 * This can be useful when content offsets are provided by layout rather than [windowInsetsPadding]
 * modifiers.
 *
 * This method consumes all of [paddingValues] in addition to whatever has been
 * consumed by other [windowInsetsPadding] modifiers by ancestors. [consumedWindowInsets]
 * accepting a [WindowInsets] argument ensures that its insets are consumed and doesn't
 * consume more if they have already been consumed by ancestors.
 *
 * @sample androidx.compose.foundation.layout.samples.consumedInsetsPaddingSample
 */
@ExperimentalLayoutApi
@Stable
fun Modifier.consumedWindowInsets(paddingValues: PaddingValues): Modifier = this.then(
    PaddingValuesConsumingModifier(paddingValues, debugInspectorInfo {
        name = "consumedWindowInsets"
        properties["paddingValues"] = paddingValues
    })
)

internal val ModifierLocalConsumedWindowInsets = modifierLocalOf {
    WindowInsets(0, 0, 0, 0)
}

internal class InsetsPaddingModifier(
    private val insets: WindowInsets,
    inspectorInfo: InspectorInfo.() -> Unit = debugInspectorInfo {
        name = "InsetsPaddingModifier"
        properties["insets"] = insets
    }
) : InspectorValueInfo(inspectorInfo), LayoutModifier,
    ModifierLocalConsumer, ModifierLocalProvider<WindowInsets> {
    private var unconsumedInsets: WindowInsets by mutableStateOf(insets)
    private var consumedInsets: WindowInsets by mutableStateOf(insets)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val left = unconsumedInsets.getLeft(this, layoutDirection)
        val top = unconsumedInsets.getTop(this)
        val right = unconsumedInsets.getRight(this, layoutDirection)
        val bottom = unconsumedInsets.getBottom(this)

        val horizontal = left + right
        val vertical = top + bottom

        val childConstraints = constraints.offset(-horizontal, -vertical)
        val placeable = measurable.measure(childConstraints)

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) {
            placeable.place(left, top)
        }
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) {
            val consumed = ModifierLocalConsumedWindowInsets.current
            unconsumedInsets = insets.exclude(consumed)
            consumedInsets = consumed.union(insets)
        }
    }

    override val key: ProvidableModifierLocal<WindowInsets>
        get() = ModifierLocalConsumedWindowInsets

    override val value: WindowInsets
        get() = consumedInsets

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InsetsPaddingModifier) {
            return false
        }

        return other.insets == insets
    }

    override fun hashCode(): Int = insets.hashCode()
}

/**
 * Base class for arbitrary insets consumption modifiers.
 */
@Stable
private sealed class InsetsConsumingModifier(
    inspectorInfo: InspectorInfo.() -> Unit
) : InspectorValueInfo(inspectorInfo), ModifierLocalConsumer, ModifierLocalProvider<WindowInsets> {
    private var consumedInsets: WindowInsets by mutableStateOf(WindowInsets(0, 0, 0, 0))

    abstract fun calculateInsets(modifierLocalInsets: WindowInsets): WindowInsets

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) {
            val current = ModifierLocalConsumedWindowInsets.current
            consumedInsets = calculateInsets(current)
        }
    }

    override val key: ProvidableModifierLocal<WindowInsets>
        get() = ModifierLocalConsumedWindowInsets

    override val value: WindowInsets
        get() = consumedInsets
}

@Stable
private class PaddingValuesConsumingModifier(
    private val paddingValues: PaddingValues,
    inspectorInfo: InspectorInfo.() -> Unit
) : InsetsConsumingModifier(inspectorInfo) {
    override fun calculateInsets(modifierLocalInsets: WindowInsets): WindowInsets =
        paddingValues.asInsets().add(modifierLocalInsets)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PaddingValuesConsumingModifier) {
            return false
        }

        return other.paddingValues == paddingValues
    }

    override fun hashCode(): Int = paddingValues.hashCode()
}

@Stable
private class UnionInsetsConsumingModifier(
    private val insets: WindowInsets,
    inspectorInfo: InspectorInfo.() -> Unit
) : InsetsConsumingModifier(inspectorInfo) {
    override fun calculateInsets(modifierLocalInsets: WindowInsets): WindowInsets =
        insets.union(modifierLocalInsets)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is UnionInsetsConsumingModifier) {
            return false
        }

        return other.insets == insets
    }

    override fun hashCode(): Int = insets.hashCode()
}