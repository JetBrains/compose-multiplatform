package example.imageviewer.icon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath

// TODO Copied from "material:material-icons-extended", because this artifact is not working on iOS for now
val IconIosShare = materialIcon(name = "Filled.IosShare") {
    materialPath {
        moveTo(16.0f, 5.0f)
        lineToRelative(-1.42f, 1.42f)
        lineToRelative(-1.59f, -1.59f)
        lineTo(12.99f, 16.0f)
        horizontalLineToRelative(-1.98f)
        lineTo(11.01f, 4.83f)
        lineTo(9.42f, 6.42f)
        lineTo(8.0f, 5.0f)
        lineToRelative(4.0f, -4.0f)
        lineToRelative(4.0f, 4.0f)
        close()
        moveTo(20.0f, 10.0f)
        verticalLineToRelative(11.0f)
        curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
        lineTo(6.0f, 23.0f)
        curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
        lineTo(4.0f, 10.0f)
        curveToRelative(0.0f, -1.11f, 0.89f, -2.0f, 2.0f, -2.0f)
        horizontalLineToRelative(3.0f)
        verticalLineToRelative(2.0f)
        lineTo(6.0f, 10.0f)
        verticalLineToRelative(11.0f)
        horizontalLineToRelative(12.0f)
        lineTo(18.0f, 10.0f)
        horizontalLineToRelative(-3.0f)
        lineTo(15.0f, 8.0f)
        horizontalLineToRelative(3.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, 0.89f, 2.0f, 2.0f)
        close()
    }
}
