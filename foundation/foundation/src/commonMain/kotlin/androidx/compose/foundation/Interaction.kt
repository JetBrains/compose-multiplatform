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

package androidx.compose.foundation

/**
 * An Interaction represents transient UI state for a component, typically separate from the
 * actual 'business' state that a component may control. For example, a button typically fires an
 * `onClick` callback when the button is pressed and released, but it will still want to show
 * that it is being pressed before this callback is fired. This transient state is represented by
 * an Interaction, in this case [Pressed]. Using Interactions allows you to build
 * components that respond to these transient, component-owned state changes.
 *
 * The current interactions present on a given component are typically represented with an
 * [InteractionState]. See [InteractionState] for more information on consuming [Interaction]s,
 * and associated sample usage.
 */
interface Interaction {
    /**
     * An interaction corresponding to a dragged state on a component.
     *
     * See [draggable][androidx.compose.foundation.gestures.draggable]
     */
    object Dragged : Interaction

    /**
     * An interaction corresponding to a pressed state on a component.
     *
     * See [clickable]
     */
    object Pressed : Interaction

    /**
     * An interaction corresponding to a focused state on a component.
     *
     * See [focusable]
     */
    object Focused : Interaction

    /* TODO: b/152525426 add these states
    object Hovered : Interaction
    */
}
