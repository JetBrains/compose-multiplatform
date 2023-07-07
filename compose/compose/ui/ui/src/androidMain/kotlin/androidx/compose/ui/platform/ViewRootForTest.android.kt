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

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.node.RootForTest

/**
 * The marker interface to be implemented by the [View] backing the composition.
 * To be used in tests.
 */
@VisibleForTesting
interface ViewRootForTest : RootForTest {

    /**
     * The view backing this Owner.
     */
    val view: View

    /**
     * Returns true when the associated LifecycleOwner is in the resumed state
     */
    val isLifecycleInResumedState: Boolean

    /**
     * Whether the Owner has pending layout work.
     */
    val hasPendingMeasureOrLayout: Boolean

    /**
     * Called to invalidate the Android [View] sub-hierarchy handled by this [View].
     */
    fun invalidateDescendants()

    companion object {
        /**
         * Called after an View implementing [ViewRootForTest] is created. Used by
         * AndroidComposeTestRule to keep track of all attached ComposeViews. Not to be
         * set or used by any other component.
         */
        @VisibleForTesting
        var onViewCreatedCallback: ((ViewRootForTest) -> Unit)? = null
    }
}
