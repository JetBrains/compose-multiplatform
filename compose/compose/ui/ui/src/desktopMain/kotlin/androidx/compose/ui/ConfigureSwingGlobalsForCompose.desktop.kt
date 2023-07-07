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
package androidx.compose.ui

import org.jetbrains.skia.impl.Library

/**
 * Override global configuration for Swing that is needed for stand-alone Compose application.
 *
 * This function is called implicitly if you use [awaitApplication] / [application] /
 * [launchApplication] and Compose for Desktop Gradle plugin tasks "run" or "package"
 *
 * If you integrate Compose into existing Swing application (like IDEA), don't call this function
 * as it affects the entire application.
 *
 * This function:
 * - sets system property `apple.laf.useScreenMenuBar` to true
 * - sets system property `sun.java2d.uiScale`/`sun.java2d.uiScale.enabled` automatically on Linux
 * - sets UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
 *
 * Should be called before using any class from `java.swing.*`
 * (even before SwingUtilities.invokeLater or MainUIDispatcher)
 */
@ExperimentalComposeUiApi
fun configureSwingGlobalsForCompose(
    overrideLookAndFeel: Boolean =
        System.getProperty("skiko.rendering.laf.global", "true") == "true",
    useScreenMenuBarOnMacOs: Boolean =
        System.getProperty("skiko.rendering.useScreenMenuBar", "true") == "true",
    useAutoDpiOnLinux: Boolean =
        System.getProperty("skiko.linux.autodpi", "true") == "true",
) {
    System.setProperty("skiko.rendering.laf.global", overrideLookAndFeel.toString())
    System.setProperty("skiko.rendering.useScreenMenuBar", useScreenMenuBarOnMacOs.toString())
    System.setProperty("skiko.linux.autodpi", useAutoDpiOnLinux.toString())
    Library.staticLoad()
}
