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

import androidx.compose.ui.draw.BuildDrawCacheParams
import androidx.compose.ui.draw.DrawCacheModifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize

internal class ModifiedDrawNode(
    wrapped: LayoutNodeWrapper,
    drawModifier: DrawModifier
) : DelegatingLayoutNodeWrapper<DrawModifier>(wrapped, drawModifier), OwnerScope {

    private var cacheDrawModifier: DrawCacheModifier? = updateCacheDrawModifier()

    private val buildCacheParams: BuildDrawCacheParams = object : BuildDrawCacheParams {
        // b/173669932 we should not cache this here, however, on subsequent modifier updates
        // the density provided via layoutNode.density becomes 1
        override val density = layoutNode.density

        override val layoutDirection: LayoutDirection get() = layoutNode.layoutDirection

        override val size: Size get() = measuredSize.toSize()
    }

    // Flag to determine if the cache should be re-built
    private var invalidateCache = true

    // Callback used to build the drawing cache
    private val updateCache = {
        // b/173669932 figure out why layoutNode.mDrawScope density is 1 after observation updates
        // and use that here instead of the cached density we get in the constructor
        cacheDrawModifier?.onBuildCache(buildCacheParams)
        invalidateCache = false
    }

    // Intentionally returning DrawCacheModifier not generic Modifier type
    // to make sure that we are updating the current DrawCacheModifier in the
    // event that a new DrawCacheModifier is provided
    // Suppressing insepctorinfo as relying on the inspector info for
    // DrawCacheModifier
    @Suppress(
        "ModifierInspectorInfo",
        "ModifierFactoryReturnType",
        "ModifierFactoryExtensionFunction"
    )
    private fun updateCacheDrawModifier(): DrawCacheModifier? {
        val current = modifier
        return if (current is DrawCacheModifier) {
            current
        } else {
            null
        }
    }

    override var modifier: DrawModifier
        get() = super.modifier
        set(value) {
            super.modifier = value
            cacheDrawModifier = updateCacheDrawModifier()
            invalidateCache = true
        }

    override fun onMeasureResultChanged(width: Int, height: Int) {
        super.onMeasureResultChanged(width, height)
        invalidateCache = true
    }

    // This is not thread safe
    override fun performDraw(canvas: Canvas) {
        val size = measuredSize.toSize()
        if (cacheDrawModifier != null && invalidateCache) {
            layoutNode.requireOwner().snapshotObserver.observeReads(
                this,
                onCommitAffectingModifiedDrawNode,
                updateCache
            )
        }

        val drawScope = layoutNode.mDrawScope
        drawScope.draw(canvas, size, wrapped) {
            with(drawScope) {
                with(modifier) {
                    draw()
                }
            }
        }
    }

    companion object {
        // Callback invoked whenever a state parameter that is read within the cache
        // execution callback is updated. This marks the cache flag as dirty and
        // invalidates the current layer.
        private val onCommitAffectingModifiedDrawNode: (ModifiedDrawNode) -> Unit =
            { modifiedDrawNode ->
                if (modifiedDrawNode.isValid) {
                    modifiedDrawNode.invalidateCache = true
                    modifiedDrawNode.invalidateLayer()
                }
            }
    }

    override val isValid: Boolean
        get() = isAttached
}