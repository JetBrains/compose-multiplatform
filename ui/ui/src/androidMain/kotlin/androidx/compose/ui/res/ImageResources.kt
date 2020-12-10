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

import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.imageFromResource
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.util.trace

/**
 * Synchronously load an image resource.
 *
 * Note: This API is transient and will be likely removed for encouraging async resource loading.
 *
 * For loading generic loading of rasterized or vector assets see [painterResource]
 *
 * @param id the resource identifier
 * @return the decoded image data associated with the resource
 */
@Composable
fun imageResource(@DrawableRes id: Int): ImageBitmap {
    val context = AmbientContext.current
    val value = remember { TypedValue() }
    context.resources.getValue(id, value, true)
    // We use the file path as a key of the request cache.
    // TODO(nona): Add density to the key?
    val key = value.string!!.toString() // image resource must have resource path.
    return remember(key) { imageFromResource(context.resources, id) }
}

/**
 * Load the image in background thread.
 *
 * Until resource loading complete, this function returns deferred image resource with
 * [PendingResource]. Once the loading finishes, recompose is scheduled and this function will
 * return deferred image resource with [LoadedResource] or [FailedResource].
 *
 * @param id the resource identifier
 * @param pendingImage an optional image to be used during loading instead.
 * @param failedImage an optional image to be used if image loading failed.
 * @return the deferred image resource.
 */
@Composable
fun loadImageResource(
    id: Int,
    pendingImage: ImageBitmap? = null,
    failedImage: ImageBitmap? = null
): DeferredResource<ImageBitmap> {
    val context = AmbientContext.current
    val res = context.resources
    val value = remember { TypedValue() }
    res.getValue(id, value, true)
    val key = value.string!!.toString() // image resource must have resource path.
    return loadResource(key, pendingImage, failedImage) {
        trace("Image Resource Loading") {
            imageFromResource(res, id)
        }
    }
}