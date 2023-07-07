/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.integration.demos.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager

/**
 * Returns a [State] that reads the shared preference with the given [key] from the [LocalContext].
 * The state will be automatically updated whenever the shared preference changes.
 */
@Composable
internal fun <T> preferenceAsState(
    key: String,
    readValue: SharedPreferences.() -> T
): State<T> {
    val context = LocalContext.current
    val sharedPreferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // De-duplicate passing keys explicitly to remembers and effects below.
    return key(key, readValue, sharedPreferences) {
        val value = remember { mutableStateOf(sharedPreferences.readValue()) }

        // Update value when preference changes.
        DisposableEffect(Unit) {
            val listener = OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    value.value = sharedPreferences.readValue()
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        // Also update the value when resumed.
        DisposableEffect(lifecycle) {
            val obs = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    value.value = sharedPreferences.readValue()
                }
            }
            lifecycle.addObserver(obs)
            onDispose {
                lifecycle.removeObserver(obs)
            }
        }

        return@key value
    }
}