/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.studies.rally

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icons below are copied from [Icons.Filled] in material-icons-extended to avoid recompiling the
 * module in demos. In the future when we release a stable artifact we could directly depend on
 * that, instead of a project dependency which causes recompilation.
 *
 * If the generated icons change, just build material-icons-extended and copy the generated
 * file, which should appear in Studio sources by searching for the name of that icon.
 */

val Icons.Filled.Sort: ImageVector by lazy {
    materialIcon("Filled.Sort") {
        materialPath {
            moveTo(3.0f, 18.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(-2.0f)
            lineTo(3.0f, 16.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(3.0f, 6.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(18.0f)
            lineTo(21.0f, 6.0f)
            lineTo(3.0f, 6.0f)
            close()
            moveTo(3.0f, 13.0f)
            horizontalLineToRelative(12.0f)
            verticalLineToRelative(-2.0f)
            lineTo(3.0f, 11.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }
}

val Icons.Filled.ArrowForwardIos: ImageVector by lazy {
    materialIcon("Filled.ArrowForwardIos") {
        materialPath {
            moveTo(5.88f, 4.12f)
            lineTo(13.76f, 12.0f)
            lineToRelative(-7.88f, 7.88f)
            lineTo(8.0f, 22.0f)
            lineToRelative(10.0f, -10.0f)
            lineTo(8.0f, 2.0f)
            close()
        }
    }
}

val Icons.Filled.AttachMoney: ImageVector by lazy {
    materialIcon("Filled.AttachMoney") {
        materialPath {
            moveTo(11.8f, 10.9f)
            curveToRelative(-2.27f, -0.59f, -3.0f, -1.2f, -3.0f, -2.15f)
            curveToRelative(0.0f, -1.09f, 1.01f, -1.85f, 2.7f, -1.85f)
            curveToRelative(1.78f, 0.0f, 2.44f, 0.85f, 2.5f, 2.1f)
            horizontalLineToRelative(2.21f)
            curveToRelative(-0.07f, -1.72f, -1.12f, -3.3f, -3.21f, -3.81f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.16f)
            curveToRelative(-1.94f, 0.42f, -3.5f, 1.68f, -3.5f, 3.61f)
            curveToRelative(0.0f, 2.31f, 1.91f, 3.46f, 4.7f, 4.13f)
            curveToRelative(2.5f, 0.6f, 3.0f, 1.48f, 3.0f, 2.41f)
            curveToRelative(0.0f, 0.69f, -0.49f, 1.79f, -2.7f, 1.79f)
            curveToRelative(-2.06f, 0.0f, -2.87f, -0.92f, -2.98f, -2.1f)
            horizontalLineToRelative(-2.2f)
            curveToRelative(0.12f, 2.19f, 1.76f, 3.42f, 3.68f, 3.83f)
            verticalLineTo(21.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(-2.15f)
            curveToRelative(1.95f, -0.37f, 3.5f, -1.5f, 3.5f, -3.55f)
            curveToRelative(0.0f, -2.84f, -2.43f, -3.81f, -4.7f, -4.4f)
            close()
        }
    }
}

val Icons.Filled.MoneyOff: ImageVector by lazy {
    materialIcon("Filled.MoneyOff") {
        materialPath {
            moveTo(12.5f, 6.9f)
            curveToRelative(1.78f, 0.0f, 2.44f, 0.85f, 2.5f, 2.1f)
            horizontalLineToRelative(2.21f)
            curveToRelative(-0.07f, -1.72f, -1.12f, -3.3f, -3.21f, -3.81f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.16f)
            curveToRelative(-0.53f, 0.12f, -1.03f, 0.3f, -1.48f, 0.54f)
            lineToRelative(1.47f, 1.47f)
            curveToRelative(0.41f, -0.17f, 0.91f, -0.27f, 1.51f, -0.27f)
            close()
            moveTo(5.33f, 4.06f)
            lineTo(4.06f, 5.33f)
            lineTo(7.5f, 8.77f)
            curveToRelative(0.0f, 2.08f, 1.56f, 3.21f, 3.91f, 3.91f)
            lineToRelative(3.51f, 3.51f)
            curveToRelative(-0.34f, 0.48f, -1.05f, 0.91f, -2.42f, 0.91f)
            curveToRelative(-2.06f, 0.0f, -2.87f, -0.92f, -2.98f, -2.1f)
            horizontalLineToRelative(-2.2f)
            curveToRelative(0.12f, 2.19f, 1.76f, 3.42f, 3.68f, 3.83f)
            verticalLineTo(21.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(-2.15f)
            curveToRelative(0.96f, -0.18f, 1.82f, -0.55f, 2.45f, -1.12f)
            lineToRelative(2.22f, 2.22f)
            lineToRelative(1.27f, -1.27f)
            lineTo(5.33f, 4.06f)
            close()
        }
    }
}

val Icons.Filled.PieChart: ImageVector by lazy {
    materialIcon("Filled.PieChart") {
        materialPath {
            moveTo(11.0f, 2.0f)
            verticalLineToRelative(20.0f)
            curveToRelative(-5.07f, -0.5f, -9.0f, -4.79f, -9.0f, -10.0f)
            reflectiveCurveToRelative(3.93f, -9.5f, 9.0f, -10.0f)
            close()
            moveTo(13.03f, 2.0f)
            verticalLineToRelative(8.99f)
            lineTo(22.0f, 10.99f)
            curveToRelative(-0.47f, -4.74f, -4.24f, -8.52f, -8.97f, -8.99f)
            close()
            moveTo(13.03f, 13.01f)
            lineTo(13.03f, 22.0f)
            curveToRelative(4.74f, -0.47f, 8.5f, -4.25f, 8.97f, -8.99f)
            horizontalLineToRelative(-8.97f)
            close()
        }
    }
}
