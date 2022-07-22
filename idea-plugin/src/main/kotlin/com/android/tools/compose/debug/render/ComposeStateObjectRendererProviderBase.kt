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
import com.intellij.debugger.ui.tree.render.ChildrenRenderer
import com.intellij.debugger.ui.tree.render.CompoundRendererProvider
import com.intellij.debugger.ui.tree.render.ValueLabelRenderer
import com.sun.jdi.Type
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * Base custom renderer provider for rendering a given compose `StateObject` type object.
 *
 * [stateObjectClassRenderer] is the actual underlying renderer for the label and the children nodes. Users can select
 * the provided renderer by [rendererName] if applicable.
 *
 * @param fqcn the fully qualified class name of the Compose State Object to apply the underlying custom renderer to.
 */
sealed class ComposeStateObjectRendererProviderBase(private val fqcn: String) : CompoundRendererProvider() {
  private val rendererName = "Compose State Object"
  private val stateObjectClassRenderer by lazy {
    ComposeStateObjectClassRenderer(fqcn)
  }

  override fun getName(): String {
    return rendererName
  }

  override fun isEnabled(): Boolean {
    return StudioFlags.COMPOSE_STATE_OBJECT_CUSTOM_RENDERER.get()
  }

  override fun getIsApplicableChecker(): Function<Type?, CompletableFuture<Boolean>> {
    return Function { type: Type? ->
      stateObjectClassRenderer.isApplicableAsync(type)
    }
  }

  override fun getValueLabelRenderer(): ValueLabelRenderer {
    return stateObjectClassRenderer
  }

  override fun getChildrenRenderer(): ChildrenRenderer {
    return stateObjectClassRenderer
  }
}

class SnapshotMutableStateImplRendererProvider : ComposeStateObjectRendererProviderBase(
  "androidx.compose.runtime.SnapshotMutableStateImpl"
)

class DerivedSnapshotStateRendererProvider : ComposeStateObjectRendererProviderBase(
  "androidx.compose.runtime.DerivedSnapshotState"
)

class ComposeStateObjectListRendererProvider : ComposeStateObjectRendererProviderBase(
  "androidx.compose.runtime.snapshots.SnapshotStateList"
)

class ComposeStateObjectMapRendererProvider : ComposeStateObjectRendererProviderBase(
  "androidx.compose.runtime.snapshots.SnapshotStateMap"
)