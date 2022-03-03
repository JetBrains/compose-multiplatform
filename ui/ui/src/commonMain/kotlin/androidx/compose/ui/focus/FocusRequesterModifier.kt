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

package androidx.compose.ui.focus

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [modifier][Modifier.Element] that is used to pass in a [FocusRequester] that can be used to
 * request focus state changes.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 *
 * @see FocusRequester
 * @see Modifier.focusRequester
 */
interface FocusRequesterModifier : Modifier.Element {
    /**
     * An instance of [FocusRequester], that can be used to request focus state changes.
     *
     * @sample androidx.compose.ui.samples.RequestFocusSample
     */
    val focusRequester: FocusRequester
}

internal val ModifierLocalFocusRequester = modifierLocalOf<FocusRequesterModifierLocal?> { null }

internal class FocusRequesterModifierLocal(
    val focusRequester: FocusRequester
) : ModifierLocalConsumer, ModifierLocalProvider<FocusRequesterModifierLocal?> {
    private var parent: FocusRequesterModifierLocal? = null
    private val focusModifiers = mutableVectorOf<FocusModifier>()

    init {
        focusRequester.focusRequesterModifierLocals += this
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        val newParent = ModifierLocalFocusRequester.current
        if (newParent != parent) {
            parent?.removeFocusModifiers(focusModifiers)
            newParent?.addFocusModifiers(focusModifiers)
            parent = newParent
        }
    }

    override val key: ProvidableModifierLocal<FocusRequesterModifierLocal?>
        get() = ModifierLocalFocusRequester
    override val value: FocusRequesterModifierLocal
        get() = this

    fun addFocusModifier(focusModifier: FocusModifier) {
        focusModifiers += focusModifier
        parent?.addFocusModifier(focusModifier)
    }

    fun addFocusModifiers(newModifiers: MutableVector<FocusModifier>) {
        focusModifiers.addAll(newModifiers)
        parent?.addFocusModifiers(newModifiers)
    }

    fun removeFocusModifier(focusModifier: FocusModifier) {
        focusModifiers -= focusModifier
        parent?.removeFocusModifier(focusModifier)
    }

    fun removeFocusModifiers(removedModifiers: MutableVector<FocusModifier>) {
        focusModifiers.removeAll(removedModifiers)
        parent?.removeFocusModifiers(removedModifiers)
    }

    fun findFocusNode(): ModifiedFocusNode? {
        // find the first child:
        val first = focusModifiers.fold(null as FocusModifier?) { mod1, mod2 ->
            if (mod1 == null) {
                return@fold mod2
            }
            var layoutNode1 = mod1.focusNode.layoutNode
            var layoutNode2 = mod2.focusNode.layoutNode

            while (layoutNode1.parent != layoutNode2.parent) {
                layoutNode1 = layoutNode1.parent!!
                layoutNode2 = layoutNode2.parent!!
            }
            val children = layoutNode1.parent!!._children
            val index1 = children.indexOf(layoutNode1)
            val index2 = children.indexOf(layoutNode2)
            if (index1 < index2) mod1 else mod2
        }

        return first?.focusNode
    }
}

/**
 * Add this modifier to a component to request changes to focus.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 */
fun Modifier.focusRequester(focusRequester: FocusRequester): Modifier =
    composed(debugInspectorInfo {
        name = "focusRequester"
        properties["focusRequester"] = focusRequester
    }) {
        remember(focusRequester) {
            FocusRequesterModifierLocal(focusRequester)
        }
    }
