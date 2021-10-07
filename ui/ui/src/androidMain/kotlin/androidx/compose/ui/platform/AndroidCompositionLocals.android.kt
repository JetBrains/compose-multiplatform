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

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ImageVectorCache
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val LocalConfiguration = compositionLocalOf<Configuration>(
    neverEqualPolicy()
) {
    noLocalProvidedFor("LocalConfiguration")
}

/**
 * Provides a [Context] that can be used by Android applications.
 */
val LocalContext = staticCompositionLocalOf<Context> {
    noLocalProvidedFor("LocalContext")
}

internal val LocalImageVectorCache = staticCompositionLocalOf<ImageVectorCache> {
    noLocalProvidedFor("LocalImageVectorCache")
}

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
val LocalLifecycleOwner = staticCompositionLocalOf<LifecycleOwner> {
    noLocalProvidedFor("LocalLifecycleOwner")
}

/**
 * The CompositionLocal containing the current [SavedStateRegistryOwner].
 */
val LocalSavedStateRegistryOwner = staticCompositionLocalOf<SavedStateRegistryOwner> {
    noLocalProvidedFor("LocalSavedStateRegistryOwner")
}

/**
 * The CompositionLocal containing the current Compose [View].
 */
val LocalView = staticCompositionLocalOf<View> {
    noLocalProvidedFor("LocalView")
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun ProvideAndroidCompositionLocals(
    owner: AndroidComposeView,
    content: @Composable () -> Unit
) {
    val view = owner
    val context = view.context
    var configuration by remember {
        mutableStateOf(
            context.resources.configuration,
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

    val imageVectorCache = obtainImageVectorCache(context, configuration)
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides context,
        LocalLifecycleOwner provides viewTreeOwners.lifecycleOwner,
        LocalSavedStateRegistryOwner provides viewTreeOwners.savedStateRegistryOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
        LocalView provides owner.view,
        LocalImageVectorCache provides imageVectorCache
    ) {
        ProvideCommonCompositionLocals(
            owner = owner,
            uriHandler = uriHandler,
            content = content
        )
    }
}

@Stable
@Composable
private fun obtainImageVectorCache(
    context: Context,
    configuration: Configuration?
): ImageVectorCache {
    val imageVectorCache = remember { ImageVectorCache() }
    var currentConfiguration = remember { configuration }
    val callbacks = remember {
        object : ComponentCallbacks2 {
            override fun onConfigurationChanged(configuration: Configuration) {
                // If there is no configuration, assume all flags have changed.
                val changedFlags = currentConfiguration?.updateFrom(configuration) ?: -0x1
                imageVectorCache.prune(changedFlags)
                currentConfiguration = configuration
            }

            override fun onLowMemory() {
                imageVectorCache.clear()
            }

            override fun onTrimMemory(level: Int) {
                imageVectorCache.clear()
            }
        }
    }
    DisposableEffect(imageVectorCache) {
        context.applicationContext.registerComponentCallbacks(callbacks)
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(callbacks)
        }
    }
    return imageVectorCache
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
