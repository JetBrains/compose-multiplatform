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

package androidx.compose.ui.semantics

import androidx.compose.ui.ExperimentalComposeUiApi

@ExperimentalComposeUiApi
object SemanticsPropertiesAndroid {
    /**
     * @see SemanticsPropertyReceiver.testTagsAsResourceId
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    @ExperimentalComposeUiApi
    val TestTagsAsResourceId = SemanticsPropertyKey<Boolean>(
        name = "TestTagsAsResourceId",
        mergePolicy = { parentValue, _ -> parentValue }
    )
}

/**
 * Configuration toggle to map testTags to resource-id.
 *
 * This provides a way of filling in AccessibilityNodeInfo.viewIdResourceName, which in the View System
 * is populated based on the resource string in the XML.
 *
 * testTags are also provided in AccessibilityNodeInfo.extras under key
 * "androidx.compose.ui.semantics.testTag". However, when using UIAutomator or on Android 7 and below,
 * extras are not available, so a more backwards-compatible way of making testTags available to
 * accessibility-tree-based integration tests is sometimes needed. resource-id was the most natural
 * property to repurpose for this.
 *
 * This property applies to a semantics subtree. For example, if it's set to true on the root semantics
 * node of the app (and no child nodes set it back to false), then every testTag will be mapped.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalComposeUiApi
@set:ExperimentalComposeUiApi
@ExperimentalComposeUiApi
var SemanticsPropertyReceiver.testTagsAsResourceId by
    SemanticsPropertiesAndroid.TestTagsAsResourceId