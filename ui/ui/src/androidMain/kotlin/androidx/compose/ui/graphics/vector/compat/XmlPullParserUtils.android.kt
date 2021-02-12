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

package androidx.compose.ui.graphics.vector.compat

import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import org.xmlpull.v1.XmlPullParser

/**
 * Assuming that we are at the [XmlPullParser.START_TAG start] of the specified [tag], iterates
 * though the events until we see a corresponding [XmlPullParser.END_TAG].
 */
internal inline fun XmlPullParser.forEachChildOf(
    tag: String,
    action: XmlPullParser.() -> Unit
) {
    next()
    while (!isAtEnd()) {
        if (eventType == XmlPullParser.END_TAG && name == tag) {
            break
        }
        action()
        next()
    }
}

internal inline fun <T> AttributeSet.attrs(
    res: Resources,
    theme: Resources.Theme?,
    styleable: IntArray,
    body: (a: TypedArray) -> T
): T {
    val a = theme?.obtainStyledAttributes(this, styleable, 0, 0)
        ?: res.obtainAttributes(this, styleable)
    try {
        return body(a)
    } finally {
        a.recycle()
    }
}
