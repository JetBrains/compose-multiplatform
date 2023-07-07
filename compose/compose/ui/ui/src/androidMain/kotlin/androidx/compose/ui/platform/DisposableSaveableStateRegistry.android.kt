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

@file:Suppress("UNCHECKED_CAST")

package androidx.compose.ui.platform

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.R
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import java.io.Serializable

/**
 * Creates [DisposableSaveableStateRegistry] associated with these [view] and [owner].
 */
internal fun DisposableSaveableStateRegistry(
    view: View,
    owner: SavedStateRegistryOwner
): DisposableSaveableStateRegistry {
    // The view id of AbstractComposeView is used as a key for SavedStateRegistryOwner. If there
    // are multiple AbstractComposeViews in the same Activity/Fragment with the same id(or with
    // no id) this means only the first view will restore its state. There is also an internal
    // mechanism to provide such id not as an Int to avoid ids collisions via view's tag. This
    // api is currently internal to compose:ui, we will see in the future if we need to make a
    // new public api for that use case.
    val composeView = (view.parent as View)
    val idFromTag = composeView.getTag(R.id.compose_view_saveable_id_tag) as? String
    val id = idFromTag ?: composeView.id.toString()
    return DisposableSaveableStateRegistry(id, owner)
}

/**
 * Creates [DisposableSaveableStateRegistry] with the restored values using [SavedStateRegistry] and
 * saves the values when [SavedStateRegistry] performs save.
 *
 * To provide a namespace we require unique [id]. We can't use the default way of doing it when we
 * have unique id on [AbstractComposeView] because we dynamically create [AbstractComposeView]s and
 * there is no way to have a unique id given there are could be any number of
 * [AbstractComposeView]s inside the same Activity. If we use [View.generateViewId]
 * this id will not survive Activity recreation. But it is reasonable to ask our users to have an
 * unique id on [AbstractComposeView].
 */
internal fun DisposableSaveableStateRegistry(
    id: String,
    savedStateRegistryOwner: SavedStateRegistryOwner
): DisposableSaveableStateRegistry {
    val key = "${SaveableStateRegistry::class.java.simpleName}:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap()

    val saveableStateRegistry = SaveableStateRegistry(restored) {
        canBeSavedToBundle(it)
    }
    val registered = try {
        androidxRegistry.registerSavedStateProvider(key) {
            saveableStateRegistry.performSave().toBundle()
        }
        true
    } catch (ignore: IllegalArgumentException) {
        // this means there are two AndroidComposeViews composed into different parents with the
        // same view id. currently we will just not save/restore state for the second
        // AndroidComposeView.
        // TODO: we should verify our strategy for such cases and improve it. b/162397322
        false
    }
    return DisposableSaveableStateRegistry(saveableStateRegistry) {
        if (registered) {
            androidxRegistry.unregisterSavedStateProvider(key)
        }
    }
}

/**
 * [SaveableStateRegistry] which can be disposed using [dispose].
 */
internal class DisposableSaveableStateRegistry(
    saveableStateRegistry: SaveableStateRegistry,
    private val onDispose: () -> Unit
) : SaveableStateRegistry by saveableStateRegistry {

    fun dispose() {
        onDispose()
    }
}

/**
 * Checks that [value] can be stored inside [Bundle].
 */
private fun canBeSavedToBundle(value: Any): Boolean {
    // SnapshotMutableStateImpl is Parcelable, but we do extra checks
    if (value is SnapshotMutableState<*>) {
        if (value.policy === neverEqualPolicy<Any?>() ||
            value.policy === structuralEqualityPolicy<Any?>() ||
            value.policy === referentialEqualityPolicy<Any?>()
        ) {
            val stateValue = value.value
            return if (stateValue == null) true else canBeSavedToBundle(stateValue)
        } else {
            return false
        }
    }
    // lambdas in Kotlin implement Serializable, but will crash if you really try to save them.
    // we check for both Function and Serializable (see kotlin.jvm.internal.Lambda) to support
    // custom user defined classes implementing Function interface.
    if (value is Function<*> && value is Serializable) {
        return false
    }
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
    String::class.java,
    SparseArray::class.java,
    Binder::class.java,
    Size::class.java,
    SizeF::class.java
)

@Suppress("DEPRECATION")
private fun Bundle.toMap(): Map<String, List<Any?>>? {
    val map = mutableMapOf<String, List<Any?>>()
    this.keySet().forEach { key ->
        val list = getParcelableArrayList<Parcelable?>(key) as ArrayList<Any?>
        map[key] = list
    }
    return map
}

private fun Map<String, List<Any?>>.toBundle(): Bundle {
    val bundle = Bundle()
    forEach { (key, list) ->
        val arrayList = if (list is ArrayList<Any?>) list else ArrayList(list)
        bundle.putParcelableArrayList(
            key,
            arrayList as ArrayList<Parcelable?>
        )
    }
    return bundle
}
