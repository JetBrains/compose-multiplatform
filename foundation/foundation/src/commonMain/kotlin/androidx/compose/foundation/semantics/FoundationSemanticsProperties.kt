/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.semantics

import androidx.compose.foundation.selection.ToggleableState
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics properties that apply to the Compose Foundation UI elements.  Used for making
 * assertions in testing.
 */
object FoundationSemanticsProperties {
    /**
     * Whether this element is selected (out of a list of possible selections).
     * The presence of this property indicates that the element is selectable.
     *
     * @see SemanticsPropertyReceiver.selected
     */
    val Selected = SemanticsPropertyKey<Boolean>("Selected")

    /**
     * Whether this element is in a group from which only a single item can be selected at any given
     * time (such as a radio group)
     *
     * The presence of this property indicates that the element is a member of
     * a selectable group (exclusive or not).
     *
     *  @see SemanticsPropertyReceiver.inMutuallyExclusiveGroup
     */
    val InMutuallyExclusiveGroup = SemanticsPropertyKey<Boolean>("InMutuallyExclusiveGroup")

    /**
     * The state of a toggleable component.
     * The presence of this property indicates that the element is toggleable.
     *
     * @see SemanticsPropertyReceiver.toggleableState
     */
    val ToggleableState = SemanticsPropertyKey<ToggleableState>("ToggleableState")
}

/**
 * Whether this element is in a group from which only a single item can be selected at any given
 * time (such as a radio group)
 *
 *  @see FoundationSemanticsProperties.InMutuallyExclusiveGroup
 */
var SemanticsPropertyReceiver.inMutuallyExclusiveGroup
by FoundationSemanticsProperties.InMutuallyExclusiveGroup

/**
 * Whether this element is selected (out of a list of possible selections).
 * The presence of this property indicates that the element is selectable.
 *
 * @see FoundationSemanticsProperties.Selected
 */
var SemanticsPropertyReceiver.selected by FoundationSemanticsProperties.Selected

/**
 * The state of a toggleable component.
 * The presence of this property indicates that the element is toggleable.
 *
 * @see FoundationSemanticsProperties.ToggleableState
 */
var SemanticsPropertyReceiver.toggleableState
by FoundationSemanticsProperties.ToggleableState
