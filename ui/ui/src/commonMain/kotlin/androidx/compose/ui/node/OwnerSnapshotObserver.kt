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

package androidx.compose.ui.node

import androidx.compose.runtime.snapshots.SnapshotStateObserver

/**
 * Performs snapshot observation for blocks like draw and layout which should be re-invoked
 * automatically when the snapshot value has been changed.
 */
@Suppress("CallbackName") // TODO rename this and SnapshotStateObserver. b/173401548
internal class OwnerSnapshotObserver(onChangedExecutor: (callback: () -> Unit) -> Unit) {

    private val observer = SnapshotStateObserver(onChangedExecutor)

    private val onCommitAffectingLookaheadMeasure: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValid) {
            layoutNode.requestLookaheadRemeasure()
        }
    }

    private val onCommitAffectingMeasure: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValid) {
            layoutNode.requestRemeasure()
        }
    }

    private val onCommitAffectingLayout: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValid) {
            layoutNode.requestRelayout()
        }
    }

    private val onCommitAffectingLayoutModifier: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValid) {
            layoutNode.requestRelayout()
        }
    }

    private val onCommitAffectingLayoutModifierInLookahead: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValid) {
            layoutNode.requestLookaheadRelayout()
        }
    }

    private val onCommitAffectingLookaheadLayout: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValid) {
            layoutNode.requestLookaheadRelayout()
        }
    }

    /**
     * Observe snapshot reads during layout of [node], executed in [block].
     */
    internal fun observeLayoutSnapshotReads(
        node: LayoutNode,
        affectsLookahead: Boolean = true,
        block: () -> Unit
    ) {
        if (affectsLookahead && node.mLookaheadScope != null) {
            observeReads(node, onCommitAffectingLookaheadLayout, block)
        } else {
            observeReads(node, onCommitAffectingLayout, block)
        }
    }

    /**
     * Observe snapshot reads during layout of [node]'s LayoutModifiers, executed in [block].
     */
    internal fun observeLayoutModifierSnapshotReads(
        node: LayoutNode,
        affectsLookahead: Boolean = true,
        block: () -> Unit
    ) {
        if (affectsLookahead && node.mLookaheadScope != null) {
            observeReads(node, onCommitAffectingLayoutModifierInLookahead, block)
        } else {
            observeReads(node, onCommitAffectingLayoutModifier, block)
        }
    }

    /**
     * Observe snapshot reads during measure of [node], executed in [block].
     */
    internal fun observeMeasureSnapshotReads(
        node: LayoutNode,
        affectsLookahead: Boolean = true,
        block: () -> Unit
    ) {
        if (affectsLookahead && node.mLookaheadScope != null) {
            observeReads(node, onCommitAffectingLookaheadMeasure, block)
        } else {
            observeReads(node, onCommitAffectingMeasure, block)
        }
    }

    /**
     * Observe snapshot reads for any target, allowing consumers to determine how to respond
     * to state changes.
     */
    internal fun <T : OwnerScope> observeReads(
        target: T,
        onChanged: (T) -> Unit,
        block: () -> Unit
    ) {
        observer.observeReads(target, onChanged, block)
    }

    internal fun clearInvalidObservations() {
        observer.clearIf { !(it as OwnerScope).isValid }
    }

    internal fun clear(target: Any) {
        observer.clear(target)
    }

    internal fun startObserving() {
        observer.start()
    }

    internal fun stopObserving() {
        observer.stop()
        observer.clear()
    }
}
