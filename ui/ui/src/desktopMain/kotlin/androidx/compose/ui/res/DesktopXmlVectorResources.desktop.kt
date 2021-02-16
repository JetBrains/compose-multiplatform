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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vector.parseVectorRoot
import androidx.compose.ui.unit.Density
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Synchronously load an xml vector image stored in resources for the application.
 *
 * XML Vector Image is came from Android world. See:
 * https://developer.android.com/guide/topics/graphics/vector-drawable-resources
 *
 * On desktop it is fully implemented except there is no resource linking
 * (for example, we can't reference to color defined in another file)
 *
 * SVG files can be converted to XML with Android Studio or with third party tools
 * (search "svg to xml" in Google)
 *
 * @param resourcePath path to the file in the resources folder
 * @return the decoded vector image associated with the resource
 */
@Composable
fun vectorXmlResource(resourcePath: String): ImageVector {
    val inputSource = remember(resourcePath) {
        object : InputSource() {
            override fun getByteStream() = openResourceStream(resourcePath)
        }
    }

    return vectorXmlResource(inputSource)
}

/**
 * Synchronously load an xml vector image from some [inputSource].
 *
 * XML Vector Image is came from Android world. See:
 * https://developer.android.com/guide/topics/graphics/vector-drawable-resources
 *
 * On desktop it is fully implemented except there is no resource linking
 * (for example, we can't reference to color defined in another file).
 *
 * SVG files can be converted to XML with Android Studio or with third party tools
 * (search "svg to xml" in Google)
 *
 * Example of usage:
 *
 * val inputSource = remember(url) {
 *     object : InputSource() {
 *         override fun getByteStream() = url.openStream()
 *     }
 * }
 *
 * vectorXmlResource(inputSource)
 *
 * @param inputSource input source to load xml resource. Will be closed automatically.
 * @return the decoded vector image associated with the resource
 */
@Composable
fun vectorXmlResource(inputSource: InputSource): ImageVector {
    val density = LocalDensity.current
    return remember(inputSource, density) {
        loadVectorXmlResource(inputSource, density)
    }
}

/**
 * Synchronously load an xml vector image from some [inputSource].
 *
 * In contrast to [vectorXmlResource] this function isn't [Composable]
 *
 * XML Vector Image is came from Android world. See:
 * https://developer.android.com/guide/topics/graphics/vector-drawable-resources
 *
 * On desktop it is fully implemented except there is no resource linking
 * (for example, we can't reference to color defined in another file)
 *
 * SVG files can be converted to XML with Android Studio or with third party tools
 * (search "svg to xml" in Google)
 *
 * @param inputSource input source to load xml resource. Will be closed automatically.
 * @return the decoded vector image associated with the resource
 */
fun loadVectorXmlResource(
    inputSource: InputSource,
    density: Density
): ImageVector = DocumentBuilderFactory
    .newInstance().apply {
        isNamespaceAware = true
    }
    .newDocumentBuilder()
    .parse(inputSource)
    .documentElement
    .parseVectorRoot(density)