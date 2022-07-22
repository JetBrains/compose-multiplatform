/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package icons

import com.intellij.ui.*
import javax.swing.*

object StudioIcons {
    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
        return IconManager.getInstance().loadRasterizedIcon(
            path,
            StudioIcons::class.java.classLoader, cacheKey, flags
        )
    }

    class Compose {
        object Editor {
            /** 16x16  */
            val COMPOSABLE_FUNCTION = load("icons/compose/composable-function.svg", -238070477, 2)
        }
    }
}