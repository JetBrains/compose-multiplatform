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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwners
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.DesktopPlatformAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Density
import org.jetbrains.skija.Surface

class TestComposeWindow(
    val width: Int,
    val height: Int,
    val density: Density = Density(1f, 1f),
    var desktopPlatform: DesktopPlatform = DesktopPlatform.Linux
) {
    val surface = Surface.makeRasterN32Premul(width, height)!!
    val canvas = surface.canvas
    val owners = DesktopOwners(invalidate = {})

    fun setContent(content: @Composable () -> Unit): DesktopOwners {
        val owner = DesktopOwner(owners, density)
        owner.setContent {
            Providers(
                DesktopPlatformAmbient provides desktopPlatform
            ) {
                content()
            }
        }
        owner.setSize(width, height)
        owner.measureAndLayout()
        owner.draw(canvas)
        return owners
    }
}