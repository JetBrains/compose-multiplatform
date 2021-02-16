package org.jetbrains.codeviewer

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import org.jetbrains.codeviewer.ui.MainView
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() = Window(
    title = "Code Viewer",
    size = IntSize(1280, 768),
    icon = loadImageResource("ic_launcher.png"),
) {
    MainView()
}


@Suppress("SameParameterValue")
private fun loadImageResource(path: String): BufferedImage {
    val resource = Thread.currentThread().contextClassLoader.getResource(path)
    requireNotNull(resource) { "Resource $path not found" }
    return resource.openStream().use(ImageIO::read)
}
