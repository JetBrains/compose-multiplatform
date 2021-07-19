/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JPanel

internal class PreviewPanel : JPanel() {
    private var image: BufferedImage? = null
    private var imageDimension: Dimension? = null

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        synchronized(this) {
            image?.let { image ->
                val w = imageDimension!!.width
                val h = imageDimension!!.height
                g.color = Color.WHITE
                g.fillRect(0, 0, w, h)
                g.drawImage(image, 0, 0, w, h, null)
            }
        }
    }

    fun previewImage(image: BufferedImage, imageDimension: Dimension) {
        synchronized(this) {
            this.image = image
            this.imageDimension = imageDimension
        }

        repaint()
    }

    override fun getPreferredSize(): Dimension? =
        imageDimension ?: super.getPreferredSize()
}