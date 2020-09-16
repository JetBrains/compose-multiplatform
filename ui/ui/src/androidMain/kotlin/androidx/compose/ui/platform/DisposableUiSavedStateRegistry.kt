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

package androidx.compose.ui.platform

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import java.io.Serializable

/**
 * Creates [DisposableUiSavedStateRegistry] associated with these [view] and [owner].
 */
internal fun DisposableUiSavedStateRegistry(
    view: View,
    owner: SavedStateRegistryOwner
): DisposableUiSavedStateRegistry {
    // When AndroidComposeView is composed into some ViewGroup we just added as a child for this
    // ViewGroup. And we don't have any id on AndroidComposeView as we can't make it unique, but
    // we require this parent ViewGroup to have an unique id for the saved instance state mechanism
    // to work (similarly to how it works without Compose). When we composed into Activity our
    // parent is the ViewGroup with android.R.id.content.
    val parentId: Int = (view.parent as? View)?.id ?: View.NO_ID
    return DisposableUiSavedStateRegistry(parentId, owner)
}

/**
 * Creates [DisposableUiSavedStateRegistry] with the restored values using [SavedStateRegistry] and
 * saves the values when [SavedStateRegistry] performs save.
 *
 * To provide a namespace we require unique [id]. We can't use the default way of doing it when we
 * have unique id on [AndroidComposeView] because we dynamically create [AndroidComposeView]s and
 * there is no way to have a unique id given there are could be any number of
 * [AndroidComposeView]s inside the same Activity. If we use [View.generateViewId]
 * this id will not survive Activity recreation.
 * But it is reasonable to ask our users to have an unique id on the parent ViewGroup in which we
 * compose our [AndroidComposeView]. If Activity.setContent is used then it will be a View with
 * [android.R.id.content], if ViewGroup.setContent is used then we will ask users to provide an
 * id for this ViewGroup. If @GenerateView will be used then we will ask users to set an id on
 * this generated View.
 */
internal fun DisposableUiSavedStateRegistry(
    id: Int,
    savedStateRegistryOwner: SavedStateRegistryOwner
): DisposableUiSavedStateRegistry {
    val key = "${UiSavedStateRegistry::class.java.simpleName}:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap()

    val uiSavedStateRegistry = UiSavedStateRegistry(restored) {
        canBeSavedToBundle(it)
    }
    val registered = try {
        androidxRegistry.registerSavedStateProvider(key) {
            uiSavedStateRegistry.performSave().toBundle()
        }
        true
    } catch (ignore: IllegalArgumentException) {
        // this means there are two AndroidComposeViews composed into different parents with the
        // same view id. currently we will just not save/restore state for the second
        // AndroidComposeView.
        // TODO: we should verify our strategy for such cases and improve it. b/162397322
        false
    }
    return DisposableUiSavedStateRegistry(uiSavedStateRegistry) {
        if (registered) {
            androidxRegistry.unregisterSavedStateProvider(key)
        }
    }
}

/**
 * [UiSavedStateRegistry] which can be disposed using [dispose].
 */
internal class DisposableUiSavedStateRegistry(
    uiSavedStateRegistry: UiSavedStateRegistry,
    private val onDispose: () -> Unit
) : UiSavedStateRegistry by uiSavedStateRegistry {

    fun dispose() {
        onDispose()
    }
}

/**
 * Checks that [value] can be stored inside [Bundle].
 */
private fun canBeSavedToBundle(value: Any): Boolean {
    for (cl in AcceptableClasses) {
        if (cl.isInstance(value)) {
            return true
        }
    }
    return false
}

/**
 * Contains Classes which can be stored inside [Bundle].
 *
 * Some of the classes are not added separately because:
 *
 * This classes implement Serializable:
 * - Arrays (DoubleArray, BooleanArray, IntArray, LongArray, ByteArray, FloatArray, ShortArray,
 * CharArray, Array<Parcelable, Array<String>)
 * - ArrayList
 * - Primitives (Boolean, Int, Long, Double, Float, Byte, Short, Char) will be boxed when casted
 * to Any, and all the boxed classes implements Serializable.
 * This class implements Parcelable:
 * - Bundle
 *
 * Note: it is simplified copy of the array from SavedStateHandle (lifecycle-viewmodel-savedstate).
 */
private val AcceptableClasses = arrayOf(
    Serializable::class.java,
    Parcelable::class.java,
    CharSequence::class.java,
    SparseArray::class.java,
    Binder::class.java,
    Size::class.java,
    SizeF::class.java
)

private fun Bundle.toMap(): Map<String, List<Any?>>? {
    val map = mutableMapOf<String, List<Any?>>()
    this.keySet().forEach { key ->
        map[key] = getParcelableArrayList<Parcelable?>(key) as List<Any?>
    }
    return map
}

private fun Map<String, List<Any?>>.toBundle(): Bundle {
    val bundle = Bundle()
    forEach { (key, list) ->
        val arrayList = if (list is ArrayList<*>) list else ArrayList(list)
        @Suppress("UNCHECKED_CAST")
        bundle.putParcelableArrayList(
            key,
            arrayList as ArrayList<Parcelable?>
        )
    }
    return bundle
}
