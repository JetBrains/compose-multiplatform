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
import kotlin.jvm.JvmName

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

private val emptyImageVector: ImageVector by lazy {
    ImageVector.Builder(defaultWidth = 1.dp, defaultHeight = 1.dp, viewportWidth = 1f, viewportHeight = 1f).build()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun <T> Resource.rememberLoadingResource(fromByteArrayConverter: ByteArray.()->T): LoadState<T> {
    val state: MutableState<LoadState<T>> = remember(this) { mutableStateOf(LoadState.Loading()) }
    LaunchedEffect(this) {
        state.value = try {
            LoadState.Success(readBytes().fromByteArrayConverter())
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
fun Resource.rememberImageBitmap(): LoadState<ImageBitmap> =
    rememberLoadingResource { toImageBitmap() }

/**
 * Get and remember resource. While loading and if resource not exists result will be null.
 */
@ExperimentalResourceApi
@Composable
fun Resource.rememberImageVector(density: Density): LoadState<ImageVector> =
    rememberLoadingResource { toImageVector(density) }

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

/**
 * Return a Painter from the given resource path.
 * Can load either a BitmapPainter for rasterized images (.png, .jpg) or
 * a VectorPainter for XML Vector Drawables (.xml).
 *
 * XML Vector Drawables have the same format as for Android
 * (https://developer.android.com/reference/android/graphics/drawable/VectorDrawable)
 * except that external references to Android resources are not supported.
 */
@ExperimentalResourceApi
@Composable
fun painterResource(res: String): Painter {
    if (res.endsWith(".xml")) {
        return rememberVectorPainter(resource(res).rememberImageVector(LocalDensity.current).orEmpty())
    }

    return BitmapPainter(resource(res).rememberImageBitmap().orEmpty())
}

/**
 * Convert LoadState<ImageVector> to Painter
 */
@ExperimentalResourceApi
@Composable
@JvmName("loadStateImageVectorToPainter")
fun LoadState<ImageVector>.toPainter(): Painter = rememberVectorPainter(orEmpty())

/**
 * Convert LoadState<ImageBitmap> to Painter
 */
@ExperimentalResourceApi
@Composable
@JvmName("loadStateImageBitmapToPainter")
fun LoadState<ImageBitmap>.toPainter(): Painter {
    val imageBitmap = orEmpty()
    return remember(imageBitmap) { BitmapPainter(imageBitmap) }
}

internal expect fun ByteArray.toImageBitmap(): ImageBitmap

internal expect fun parseXML(byteArray: ByteArray): Element

internal fun ByteArray.toImageVector(density: Density): ImageVector = parseXML(this).parseVectorRoot(density)
