/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.res

import android.content.res.Resources
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Load a string resource.
 *
 * @param id the resource identifier
 * @return the string data associated with the resource
 */
@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int): String {
    val resources = resources()
    return resources.getString(id)
}

/**
 * Load a string resource with formatting.
 *
 * @param id the resource identifier
 * @param formatArgs the format arguments
 * @return the string data associated with the resource
 */
@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val resources = resources()
    return resources.getString(id, *formatArgs)
}

/**
 * Load a string resource.
 *
 * @param id the resource identifier
 * @return the string data associated with the resource
 */
@Composable
@ReadOnlyComposable
fun stringArrayResource(@ArrayRes id: Int): Array<String> {
    val resources = resources()
    return resources.getStringArray(id)
}

/**
 * Load a plurals resource.
 *
 * @param id the resource identifier
 * @param count the count
 * @return the pluralized string data associated with the resource
 */
@ExperimentalComposeUiApi
@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int): String {
    val resources = resources()
    return resources.getQuantityString(id, count)
}

/**
 * Load a plurals resource with provided format arguments.
 *
 * @param id the resource identifier
 * @param count the count
 * @param formatArgs arguments used in the format string
 * @return the pluralized string data associated with the resource
 */
@ExperimentalComposeUiApi
@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String {
    val resources = resources()
    return resources.getQuantityString(id, count, *formatArgs)
}

/**
 * A composable function that returns the [Resources]. It will be recomposed when [Configuration]
 * gets updated.
 */
@Composable
@ReadOnlyComposable
private fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}
