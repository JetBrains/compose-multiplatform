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

package androidx.compose.ui

import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

internal expect fun areObjectsOfSameType(a: Any, b: Any): Boolean

/**
 * Reflectively resolves the properties and name of [element], and populates it in the receiver.
 * This function is used by [ModifierNodeElement] as a default implementation to provide inspection
 * info. If the lookup fails for any reason (either due to reflection being supported, property
 * lookups failing, or any other unexpected exception), we will silently ignore the error and
 * proceed so that we don't unexpectedly end the debugging session. This may result in omitted
 * information in these cases.
 *
 * @see ModifierNodeElement.inspectableProperties
 */
// TODO: For non-JVM platforms, you can revive the kotlin-reflect implementation from
//  https://android-review.googlesource.com/c/platform/frameworks/support/+/2441379
@OptIn(ExperimentalComposeUiApi::class)
internal expect fun InspectorInfo.tryPopulateReflectively(
    element: ModifierNodeElement<*>
)