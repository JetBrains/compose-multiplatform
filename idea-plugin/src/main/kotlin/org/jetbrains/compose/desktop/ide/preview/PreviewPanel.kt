/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JPanel

internal class PreviewPanel : JPanel() {
    private var image: BufferedImage? = null

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        synchronized(this) {
            image?.let { image ->
                g.color = Color.WHITE
                g.fillRect(0, 0, image.width , image.height)
                g.drawImage(image, 0, 0, image.width, image.height, null)
            }
        }
    }

    @Synchronized
    fun previewImage(newImage: BufferedImage) {
        synchronized(this) {
            image = newImage
        }

        repaint()
    }
}