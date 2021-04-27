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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.NoInspectorInfo

/**
 * Declare a just-in-time composition of a [Modifier] that will be composed for each element it
 * modifies. [composed] may be used to implement **stateful modifiers** that have
 * instance-specific state for each modified element, allowing the same [Modifier] instance to be
 * safely reused for multiple elements while maintaining element-specific state.
 *
 * If [inspectorInfo] is specified this modifier will be visible to tools during development.
 * Specify the name and arguments of the original modifier.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierSample
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierWithArgumentsSample
 *
 * [materialize] must be called to create instance-specific modifiers if you are directly
 * applying a [Modifier] to an element tree node.
 */
fun Modifier.composed(
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
    factory: @Composable Modifier.() -> Modifier
): Modifier = this.then(ComposedModifier(inspectorInfo, factory))

private class ComposedModifier(
    inspectorInfo: InspectorInfo.() -> Unit,
    val factory: @Composable Modifier.() -> Modifier
) : Modifier.Element, InspectorValueInfo(inspectorInfo)

/**
 * Materialize any instance-specific [composed modifiers][composed] for applying to a raw tree node.
 * Call right before setting the returned modifier on an emitted node.
 * You almost certainly do not need to call this function directly.
 */
@Suppress("ModifierFactoryExtensionFunction")
fun Composer.materialize(modifier: Modifier): Modifier {
    if (modifier.all { it !is ComposedModifier }) return modifier

    // This is a fake composable function that invokes the compose runtime directly so that it
    // can call the element factory functions from the non-@Composable lambda of Modifier.foldIn.
    // It would be more efficient to redefine the Modifier type hierarchy such that the fold
    // operations could be inlined or otherwise made cheaper, which could make this unnecessary.

    // Random number for fake group key. Chosen by fair die roll.
    startReplaceableGroup(0x48ae8da7)

    val result = modifier.foldIn<Modifier>(Modifier) { acc, element ->
        acc.then(
            if (element is ComposedModifier) {
                @kotlin.Suppress("UNCHECKED_CAST")
                val factory = element.factory as Modifier.(Composer, Int) -> Modifier
                val composedMod = factory(Modifier, this, 0)
                materialize(composedMod)
            } else element
        )
    }

    endReplaceableGroup()
    return result
}
