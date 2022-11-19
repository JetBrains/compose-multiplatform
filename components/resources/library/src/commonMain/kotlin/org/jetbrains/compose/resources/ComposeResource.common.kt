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
import kotlinx.coroutines.runBlocking

/**
 * Get and remember resource in a blocking way.
 * May cause performance issues when used on the main thread.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageBitmap(): ImageBitmap = remember(this) {
    runBlocking {
        readBytes().toImageBitmap()
    }
}

/**
 * Get and remember resource in an asynchronous way.
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
