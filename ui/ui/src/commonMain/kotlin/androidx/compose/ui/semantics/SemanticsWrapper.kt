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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRoot
import androidx.compose.ui.node.DelegatingLayoutNodeWrapper
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.LayoutNodeWrapper

internal class SemanticsWrapper(
    wrapped: LayoutNodeWrapper,
    semanticsModifier: SemanticsModifier
) : DelegatingLayoutNodeWrapper<SemanticsModifier>(wrapped, semanticsModifier) {
    private val useMinimumTouchTarget: Boolean
        get() = modifier.semanticsConfiguration.getOrNull(SemanticsActions.OnClick) != null

    fun collapsedSemanticsConfiguration(): SemanticsConfiguration {
        val nextSemantics = wrapped.nearestSemantics { true }
        if (nextSemantics == null || modifier.semanticsConfiguration.isClearingSemantics) {
            return modifier.semanticsConfiguration
        }

        val config = modifier.semanticsConfiguration.copy()
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
        hitSemanticsWrappers: HitTestResult<SemanticsWrapper>
    ) {
        hitTestInMinimumTouchTarget(
            pointerPosition,
            hitSemanticsWrappers,
            forceParentIntercept = false,
            useTouchSize = true,
            content = this
        ) {
            // Also, keep looking to see if we also might hit any children.
            // This avoids checking layer bounds twice as when we call super.hitTest()
            val positionInWrapped = wrapped.fromParentPosition(pointerPosition)
            wrapped.hitTestSemantics(positionInWrapped, hitSemanticsWrappers)
        }
    }

    fun touchBoundsInRoot(): Rect {
        if (!isAttached) {
            return Rect.Zero
        }
        if (!useMinimumTouchTarget) {
            return boundsInRoot()
        }

        val root = findRoot()

        val bounds = rectCache
        val padding = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        bounds.left = -padding.width
        bounds.top = -padding.height
        bounds.right = measuredWidth + padding.width
        bounds.bottom = measuredHeight + padding.height

        var wrapper: LayoutNodeWrapper = this
        while (wrapper !== root) {
            wrapper.rectInParent(bounds, clipBounds = false, clipToMinimumTouchTargetSize = true)
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            wrapper = wrapper.wrappedBy!!
        }
        return bounds.toRect()
    }
}
