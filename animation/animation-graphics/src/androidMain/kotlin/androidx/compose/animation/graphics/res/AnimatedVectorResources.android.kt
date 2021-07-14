/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.animation.graphics.res

import android.content.res.Resources
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.graphics.vector.compat.parseAnimatedImageVector
import androidx.compose.animation.graphics.vector.compat.seekToStartTag
import androidx.compose.ui.platform.LocalContext
import org.xmlpull.v1.XmlPullParserException

/**
 * Load an [AnimatedImageVector] from an Android resource id.
 *
 * Note: This API is transient and will be likely removed for encouraging async resource loading.
 *
 * @param id the resource identifier
 * @return an animated vector drawable resource.
 *
 * @sample androidx.compose.animation.graphics.samples.AnimatedVectorSample
 */
@ExperimentalAnimationGraphicsApi
@Composable
fun animatedVectorResource(@DrawableRes id: Int): AnimatedImageVector {
    val context = LocalContext.current
    val res = context.resources
    val theme = context.theme
    return remember(id) {
        loadAnimatedVectorResource(theme, res, id)
    }
}

@ExperimentalAnimationGraphicsApi
@Throws(XmlPullParserException::class)
internal fun loadAnimatedVectorResource(
    theme: Resources.Theme? = null,
    res: Resources,
    resId: Int
): AnimatedImageVector {
    val parser = res.getXml(resId).seekToStartTag()
    val attrs = Xml.asAttributeSet(parser)
    return parser.parseAnimatedImageVector(res, theme, attrs)
}
