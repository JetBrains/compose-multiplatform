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
package com.android.tools.compose.debug;

import com.intellij.openapi.options.ConfigurableUi;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

public class ComposeDebuggerSettingsUi implements ConfigurableUi<ComposeDebuggerSettings> {
  private JPanel myPanel;
  private JCheckBox filterComposeInternalClasses;


  @Override
  public void reset(@NotNull ComposeDebuggerSettings settings) {
    filterComposeInternalClasses.setSelected(settings.getFilterComposeRuntimeClasses());
  }

  @Override
  public boolean isModified(@NotNull ComposeDebuggerSettings settings) {
    return filterComposeInternalClasses.isSelected() != settings.getFilterComposeRuntimeClasses();
  }

  @Override
  public void apply(@NotNull ComposeDebuggerSettings settings) {
    settings.setFilterComposeRuntimeClasses(filterComposeInternalClasses.isSelected());
  }

  @Override
  public @NotNull JComponent getComponent() {
    return myPanel;
  }
}
