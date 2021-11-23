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

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vector.parseVectorRoot
import androidx.compose.ui.unit.Density
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Synchronously load an xml vector image from some [inputSource].
 *
 * XML Vector Image came from Android world. See:
 * https://developer.android.com/guide/topics/graphics/vector-drawable-resources
 *
 * On desktop it is fully implemented except there is no resource linking
 * (for example, we can't reference to color defined in another file)
 *
 * @param inputSource input source to load xml vector image. Will be closed automatically.
 * @param density density that will be used to set the default size of the ImageVector. If the image
 * will be drawn with the specified size, density will have no effect.
 * @return the decoded vector image associated with the image
 */
fun loadXmlImageVector(
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
