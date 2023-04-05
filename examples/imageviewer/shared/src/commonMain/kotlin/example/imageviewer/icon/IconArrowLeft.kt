package example.imageviewer.icon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath

val IconCustomArrowBack = materialIcon("Filled.CustomArrowBack") {
    val startY = 12f
    val startX = 1f
    val arrowWidth = 8f
    val arrowHeight = 14f
    val lineWidth = 14f
    val lineHeight = 2f
    materialPath {
        moveTo(startX, startY)
        lineToRelative(arrowWidth, arrowHeight / 2)
        verticalLineToRelative(-arrowHeight)
        close()
        moveTo(startX + arrowWidth, startY + lineHeight / 2)
        verticalLineToRelative(-lineHeight)
        horizontalLineToRelative(lineWidth)
        verticalLineToRelative(lineHeight)
        close()
    }
}
