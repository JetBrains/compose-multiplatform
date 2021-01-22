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

package androidx.compose.ui.inspection.compose

import android.view.View
import androidx.compose.ui.inspection.proto.StringTable
import androidx.compose.ui.inspection.proto.toComposableNodes
import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.compose.ui.tooling.inspector.LayoutInspectorTree
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableRoot

/**
 * Returns true if this view represents a special type that bridges between the legacy UI
 * framework and Jetpack Compose.
 *
 * Note: AndroidComposeView lives in compose.ui but is internal, which is why we need to check
 * indirectly like this. TODO(b/177998085): Expose this class to our library.
 */
private fun View.isAndroidComposeView(): Boolean {
    return javaClass.canonicalName == "androidx.compose.ui.platform.AndroidComposeView"
}

/**
 * The `AndroidComposeView` class inside the compose library is internal, so we make our own fake
 * class there that wraps a normal [View], verifies it's the expected type, and exposes compose
 * related data that we care about.
 *
 * As this class extracts information about the view it's targeting, it must be instantiated on the
 * UI thread.
 */
class AndroidComposeViewWrapper(private val composeView: View, skipSystemComposables: Boolean) {
    companion object {
        fun tryCreateFor(view: View, skipSystemComposables: Boolean): AndroidComposeViewWrapper? {
            return if (view.isAndroidComposeView()) {
                AndroidComposeViewWrapper(view, skipSystemComposables)
            } else {
                null
            }
        }
    }

    init {
        ThreadUtils.assertOnMainThread()
        check(composeView.isAndroidComposeView())
    }

    val inspectorNodes = LayoutInspectorTree().apply {
        this.hideSystemNodes = skipSystemComposables
    }.convert(composeView)

    fun createComposableRoot(stringTable: StringTable): ComposableRoot {
        ThreadUtils.assertOnMainThread()

        return ComposableRoot.newBuilder().apply {
            viewId = composeView.uniqueDrawingId
            addAllNodes(inspectorNodes.toComposableNodes(stringTable))
        }.build()
    }
}