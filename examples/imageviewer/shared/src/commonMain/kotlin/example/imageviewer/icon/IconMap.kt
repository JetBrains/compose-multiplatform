package example.imageviewer.icon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath

// TODO Copied from "material:material-icons-extended", because this artifact is not working on iOS for now
val IconMap = materialIcon(name = "Filled.Map") {
    materialPath {
        moveTo(20.5f, 3.0f)
        lineToRelative(-0.16f, 0.03f)
        lineTo(15.0f, 5.1f)
        lineTo(9.0f, 3.0f)
        lineTo(3.36f, 4.9f)
        curveToRelative(-0.21f, 0.07f, -0.36f, 0.25f, -0.36f, 0.48f)
        verticalLineTo(20.5f)
        curveToRelative(0.0f, 0.28f, 0.22f, 0.5f, 0.5f, 0.5f)
        lineToRelative(0.16f, -0.03f)
        lineTo(9.0f, 18.9f)
        lineToRelative(6.0f, 2.1f)
        lineToRelative(5.64f, -1.9f)
        curveToRelative(0.21f, -0.07f, 0.36f, -0.25f, 0.36f, -0.48f)
        verticalLineTo(3.5f)
        curveToRelative(0.0f, -0.28f, -0.22f, -0.5f, -0.5f, -0.5f)
        close()
        moveTo(15.0f, 19.0f)
        lineToRelative(-6.0f, -2.11f)
        verticalLineTo(5.0f)
        lineToRelative(6.0f, 2.11f)
        verticalLineTo(19.0f)
        close()
    }
}
