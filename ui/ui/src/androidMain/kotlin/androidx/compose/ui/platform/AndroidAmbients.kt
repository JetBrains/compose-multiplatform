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
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.rootAnimationClockFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Providers
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
@Deprecated(
    "Renamed to LocalConfiguration",
    replaceWith = ReplaceWith(
        "LocalConfiguration",
        "androidx.compose.ui.platform.LocalConfiguration"
    )
)
val AmbientConfiguration get() = LocalConfiguration

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val LocalConfiguration = compositionLocalOf<Configuration>(
    @OptIn(ExperimentalComposeApi::class)
    neverEqualPolicy()
)

/**
 * Provides a [Context] that can be used by Android applications.
 */
@Deprecated(
    "Renamed to LocalContext",
    replaceWith = ReplaceWith(
        "LocalContext",
        "androidx.compose.ui.platform.LocalContext"
    )
)
val AmbientContext get() = LocalContext

/**
 * Provides a [Context] that can be used by Android applications.
 */
val LocalContext = staticCompositionLocalOf<Context>()

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
@Deprecated(
    "Renamed to LocalLifecycleOwner",
    replaceWith = ReplaceWith(
        "LocalLifecycleOwner",
        "androidx.compose.ui.platform.LocalLifecycleOwner"
    )
)
val AmbientLifecycleOwner get() = LocalLifecycleOwner

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
val LocalLifecycleOwner = staticCompositionLocalOf<LifecycleOwner>()

/**
 * The CompositionLocal containing the current [SavedStateRegistryOwner].
 */
@Deprecated(
    "Renamed to LocalSavedStateRegistryOwner",
    replaceWith = ReplaceWith(
        "LocalSavedStateRegistryOwner",
        "androidx.compose.ui.platform.LocalSavedStateRegistryOwner"
    )
)
val AmbientSavedStateRegistryOwner get() = LocalSavedStateRegistryOwner

/**
 * The CompositionLocal containing the current [SavedStateRegistryOwner].
 */
val LocalSavedStateRegistryOwner = staticCompositionLocalOf<SavedStateRegistryOwner>()

/**
 * The CompositionLocal containing the current Compose [View].
 */
@Deprecated(
    "Renamed to LocalView",
    replaceWith = ReplaceWith(
        "LocalView",
        "androidx.compose.ui.platform.LocalView"
    )
)
val AmbientView get() = LocalView

/**
 * The CompositionLocal containing the current Compose [View].
 */
val LocalView = staticCompositionLocalOf<View>()

/**
 * The CompositionLocal containing the current [ViewModelStoreOwner].
 */
@Deprecated(
    "Renamed to LocalViewModelStoreOwner",
    replaceWith = ReplaceWith(
        "LocalViewModelStoreOwner",
        "androidx.compose.ui.platform.LocalViewModelStoreOwner"
    )
)
val AmbientViewModelStoreOwner get() = LocalViewModelStoreOwner

/**
 * The CompositionLocal containing the current [ViewModelStoreOwner].
 */
val LocalViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner>()

@Composable
@OptIn(ExperimentalComposeUiApi::class, InternalAnimationApi::class)
internal fun ProvideAndroidCompositionLocals(
    owner: AndroidComposeView,
    content: @Composable () -> Unit
) {
    val view = owner
    val context = view.context
    val scope = rememberCoroutineScope()
    val rootAnimationClock = remember(scope) {
        rootAnimationClockFactory(scope)
    }

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

    val saveableStateRegistry = remember {
        DisposableSaveableStateRegistry(view, viewTreeOwners.savedStateRegistryOwner)
    }
    DisposableEffect(Unit) {
        onDispose {
            saveableStateRegistry.dispose()
        }
    }

    Providers(
        LocalConfiguration provides configuration,
        LocalContext provides context,
        LocalLifecycleOwner provides viewTreeOwners.lifecycleOwner,
        LocalSavedStateRegistryOwner provides viewTreeOwners.savedStateRegistryOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
        LocalView provides owner.view,
        LocalViewModelStoreOwner provides viewTreeOwners.viewModelStoreOwner
    ) {
        ProvideCommonCompositionLocals(
            owner = owner,
            animationClock = rootAnimationClock,
            uriHandler = uriHandler,
            content = content
        )
    }
}
