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

package androidx.compose.foundation

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.debugInspectorInfo

@OptIn(ExperimentalFoundationApi::class)
internal val ModifierLocalFocusedBoundsObserver =
    modifierLocalOf<((LayoutCoordinates?) -> Unit)?> { null }

/**
 * Calls [onPositioned] whenever the bounds of the currently-focused area changes.
 * If a child of this node has focus, [onPositioned] will be called immediately with a non-null
 * [LayoutCoordinates] that can be queried for the focused bounds, and again every time the focused
 * child changes or is repositioned. When a child loses focus, [onPositioned] will be passed `null`.
 *
 * When an event occurs, it is bubbled up from the focusable node, so the nearest parent gets the
 * event first, and then its parent, etc.
 *
 * Note that there may be some cases where the focused bounds change but the callback is _not_
 * invoked, but the last [LayoutCoordinates] will always return the most up-to-date bounds.
 */
@ExperimentalFoundationApi
fun Modifier.onFocusedBoundsChanged(onPositioned: (LayoutCoordinates?) -> Unit): Modifier =
    composed(
        debugInspectorInfo {
            name = "onFocusedBoundsChanged"
            properties["onPositioned"] = onPositioned
        }
    ) {
        remember(onPositioned) { FocusedBoundsObserverModifier(onPositioned) }
    }

private class FocusedBoundsObserverModifier(
    private val handler: (LayoutCoordinates?) -> Unit
) : ModifierLocalConsumer,
    ModifierLocalProvider<((LayoutCoordinates?) -> Unit)?>,
        (LayoutCoordinates?) -> Unit {
    private var parent: ((LayoutCoordinates?) -> Unit)? = null
    private var lastBounds: LayoutCoordinates? = null

    override val key: ProvidableModifierLocal<((LayoutCoordinates?) -> Unit)?>
        get() = ModifierLocalFocusedBoundsObserver
    override val value: (LayoutCoordinates?) -> Unit
        get() = this

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        val newParent = with(scope) { ModifierLocalFocusedBoundsObserver.current }
        if (newParent != parent) {
            parent = newParent
            // Don't need to call the new parent ourselves because the child will also get the
            // modifier locals updated callback, and it will bubble the event up itself.
        }
    }

    /** Called when a child gains/loses focus or is focused and changes position. */
    override fun invoke(focusedBounds: LayoutCoordinates?) {
        lastBounds = focusedBounds
        handler(focusedBounds)
        parent?.invoke(focusedBounds)
    }
}

/**
 * Modifier used by [Modifier.focusable] to publish the location of the focused element.
 * Should only be applied to the node when it is actually focused.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class FocusedBoundsModifier : ModifierLocalConsumer,
    OnGloballyPositionedModifier {
    private var observer: ((LayoutCoordinates?) -> Unit)? = null
    private var layoutCoordinates: LayoutCoordinates? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        layoutCoordinates = coordinates
        if (coordinates.isAttached) {
            notifyObserverWhenAttached()
        } else {
            observer?.invoke(null)
        }
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        val newObserver = with(scope) { ModifierLocalFocusedBoundsObserver.current }
        if (newObserver == null) {
            // We're being removed from the hierarchy. Inform the previous listener.
            observer?.invoke(null)
        }
        observer = newObserver
        // Don't need to explicitly notify observers here because onGloballyPositioned will get
        // called after this method, and that will notify observers.
    }

    private fun notifyObserverWhenAttached() {
        if (layoutCoordinates != null && layoutCoordinates!!.isAttached) {
            observer?.invoke(layoutCoordinates)
        }
    }
}