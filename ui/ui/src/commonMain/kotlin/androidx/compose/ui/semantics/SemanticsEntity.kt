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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.EntityList
import androidx.compose.ui.node.LayoutNodeEntity
import androidx.compose.ui.node.NodeCoordinator

internal class SemanticsEntity(
    wrapped: NodeCoordinator,
    modifier: SemanticsModifier
) : LayoutNodeEntity<SemanticsEntity, SemanticsModifier>(wrapped, modifier) {
    val id: Int
        get() = layoutNode.semanticsId

    private val useMinimumTouchTarget: Boolean
        get() = modifier.semanticsConfiguration.getOrNull(SemanticsActions.OnClick) != null

    fun collapsedSemanticsConfiguration(): SemanticsConfiguration {
        val next = next
        val nextSemantics = if (next == null) {
            coordinator.wrapped?.nearestSemantics { true }
        } else {
            next.nearestSemantics { true }
        }
        if (nextSemantics == null || modifier.semanticsConfiguration.isClearingSemantics) {
            return modifier.semanticsConfiguration
        }

        val config = modifier.semanticsConfiguration.copy()
        config.collapsePeer(nextSemantics.collapsedSemanticsConfiguration())
        return config
    }

    override fun onDetach() {
        super.onDetach()
        layoutNode.owner?.onSemanticsChange()
    }

    override fun onAttach() {
        super.onAttach()
        layoutNode.owner?.onSemanticsChange()
    }

    override fun toString(): String {
        return "${super.toString()} semanticsId: $id config: ${modifier.semanticsConfiguration}"
    }

    fun touchBoundsInRoot(): Rect {
        if (!isAttached) {
            return Rect.Zero
        }
        if (!useMinimumTouchTarget) {
            return coordinator.boundsInRoot()
        }

        return coordinator.touchBoundsInRoot()
    }

    internal inline fun nearestSemantics(
        predicate: (SemanticsEntity) -> Boolean
    ): SemanticsEntity? {
        var coordinator: NodeCoordinator? = coordinator
        var next: SemanticsEntity? = this
        while (coordinator != null) {
            while (next != null) {
                if (predicate(next)) {
                    return next
                }
                next = next.next
            }
            coordinator = coordinator.wrapped
            next = coordinator?.entities?.head(EntityList.SemanticsEntityType)
        }
        return null
    }
}
