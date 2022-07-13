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

package androidx.compose.ui.modifier

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A Modifier that can be used to provide [ModifierLocal]s that can be read by other modifiers to
 * the right of this modifier, or modifiers that are children of the layout node that this
 * modifier is attached to.
 */
@Stable
@JvmDefaultWithCompatibility
interface ModifierLocalProvider<T> : Modifier.Element {
    /**
     * Each [ModifierLocalProvider] stores a [ModifierLocal] instance that can be used as a key
     * by a [ModifierLocalConsumer] to read the provided value.
     */
    val key: ProvidableModifierLocal<T>

    /**
     * The provided value, that can be read by modifiers on the right of this modifier, and
     * modifiers added to children of the composable using this modifier.
     */
    val value: T
}

/**
 * A Modifier that can be used to provide [ModifierLocal]s that can be read by other modifiers to
 * the right of this modifier, or modifiers that are children of the layout node that this
 * modifier is attached to.
 */
@ExperimentalComposeUiApi
fun <T> Modifier.modifierLocalProvider(key: ProvidableModifierLocal<T>, value: () -> T): Modifier {
    return this.then(
        object : ModifierLocalProvider<T>,
            InspectorValueInfo(
                debugInspectorInfo {
                    name = "modifierLocalProvider"
                    properties["key"] = key
                    properties["value"] = value
                }
            ) {
            override val key: ProvidableModifierLocal<T> = key
            override val value by derivedStateOf(value)
        }
    )
}