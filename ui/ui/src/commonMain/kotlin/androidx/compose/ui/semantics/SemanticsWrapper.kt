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

package androidx.compose.ui.semantics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatingLayoutNodeWrapper
import androidx.compose.ui.node.LayoutNodeWrapper

internal class SemanticsWrapper(
    wrapped: LayoutNodeWrapper,
    semanticsModifier: SemanticsModifier
) : DelegatingLayoutNodeWrapper<SemanticsModifier>(wrapped, semanticsModifier) {
    fun collapsedSemanticsConfiguration(): SemanticsConfiguration {
        val nextSemantics = wrapped.nearestSemantics { true }
        if (nextSemantics == null || modifier.semanticsConfiguration.isClearingSemantics) {
            return modifier.semanticsConfiguration
        }

        var config = modifier.semanticsConfiguration.copy()
        config.collapsePeer(nextSemantics.collapsedSemanticsConfiguration())
        return config
    }

    override fun detach() {
        super.detach()
        layoutNode.owner?.onSemanticsChange()
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        layoutNode.owner?.onSemanticsChange()
    }

    override fun toString(): String {
        return "${super.toString()} id: ${modifier.id} config: ${modifier.semanticsConfiguration}"
    }

    override fun hitTestSemantics(
        pointerPosition: Offset,
        hitSemanticsWrappers: MutableList<SemanticsWrapper>
    ) {
        if (isPointerInBounds(pointerPosition) && withinLayerBounds(pointerPosition)) {
            hitSemanticsWrappers.add(this)

            val positionInWrapped = wrapped.fromParentPosition(pointerPosition)
            wrapped.hitTestSemantics(positionInWrapped, hitSemanticsWrappers)
        }
    }
}
