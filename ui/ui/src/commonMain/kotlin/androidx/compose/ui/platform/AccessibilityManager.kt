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

package androidx.compose.ui.platform

import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Interface for managing accessibility.
 */
@JvmDefaultWithCompatibility
interface AccessibilityManager {
    /**
     * Calculate the recommended timeout for changes to the UI needed by this user. Controls should
     * remain on the screen for at least this long to give users time to react. Some users may need
     * extra time to review the controls, or to reach them, or to activate assistive technology
     * to activate the controls automatically.
     * <p>
     * Use the boolean parameters to indicate contents of UI. For example, set [containsIcons]
     * and [containsText] to true for message notification which contains icons and text, or set
     * [containsText] and [containsControls] to true for button dialog which contains text and
     * button controls.
     * <p/>
     *
     * @param originalTimeoutMillis The timeout appropriate for users with no accessibility needs
     * in milliseconds.
     * @param containsIcons The contents of UI contain icons.
     * @param containsText The contents of UI contain text.
     * @param containsControls The contents of UI contain controls.
     * @return The recommended UI timeout for the current user in milliseconds.
     */
    fun calculateRecommendedTimeoutMillis(
        originalTimeoutMillis: Long,
        containsIcons: Boolean = false,
        containsText: Boolean = false,
        containsControls: Boolean = false
    ): Long
}