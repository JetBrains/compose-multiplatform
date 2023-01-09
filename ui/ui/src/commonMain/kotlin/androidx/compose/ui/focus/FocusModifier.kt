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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier

/**
 * Add this modifier to a component to make it focusable.
 *
 * Focus state is stored within this modifier. The bounds of this modifier reflect the bounds of
 * the focus box.
 *
 * Note: This is a low level modifier. Before using this consider using
 * [Modifier.focusable()][androidx.compose.foundation.focusable]. It uses a [focusTarget] in
 * its implementation. [Modifier.focusable()][androidx.compose.foundation.focusable] adds semantics
 * that are needed for accessibility.
 *
 * @sample androidx.compose.ui.samples.FocusableSampleUsingLowerLevelFocusTarget
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusTarget(): Modifier = this then FocusTargetModifierNode.FocusTargetModifierElement

/**
 * Add this modifier to a component to make it focusable.
 */
@Deprecated(
    "Replaced by focusTarget",
    ReplaceWith("focusTarget()", "androidx.compose.ui.focus.focusTarget")
)
fun Modifier.focusModifier(): Modifier = focusTarget()
