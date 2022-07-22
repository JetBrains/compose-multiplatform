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
package com.android.tools.compose.debug

import com.intellij.ui.classFilter.ClassFilter
import com.intellij.ui.classFilter.DebuggerClassFilterProvider

class ComposeDebuggerClassesFilterProvider : DebuggerClassFilterProvider {
  private companion object {
    private val FILTERS = listOf(ClassFilter("androidx.compose.runtime*"))
  }

  override fun getFilters(): List<ClassFilter> {
    return if (ComposeDebuggerSettings.getInstance().filterComposeRuntimeClasses) FILTERS else listOf()
  }
}
