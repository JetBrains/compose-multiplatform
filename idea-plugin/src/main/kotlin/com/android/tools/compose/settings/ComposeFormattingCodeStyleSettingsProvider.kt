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

import com.android.tools.compose.ComposeBundle
import com.intellij.openapi.ui.DialogPanel
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.ui.layout.panel
import org.jetbrains.kotlin.idea.KotlinLanguage
import javax.swing.JCheckBox


/**
 * Allows to turn on and off [ComposePostFormatProcessor] in Code Style settings.
 */
class ComposeFormattingCodeStyleSettingsProvider : CodeStyleSettingsProvider() {

  override fun hasSettingsPage() = false

  override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings {
    return ComposeCustomCodeStyleSettings(settings)
  }

  override fun getConfigurableDisplayName(): String = ComposeBundle.message("compose")
  override fun getLanguage() = KotlinLanguage.INSTANCE

  override fun createConfigurable(originalSettings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable {
    return object : CodeStyleConfigurable {

      private lateinit var checkBox: JCheckBox

      override fun createComponent(): DialogPanel {
        return panel {
          row {
            titledRow("Compose formatting") {
              row {
                checkBox = checkBox(
                  ComposeBundle.message("compose.enable.formatting.for.modifiers"),
                  ComposeCustomCodeStyleSettings.getInstance(originalSettings).USE_CUSTOM_FORMATTING_FOR_MODIFIERS).component
              }
            }
          }
        }
      }

      override fun isModified() = ComposeCustomCodeStyleSettings.getInstance(
        originalSettings).USE_CUSTOM_FORMATTING_FOR_MODIFIERS != checkBox.isSelected

      override fun apply(settings: CodeStyleSettings) {
        ComposeCustomCodeStyleSettings.getInstance(settings).USE_CUSTOM_FORMATTING_FOR_MODIFIERS = checkBox.isSelected
      }

      override fun apply() = apply(originalSettings)

      override fun reset(settings: CodeStyleSettings) {
        checkBox.isSelected = ComposeCustomCodeStyleSettings.getInstance(settings).USE_CUSTOM_FORMATTING_FOR_MODIFIERS
      }

      override fun getDisplayName() = ComposeBundle.message("compose")
    }

  }
}