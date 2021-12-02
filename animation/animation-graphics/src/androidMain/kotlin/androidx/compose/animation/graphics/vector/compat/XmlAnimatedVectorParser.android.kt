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

package androidx.compose.animation.graphics.vector.compat

import android.content.res.Resources
import android.util.AttributeSet
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.loadAnimatorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.graphics.vector.AnimatedVectorTarget
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.xmlpull.v1.XmlPullParser

private const val TagAnimatedVector = "animated-vector"
private const val TagAnimatedVectorTarget = "target"

private fun parseAnimatedVectorTarget(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): AnimatedVectorTarget {
    return attrs.attrs(
        res, theme, AndroidVectorResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET
    ) { a ->
        AnimatedVectorTarget(
            a.getString(
                AndroidVectorResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET_NAME
            ) ?: "",
            loadAnimatorResource(
                theme,
                res,
                a.getResourceId(
                    AndroidVectorResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET_ANIMATION,
                    0
                )
            )
        )
    }
}

@ExperimentalAnimationGraphicsApi
internal fun XmlPullParser.parseAnimatedImageVector(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): AnimatedImageVector {
    return attrs.attrs(res, theme, AndroidVectorResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE) { a ->
        val drawableId = a.getResourceId(
            AndroidVectorResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_DRAWABLE,
            0
        )
        val targets = mutableListOf<AnimatedVectorTarget>()
        forEachChildOf(TagAnimatedVector) {
            if (eventType == XmlPullParser.START_TAG && name == TagAnimatedVectorTarget) {
                targets.add(parseAnimatedVectorTarget(res, theme, attrs))
            }
        }
        AnimatedImageVector(
            ImageVector.vectorResource(theme, res, drawableId),
            targets
        )
    }
}
