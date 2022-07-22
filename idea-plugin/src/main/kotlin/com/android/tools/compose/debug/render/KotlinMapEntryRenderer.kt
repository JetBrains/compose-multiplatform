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
package com.android.tools.compose.debug.render

import com.android.tools.idea.flags.StudioFlags
import com.intellij.debugger.impl.DebuggerUtilsAsync
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.tree.render.ChildrenRenderer
import com.intellij.debugger.ui.tree.render.CompoundReferenceRenderer
import com.intellij.debugger.ui.tree.render.CompoundRendererProvider
import com.intellij.debugger.ui.tree.render.ValueLabelRenderer
import com.sun.jdi.ClassType
import com.sun.jdi.Type
import org.jetbrains.kotlin.idea.debugger.isInKotlinSources
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * Custom renderer for "MapEntry" type objects.
 *
 * This is to precede the `Kotlin class` renderer, as [KotlinMapEntryRenderer] provides a more readable data view,
 * that the underlying `Map.Entry` renderer does the real work.
 */
class KotlinMapEntryRenderer : CompoundRendererProvider() {
  private val MAP_ENTRY_FQCN = "java.util.Map\$Entry"

  private val mapEntryLabelRender = NodeRendererSettings.getInstance().alternateCollectionRenderers.find {
    it.name == "Map.Entry"
  }

  override fun isEnabled(): Boolean {
    if (StudioFlags.COMPOSE_STATE_OBJECT_CUSTOM_RENDERER.get()) return true

    return false
  }

  override fun getName(): String {
    return "Kotlin MapEntry"
  }

  override fun getIsApplicableChecker(): Function<Type?, CompletableFuture<Boolean>> {
    return Function { type: Type? ->
      if (type !is ClassType || !type.isInKotlinSources()) return@Function CompletableFuture.completedFuture(false)

      DebuggerUtilsAsync.instanceOf(type, MAP_ENTRY_FQCN)
    }
  }

  override fun getValueLabelRenderer(): ValueLabelRenderer {
    return (mapEntryLabelRender as CompoundReferenceRenderer).labelRenderer
  }

  override fun getChildrenRenderer(): ChildrenRenderer {
    return NodeRendererSettings.createEnumerationChildrenRenderer(arrayOf(arrayOf("key", "getKey()"), arrayOf("value", "getValue()")))
  }
}