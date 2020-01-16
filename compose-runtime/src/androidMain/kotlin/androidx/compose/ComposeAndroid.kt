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

package androidx.compose

import android.app.Activity

/**
 * Sets the contentView of an activity to a FrameLayout, and composes the contents of the layout
 * with the passed in [composable]. This is a convenience method around [Compose.composeInto].
 *
 * @see Compose.composeInto
 * @see Activity.setContentView
 */
fun Activity.setViewContent(composable: @Composable() () -> Unit): Composition? {
    // If there is already a FrameLayout in the root, we assume we want to compose
    // into it instead of create a new one. This allows for `setContent` to be
    // called multiple times.
    val root = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: FrameLayout(this).also { setContentView(it) }
    return root.setViewContent(composable)
}

/**
 * Disposes of a composition that was started using [setContent]. This is a convenience method
 * around [Compose.disposeComposition].
 *
 * @see setContent
 * @see Compose.disposeComposition
 */
fun Activity.disposeComposition() {
    val view = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: error("No root view found")
    Compose.disposeComposition(view, null)
}