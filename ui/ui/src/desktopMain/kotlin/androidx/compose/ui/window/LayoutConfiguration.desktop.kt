/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.awt.Component
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment

// TODO(demin): detect OS fontScale
//  font size can be changed on Windows 10 in Settings - Ease of Access,
//  on Ubuntu in Settings - Universal Access
//  on macOS there is no such setting
//  issue: https://github.com/JetBrains/compose-jb/issues/57

// TODO(demin) support RTL. see https://github.com/JetBrains/compose-jb/issues/872.
//  also, don't forget to search all LayoutDirection.Ltr in desktopMain

internal val GlobalDensity get() = GraphicsEnvironment.getLocalGraphicsEnvironment()
    .defaultScreenDevice
    .defaultConfiguration
    .density

internal val Component.density: Density get() = graphicsConfiguration.density

private val GraphicsConfiguration.density: Density get() = Density(
    defaultTransform.scaleX.toFloat(),
    fontScale = 1f
)

internal val GlobalLayoutDirection get() = LayoutDirection.Ltr

@Suppress("unused")
internal val Component.layoutDirection: LayoutDirection get() = LayoutDirection.Ltr