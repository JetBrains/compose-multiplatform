/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.compose

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(name = "ComposeSettings", storages = [Storage("composeSettings.xml")])
class ComposeSettings : SimplePersistentStateComponent<ComposeSettingsState>(ComposeSettingsState()) {
  companion object {
    fun getInstance(): ComposeSettings = ApplicationManager.getApplication().getService(ComposeSettings::class.java)
  }
}

class ComposeSettingsState : BaseState() {
  var isComposeInsertHandlerEnabled by property(true)
}