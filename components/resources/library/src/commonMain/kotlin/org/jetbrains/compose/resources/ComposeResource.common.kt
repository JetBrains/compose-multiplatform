/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.parseVectorRoot
import androidx.compose.ui.unit.dp

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

private val emptyImageVector: ImageVector by lazy {
    ImageVector.Builder(defaultWidth = 1.dp, defaultHeight = 1.dp, viewportWidth = 1f, viewportHeight = 1f).build()
}

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
 * Get and remember resource. While loading and if resource not exists result will be null.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageVector(density: Density): LoadState<ImageVector> {
    val state: MutableState<LoadState<ImageVector>> = remember(this, density) { mutableStateOf(LoadState.Loading()) }
    LaunchedEffect(this, density) {
        state.value = try {
            LoadState.Success(readBytes().toImageVector(density))
        } catch (e: Exception) {
            LoadState.Error(e)
        }
    }
    return state.value
}

private fun <T> LoadState<T>.orEmpty(emptyValue: T): T = when (this) {
    is LoadState.Loading -> emptyValue
    is LoadState.Success -> this.value
    is LoadState.Error -> emptyValue
}

/**
 * Return current ImageBitmap or return empty while loading.
 */
@ExperimentalResourceApi
fun LoadState<ImageBitmap>.orEmpty(): ImageBitmap = orEmpty(emptyImageBitmap)

/**
 * Return current ImageVector or return empty while loading.
 */
@ExperimentalResourceApi
fun LoadState<ImageVector>.orEmpty(): ImageVector = orEmpty(emptyImageVector)


@OptIn(ExperimentalResourceApi::class)
@Composable
private fun Resource.rememberImageBitmapSync(): ImageBitmap = remember(this) {
    readBytesSync().toImageBitmap()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun Resource.rememberImageVectorSync(density: Density): ImageVector = remember(this, density) {
    readBytesSync().toImageVector(density)
}


@OptIn(ExperimentalResourceApi::class)
@Composable
private fun painterResource(
    res: String,
    rememberImageBitmap: @Composable Resource.() -> ImageBitmap,
    rememberImageVector: @Composable Resource.(Density) -> ImageVector
): Painter =
    if (res.endsWith(".xml")) {
        rememberVectorPainter(resource(res).rememberImageVector(LocalDensity.current))
    } else {
        BitmapPainter(resource(res).rememberImageBitmap())
    }

/**
 * Return a Painter from the given resource path.
 * Can load either a BitmapPainter for rasterized images (.png, .jpg) or
 * a VectorPainter for XML Vector Drawables (.xml).
 *
 * XML Vector Drawables have the same format as for Android
 * (https://developer.android.com/reference/android/graphics/drawable/VectorDrawable)
 * except that external references to Android resources are not supported.
 *
 * Note that XML Vector Drawables are not supported for Web target currently.
 */
@ExperimentalResourceApi
@Composable
fun painterResource(res: String): Painter =
    if (isSyncResourceLoadingSupported()) {
        painterResource(res, {rememberImageBitmapSync()}, {density->rememberImageVectorSync(density)})
    } else {
        painterResource(res, {rememberImageBitmap().orEmpty()}, {density->rememberImageVector(density).orEmpty()})
    }


internal expect fun isSyncResourceLoadingSupported(): Boolean

@OptIn(ExperimentalResourceApi::class)
internal expect fun Resource.readBytesSync(): ByteArray

internal expect fun ByteArray.toImageBitmap(): ImageBitmap

internal expect fun parseXML(byteArray: ByteArray): Element

internal fun ByteArray.toImageVector(density: Density): ImageVector = parseXML(this).parseVectorRoot(density)
