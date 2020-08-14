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

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistryAmbient
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticAmbientOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val ConfigurationAmbient = ambientOf<Configuration>(
    @OptIn(ExperimentalComposeApi::class)
    neverEqualPolicy()
)

/**
 * Provides a [Context] that can be used by Android applications.
 */
val ContextAmbient = staticAmbientOf<Context>()

/**
 * The ambient containing the current [LifecycleOwner].
 */
val LifecycleOwnerAmbient = staticAmbientOf<LifecycleOwner>()

/**
 * The ambient containing the current Compose [View].
 */
val ViewAmbient = staticAmbientOf<View>()

/**
 * The ambient containing the current [ViewModelStoreOwner].
 */
val ViewModelStoreOwnerAmbient = staticAmbientOf<ViewModelStoreOwner>()

@Composable
internal fun ProvideAndroidAmbients(owner: AndroidOwner, content: @Composable () -> Unit) {
    val view = owner.view
    val context = view.context

    var configuration by remember {
        mutableStateOf(
            context.resources.configuration,
            @OptIn(ExperimentalComposeApi::class)
            neverEqualPolicy()
        )
    }

    owner.configurationChangeObserver = { configuration = it }

    val uriHandler = remember { AndroidUriHandler(context) }
    val viewTreeOwners = owner.viewTreeOwners ?: throw IllegalStateException(
        "Called when the ViewTreeOwnersAvailability is not yet in Available state"
    )

    val uiSavedStateRegistry = remember {
        DisposableUiSavedStateRegistry(view, viewTreeOwners.savedStateRegistryOwner)
    }
    onDispose {
        uiSavedStateRegistry.dispose()
    }

    Providers(
        ConfigurationAmbient provides configuration,
        ContextAmbient provides context,
        LifecycleOwnerAmbient provides viewTreeOwners.lifecycleOwner,
        UiSavedStateRegistryAmbient provides uiSavedStateRegistry,
        ViewAmbient provides owner.view,
        ViewModelStoreOwnerAmbient provides viewTreeOwners.viewModelStoreOwner
    ) {
        ProvideCommonAmbients(
            owner = owner,
            uriHandler = uriHandler,
            content = content
        )
    }
}
