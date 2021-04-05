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
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.fastForEach
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment
import androidx.viewbinding.ViewBinding

/**
 * Composes an Android layout resource in the presence of [ViewBinding]. The binding is obtained
 * from the [factory] block, which will be called exactly once to obtain the [ViewBinding]
 * to be composed, and it is also guaranteed to be invoked on the UI thread.
 * Therefore, in addition to creating the [ViewBinding], the block can also be used
 * to perform one-off initializations and [View] constant properties' setting.
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [View] properties depending on state. When state changes,
 * the block will be reexecuted to set the new properties. Note the block will also be ran once
 * right after the [factory] block completes.
 *
 * @sample androidx.compose.ui.samples.AndroidViewBindingSample
 *
 * @param factory The block creating the [ViewBinding] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
fun <T : ViewBinding> AndroidViewBinding(
    factory: (inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) -> T,
    modifier: Modifier = Modifier,
    update: T.() -> Unit = {}
) {
    val viewBindingRef = remember { Ref<T>() }
    val localView = LocalView.current
    // Find the parent fragment, if one exists. This will let us ensure that
    // fragments inflated via a FragmentContainerView are properly nested
    // (which, in turn, allows the fragments to properly save/restore their state)
    val parentFragment = remember(localView) {
        try {
            localView.findFragment<Fragment>()
        } catch (e: IllegalStateException) {
            // findFragment throws if no parent fragment is found
            null
        }
    }
    val fragmentContainerViews = remember { mutableStateListOf<FragmentContainerView>() }
    val viewBlock: (Context) -> View = remember(localView) {
        { context ->
            // Inflated fragments are automatically nested properly when
            // using the parent fragment's LayoutInflater
            val inflater = parentFragment?.layoutInflater ?: LayoutInflater.from(context)
            val viewBinding = factory(inflater, FrameLayout(context), false)
            viewBindingRef.value = viewBinding
            // Find all FragmentContainerView instances in the newly inflated layout
            fragmentContainerViews.clear()
            val rootGroup = viewBinding.root as? ViewGroup
            if (rootGroup != null) {
                findFragmentContainerViews(rootGroup, fragmentContainerViews)
            }
            viewBinding.root
        }
    }
    AndroidView(
        factory = viewBlock,
        modifier = modifier,
        update = { viewBindingRef.value?.update() }
    )

    // Set up a DisposableEffect for each FragmentContainerView that will
    // clean up inflated fragments when the AndroidViewBinding is disposed
    val localContext = LocalContext.current
    fragmentContainerViews.fastForEach { container ->
        DisposableEffect(localContext, container) {
            // Find the right FragmentManager
            val fragmentManager = parentFragment?.childFragmentManager
                ?: (localContext as? FragmentActivity)?.supportFragmentManager
            // Now find the fragment inflated via the FragmentContainerView
            val existingFragment = fragmentManager?.findFragmentById(container.id)
            onDispose {
                if (existingFragment != null && !fragmentManager.isStateSaved) {
                    // If the state isn't saved, that means that some state change
                    // has removed this Composable from the hierarchy
                    fragmentManager.commit {
                        remove(existingFragment)
                    }
                }
            }
        }
    }
}

private fun findFragmentContainerViews(
    viewGroup: ViewGroup,
    list: MutableList<FragmentContainerView>
) {
    if (viewGroup is FragmentContainerView) {
        list += viewGroup
    } else {
        viewGroup.forEach {
            if (it is ViewGroup) {
                findFragmentContainerViews(it, list)
            }
        }
    }
}
