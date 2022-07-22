/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.compose.settings

import com.intellij.configurationStore.*
import com.intellij.psi.codeStyle.*

class ComposeCustomCodeStyleSettings(settings: CodeStyleSettings) : CustomCodeStyleSettings("ComposeCustomCodeStyleSettings", settings) {
    @Property(externalName = "use_custom_formatting_for_modifiers")
    @JvmField
    var USE_CUSTOM_FORMATTING_FOR_MODIFIERS = true

    companion object {
        fun getInstance(settings: CodeStyleSettings): ComposeCustomCodeStyleSettings {
            return settings.getCustomSettings(ComposeCustomCodeStyleSettings::class.java)
        }
    }
}