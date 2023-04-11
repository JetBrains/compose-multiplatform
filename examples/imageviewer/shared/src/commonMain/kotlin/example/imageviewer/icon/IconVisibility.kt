package example.imageviewer.icon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath

// TODO Copied from "material:material-icons-extended", because this artifact is not working on iOS for now
val IconVisibility = materialIcon(name = "Filled.Visibility") {
    materialPath {
        moveTo(12.0f, 4.5f)
        curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
        curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
        reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
        curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
        close()
        moveTo(12.0f, 17.0f)
        curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
        reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
        reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
        reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
        close()
        moveTo(12.0f, 9.0f)
        curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
        reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
        reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
        reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
        close()
    }
}
