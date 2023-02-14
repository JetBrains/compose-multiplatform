/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

/**
 * Get and remember resource. While loading and if resource not exists result will be null.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageBitmap(): LoadState<ImageBitmap> {
    val state: MutableState<LoadState<ImageBitmap>> = remember(this) { mutableStateOf(LoadState.Loading()) }
    LaunchedEffect(this) {
        state.value = try {
            LoadState.Success(readBytes().toImageBitmap())
        } catch (e: Exception) {
            LoadState.Error(e)
        }
    }
    return state.value
}

/**
 * return current ImageBitmap or return empty while loading
 */
@ExperimentalResourceApi
fun LoadState<ImageBitmap>.orEmpty(): ImageBitmap = when (this) {
    is LoadState.Loading -> emptyImageBitmap
    is LoadState.Success -> this.value
    is LoadState.Error -> emptyImageBitmap
}

internal expect fun ByteArray.toImageBitmap(): ImageBitmap
