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

package androidx.compose.foundation.interaction

/**
 * An Interaction represents transient UI state for a component, typically separate from the
 * actual 'business' state that a component may control. For example, a button typically fires an
 * `onClick` callback when the button is pressed and released, but it may still want to show
 * that it is being pressed before this callback is fired. This transient state is represented by
 * an Interaction, in this case [PressInteraction.Press]. Using Interactions allows you to build
 * components that respond to these transient, component-owned state changes.
 *
 * To emit / observe current Interactions, see [MutableInteractionSource], which represents a
 * stream of Interactions present for a given component.
 *
 * @see InteractionSource
 * @see MutableInteractionSource
 */
interface Interaction
