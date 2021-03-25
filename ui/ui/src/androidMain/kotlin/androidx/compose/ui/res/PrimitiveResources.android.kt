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

import androidx.annotation.ArrayRes
import androidx.annotation.BoolRes
import androidx.annotation.DimenRes
import androidx.annotation.IntegerRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Load an integer resource.
 *
 * @param id the resource identifier
 * @return the integer associated with the resource
 */
@Composable
@ReadOnlyComposable
fun integerResource(@IntegerRes id: Int): Int {
    val context = LocalContext.current
    return context.resources.getInteger(id)
}

/**
 * Load an array of integer resource.
 *
 * @param id the resource identifier
 * @return the integer array associated with the resource
 */
@Composable
@ReadOnlyComposable
fun integerArrayResource(@ArrayRes id: Int): IntArray {
    val context = LocalContext.current
    return context.resources.getIntArray(id)
}

/**
 * Load a boolean resource.
 *
 * @param id the resource identifier
 * @return the boolean associated with the resource
 */
@Composable
@ReadOnlyComposable
fun booleanResource(@BoolRes id: Int): Boolean {
    val context = LocalContext.current
    return context.resources.getBoolean(id)
}

/**
 * Load a dimension resource.
 *
 * @param id the resource identifier
 * @return the dimension value associated with the resource
 */
@Composable
@ReadOnlyComposable
fun dimensionResource(@DimenRes id: Int): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    val pxValue = context.resources.getDimension(id)
    return Dp(pxValue / density.density)
}
