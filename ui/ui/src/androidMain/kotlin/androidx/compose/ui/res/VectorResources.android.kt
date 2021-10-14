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

import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.compat.AndroidVectorParser
import androidx.compose.ui.graphics.vector.compat.createVectorImageBuilder
import androidx.compose.ui.graphics.vector.compat.isAtEnd
import androidx.compose.ui.graphics.vector.compat.parseCurrentVectorNode
import androidx.compose.ui.graphics.vector.compat.seekToStartTag
import androidx.compose.ui.platform.LocalContext
import org.xmlpull.v1.XmlPullParserException
import java.lang.ref.WeakReference

/**
 * Load an ImageVector from a vector resource.
 *
 * This function is intended to be used for when low-level ImageVector-specific
 * functionality is required.  For simply displaying onscreen, the vector/bitmap-agnostic
 * [painterResource] is recommended instead.
 *
 * @param id the resource identifier
 * @return the vector data associated with the resource
 */
@Composable
fun ImageVector.Companion.vectorResource(@DrawableRes id: Int): ImageVector {
    val context = LocalContext.current
    val res = context.resources
    val theme = context.theme
    return remember(id) {
        vectorResource(theme, res, id)
    }
}

@Throws(XmlPullParserException::class)
fun ImageVector.Companion.vectorResource(
    theme: Resources.Theme? = null,
    res: Resources,
    resId: Int,
): ImageVector =
    loadVectorResourceInner(theme, res, res.getXml(resId).apply { seekToStartTag() }).imageVector

/**
 * Helper method that parses a vector asset from the given [XmlResourceParser] position.
 * This method assumes the parser is already been positioned to the start tag
 */
@Throws(XmlPullParserException::class)
@SuppressWarnings("RestrictedApi")
internal fun loadVectorResourceInner(
    theme: Resources.Theme? = null,
    res: Resources,
    parser: XmlResourceParser
): ImageVectorCache.ImageVectorEntry {
    val attrs = Xml.asAttributeSet(parser)
    val resourceParser = AndroidVectorParser(parser)
    val builder = resourceParser.createVectorImageBuilder(res, theme, attrs)

    var nestedGroups = 0
    while (!parser.isAtEnd()) {
        nestedGroups = resourceParser.parseCurrentVectorNode(
            res,
            attrs,
            theme,
            builder,
            nestedGroups
        )
        parser.next()
    }
    return ImageVectorCache.ImageVectorEntry(builder.build(), resourceParser.config)
}

/**
 * Object responsible for caching [ImageVector] instances
 * based on the given theme and drawable resource identifier
 */
internal class ImageVectorCache {

    /**
     * Key that binds the corresponding theme with the resource identifier for the vector asset
     */
    data class Key(
        val theme: Resources.Theme,
        val id: Int
    )

    /**
     * Tuple that contains the [ImageVector] as well as the corresponding configuration flags
     * that the [ImageVector] depends on. That is if there is a configuration change that updates
     * the parameters in the flag, this vector should be regenerated from the current configuration
     */
    data class ImageVectorEntry(
        val imageVector: ImageVector,
        val configFlags: Int
    )

    private val map = HashMap<Key, WeakReference<ImageVectorEntry>>()

    operator fun get(key: Key): ImageVectorEntry? = map[key]?.get()

    fun prune(configChanges: Int) {
        val it = map.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val imageVectorEntry = entry.value.get()
            if (imageVectorEntry == null ||
                Configuration.needNewResources(configChanges, imageVectorEntry.configFlags)
            ) {
                it.remove()
            }
        }
    }

    operator fun set(key: Key, imageVectorEntry: ImageVectorEntry) {
        map[key] = WeakReference<ImageVectorEntry>(imageVectorEntry)
    }

    fun clear() {
        map.clear()
    }
}