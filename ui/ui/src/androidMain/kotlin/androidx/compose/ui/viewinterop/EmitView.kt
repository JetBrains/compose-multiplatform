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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.AmbientContext

@Suppress("ComposableNaming")
@Composable
@Deprecated(
    "emitView will be removed. Use AndroidView instead if possible. " +
        "Composing Views and ViewGroups directly in an existing UI composition will not be " +
        "supported in the future."
)
fun <T : View> emitView(
    ctor: (Context) -> T,
    update: (T) -> Unit
) {
    val context = AmbientContext.current
    ComposeNode<T, UiApplier>(
        factory = { ctor(context) },
        update = {
            reconcile(update)
        }
    )
}

@Suppress("ComposableNaming", "ComposableLambdaParameterNaming")
@Composable
@Deprecated(
    "emitView will be removed. Use AndroidView instead if possible. " +
        "Composing Views and ViewGroups directly in an existing UI composition will not be " +
        "supported in the future."
)
fun <T : ViewGroup> emitView(
    ctor: (Context) -> T,
    update: (T) -> Unit,
    children: @Composable () -> Unit
) {
    val context = AmbientContext.current
    ComposeNode<T, UiApplier>(
        factory = { ctor(context) },
        update = {
            reconcile(update)
        },
        content = children
    )
}
