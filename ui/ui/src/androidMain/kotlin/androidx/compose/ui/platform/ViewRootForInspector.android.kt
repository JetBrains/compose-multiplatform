/*
 * Copyright 2021 The Android Open Source Project
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
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * The marker interface to be implemented by [View]s that are initialized from Compose.
 * Examples are: DialogWrapper, PopupLayout, ViewFactoryHolder.
 * To be used by the inspector.
 */
@JvmDefaultWithCompatibility
interface ViewRootForInspector {

    /**
     * Return the [AbstractComposeView] if this is creating for a sub composition.
     *
     * This allows the inspector to add the creating compose nodes to the sub composition.
     */
    val subCompositionView: AbstractComposeView?
        get() = null

    /**
     * Return the top view initialized from Compose.
     *
     * This allows the inspector to place the view under the correct compose node.
     */
    val viewRoot: View?
        get() = null
}
