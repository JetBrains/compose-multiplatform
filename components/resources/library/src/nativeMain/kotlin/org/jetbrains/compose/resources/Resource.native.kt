/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.parse

internal actual fun parseXML(byteArray: ByteArray): Element = parse(byteArray.decodeToString())

actual typealias ResourcesRawResult = ByteArray
actual typealias ResourcesRawImageResult = ByteArray
internal actual fun ResourcesRawImageResult.rawToImageBitmap(): ImageBitmap = this.toImageBitmap()

actual suspend fun ResourcesRawResult.asResourcesRawImageResult(): ResourcesRawImageResult {
    return this
}
