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
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Load an ImageBitmap from an image resource.
 *
 * This function is intended to be used for when low-level ImageBitmap-specific
 * functionality is required.  For simply displaying onscreen, the vector/bitmap-agnostic
 * [painterResource] is recommended instead.
 *
 * @return Loaded image file represented as an [ImageBitmap]
 */
fun ImageBitmap.Companion.imageResource(res: Resources, @DrawableRes id: Int): ImageBitmap {
    return (res.getDrawable(id, null) as BitmapDrawable).bitmap.asImageBitmap()
}

/**
 * Load an ImageBitmap from an image resource.
 *
 * This function is intended to be used for when low-level ImageBitmap-specific
 * functionality is required.  For simply displaying onscreen, the vector/bitmap-agnostic
 * [painterResource] is recommended instead.
 *
 * @param id the resource identifier
 * @return the decoded image data associated with the resource
 */
@Composable
fun ImageBitmap.Companion.imageResource(@DrawableRes id: Int): ImageBitmap {
    val context = LocalContext.current
    val value = remember { TypedValue() }
    context.resources.getValue(id, value, true)
    val key = value.string!!.toString() // image resource must have resource path.
    return remember(key) { imageResource(context.resources, id) }
}
