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
package com.android.tools.compose.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

// Don't convert to Kotlin due to the serialization implementation for Settings.
public class ComposeCustomCodeStyleSettings extends CustomCodeStyleSettings {
  public boolean USE_CUSTOM_FORMATTING_FOR_MODIFIERS = true;

  protected ComposeCustomCodeStyleSettings(CodeStyleSettings container) {
    super("ComposeCustomCodeStyleSettings", container);
  }

  public static ComposeCustomCodeStyleSettings getInstance(CodeStyleSettings settings) {
    return settings.getCustomSettings(ComposeCustomCodeStyleSettings.class);
  }
}