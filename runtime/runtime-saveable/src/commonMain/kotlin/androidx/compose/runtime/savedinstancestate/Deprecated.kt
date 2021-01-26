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

@file:Suppress(
    "DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER", "DEPRECATION_ERROR", "DEPRECATION"
)

package androidx.compose.runtime.savedinstancestate

import androidx.compose.runtime.Composable

@Deprecated(
    "It was moved to androidx.compose.runtime.saveable package",
    level = DeprecationLevel.ERROR
)
@Suppress("DocumentExceptions")
fun <Original : Any, Saveable : Any> listSaver(
    save: SaverScope.(value: Original) -> List<Saveable>,
    restore: (list: List<Saveable>) -> Original?
): Saver<Original, *> = throw IllegalStateException(
    "This method is deprecated and moved to androidx.compose.runtime.saveable package"
)

@Deprecated(
    "It was moved to androidx.compose.runtime.saveable package",
    level = DeprecationLevel.ERROR
)
@Suppress("DocumentExceptions")
fun <T : Any> mapSaver(
    save: SaverScope.(value: T) -> Map<String, Any>,
    restore: (Map<String, Any>) -> T?
): Saver<T, *> = throw IllegalStateException(
    "This method is deprecated and moved to androidx.compose.runtime.saveable package"
)

@Deprecated(
    "It was moved to androidx.compose.runtime.saveable package",
    level = DeprecationLevel.ERROR
)
interface Saver<Original, Saveable : Any> :
    androidx.compose.runtime.saveable.Saver<Original, Saveable>

@Deprecated(
    "It was moved to androidx.compose.runtime.saveable package",
    level = DeprecationLevel.ERROR
)
@Suppress("DocumentExceptions")
fun <Original, Saveable : Any> Saver(
    save: SaverScope.(value: Original) -> Saveable?,
    restore: (value: Saveable) -> Original?
): Saver<Original, Saveable> = throw IllegalStateException(
    "This method was moved to androidx.compose.runtime.saveable package"
)

@Deprecated(
    "It was moved to androidx.compose.runtime.saveable package",
    level = DeprecationLevel.ERROR
)
interface SaverScope : androidx.compose.runtime.saveable.SaverScope

@Deprecated(
    "It was moved to androidx.compose.runtime.saveable package",
    level = DeprecationLevel.ERROR
)
@Suppress("DocumentExceptions")
fun <T> autoSaver(): Saver<T, Any> = throw IllegalStateException(
    "This method was moved to androidx.compose.runtime.saveable package"
)

@Deprecated("This annotation is going to be removed")
annotation class ExperimentalRestorableStateHolder

@Deprecated(
    "It was renamed to SaveableStateHolder and moved to androidx.compose.runtime.saveable package",
    ReplaceWith(
        "SaveableStateHolder",
        "androidx.compose.runtime.saveable.SaveableStateHolder"
    ),
    level = DeprecationLevel.ERROR
)
interface RestorableStateHolder<T : Any> :
    androidx.compose.runtime.saveable.SaveableStateHolder

@Deprecated(
    "It was renamed to moved to androidx.compose.runtime.saveable package",
    ReplaceWith(
        "rememberSaveableStateHolder()",
        "androidx.compose.runtime.saveable.rememberSaveableStateHolder"
    ),
    level = DeprecationLevel.ERROR
)
@Suppress("DocumentExceptions")
@Composable
fun <T : Any> rememberRestorableStateHolder(): RestorableStateHolder<T> =
    throw IllegalStateException(
        "This method is deprecated and moved to androidx.compose.runtime.saveable package"
    )
