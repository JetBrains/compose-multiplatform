/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Get and remember resource.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageBitmap(): ImageBitmap = rememberImageBitmapAsync() ?: remember { ImageBitmap(1, 1) }

/**
 * Get and remember resource. While loading and if resource not exists result will be null.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageBitmapAsync(): ImageBitmap? {
    val state = remember(this) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(this) {
        state.value = readBytes().toImageBitmap()
    }
    return state.value
}

internal expect fun ByteArray.toImageBitmap(): ImageBitmap
