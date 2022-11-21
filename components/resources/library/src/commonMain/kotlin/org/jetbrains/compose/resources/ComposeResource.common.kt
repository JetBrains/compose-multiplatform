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
 * Get and remember resource. While loading and if resource not exists result will be null.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageBitmap(): ImageBitmap? {
    val state = remember(this) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(this) {
        val loadingBytesState = readBytes()
        if (loadingBytesState is LoadState.Success) {
            state.value = loadingBytesState.value.toImageBitmap()
        }
    }
    return state.value
}

/**
 * return current ImageBitmap or return empty while loading
 */
@ExperimentalResourceApi
@Composable
inline fun ImageBitmap?.orEmpty(): ImageBitmap = this ?: remember { ImageBitmap(1, 1) }

internal expect fun ByteArray.toImageBitmap(): ImageBitmap
