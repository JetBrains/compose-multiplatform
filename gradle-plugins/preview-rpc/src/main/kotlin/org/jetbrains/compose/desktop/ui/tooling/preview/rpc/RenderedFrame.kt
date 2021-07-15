/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

data class RenderedFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderedFrame

        if (!bytes.contentEquals(other.bytes)) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    val image: BufferedImage
        get() = ByteArrayInputStream(bytes).use { ImageIO.read(it) }

    val dimension: Dimension
        get() = Dimension(width, height)
}