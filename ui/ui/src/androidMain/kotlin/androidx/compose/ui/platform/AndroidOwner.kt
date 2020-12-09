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

import android.content.res.Configuration
import android.graphics.Canvas
import android.view.View
import androidx.annotation.RestrictTo
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.compose.ui.viewinterop.InternalInteropApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import org.jetbrains.annotations.TestOnly

/**
 * Interface to be implemented by [Owner]s able to handle Android View specific functionality.
 */
interface AndroidOwner : Owner {

    /**
     * The view backing this Owner.
     */
    val view: View

    /**
     * Called to inform the owner that a new Android [View] was [attached][Owner.onAttach]
     * to the hierarchy.
     */
    @OptIn(InternalInteropApi::class)
    fun addAndroidView(view: AndroidViewHolder, layoutNode: LayoutNode)

    /**
     * Called to inform the owner that an Android [View] was [detached][Owner.onDetach]
     * from the hierarchy.
     */
    @OptIn(InternalInteropApi::class)
    fun removeAndroidView(view: AndroidViewHolder)

    /**
     * Called to ask the owner to draw a child Android [View] to [canvas].
     */
    @OptIn(InternalInteropApi::class)
    fun drawAndroidView(view: AndroidViewHolder, canvas: Canvas)

    /**
     * Used for updating the ConfigurationAmbient when configuration changes - consume the
     * configuration ambient instead of changing this observer if you are writing a component
     * that adapts to configuration changes.
     */
    var configurationChangeObserver: (Configuration) -> Unit

    /**
     * Current [ViewTreeOwners]. Use [setOnViewTreeOwnersAvailable] if you want to
     * execute your code when the object will be created.
     */
    val viewTreeOwners: ViewTreeOwners?

    /**
     * The callback to be executed when [viewTreeOwners] is created and not-null anymore.
     * Note that this callback will be fired inline when it is already available
     */
    fun setOnViewTreeOwnersAvailable(callback: (ViewTreeOwners) -> Unit)

    /**
     * Called to invalidate the Android [View] sub-hierarchy handled by this Owner.
     */
    fun invalidateDescendants()

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Called after an [AndroidOwner] is created. Used by AndroidComposeTestRule to keep
         * track of all attached [AndroidComposeView]s. Not to be set or used by any other
         * component.
         */
        var onAndroidOwnerCreatedCallback: ((AndroidOwner) -> Unit)? = null
            @TestOnly
            set
    }

    /**
     * Combines objects populated via ViewTree*Owner
     */
    class ViewTreeOwners(
        /**
         * The [LifecycleOwner] associated with this owner.
         */
        val lifecycleOwner: LifecycleOwner,
        /**
         * The [ViewModelStoreOwner] associated with this owner.
         */
        val viewModelStoreOwner: ViewModelStoreOwner,
        /**
         * The [SavedStateRegistryOwner] associated with this owner.
         */
        val savedStateRegistryOwner: SavedStateRegistryOwner
    )
}
