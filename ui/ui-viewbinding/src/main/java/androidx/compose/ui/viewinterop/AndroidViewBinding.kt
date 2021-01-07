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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.emit
import androidx.compose.ui.Modifier
import androidx.compose.ui.materialize
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.AmbientDensity
import androidx.viewbinding.ViewBinding

/**
 * Composes an Android layout resource in the presence of [ViewBinding]. The binding is obtained
 * from the [bindingBlock] block, which will be called exactly once to obtain the [ViewBinding]
 * to be composed, and it is also guaranteed to be invoked on the UI thread.
 * Therefore, in addition to creating the [ViewBinding], the block can also be used
 * to perform one-off initializations and [View] constant properties' setting.
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [View] properties depending on state. When state changes,
 * the block will be reexecuted to set the new properties. Note the block will also be ran once
 * right after the [bindingBlock] block completes.
 *
 * @sample androidx.compose.ui.samples.AndroidViewBindingSample
 *
 * @param bindingBlock The block creating the [ViewBinding] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
fun <T : ViewBinding> AndroidViewBinding(
    bindingBlock: (LayoutInflater, ViewGroup, Boolean) -> T,
    modifier: Modifier = Modifier,
    update: T.() -> Unit = {}
) {
    val context = AmbientContext.current
    val materialized = currentComposer.materialize(modifier)
    val density = AmbientDensity.current
    emit<ViewBindingHolder<T>, UiApplier>(
        factory = { ViewBindingHolder<T>(context).also { it.bindingBlock = bindingBlock } },
        update = {
            set(materialized) { this.modifier = it }
            set(density) { this.density = it }
            set(update) { this.updateBlock = it }
        }
    )
}

@OptIn(InternalInteropApi::class)
internal class ViewBindingHolder<T : ViewBinding>(
    context: Context
) : AndroidViewHolder(context) {
    private var viewBinding: T? = null
        set(value) {
            field = value
            if (value != null) {
                view = value.root
            }
        }

    internal var bindingBlock: ((LayoutInflater, ViewGroup, Boolean) -> T)? = null
        set(value) {
            field = value
            if (value != null) {
                val layoutParamsParent = (parent as? ViewGroup) ?: this
                viewBinding = value(LayoutInflater.from(context), layoutParamsParent, false)
            }
        }

    internal var updateBlock: (T) -> Unit = {}
        set(value) {
            field = value
            update = { viewBinding?.apply(updateBlock) }
        }
}