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

import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_TYPE_WITH_ANNOTATIONS

object ComposeErrorMessages : DefaultErrorMessages.Extension {
  private val MAP = DiagnosticFactoryToRendererMap("Compose")
  override fun getMap() = MAP

  init {

    MAP.put(
      ComposeErrors.COMPOSABLE_INVOCATION,
      ComposeBundle.message("errors.composable_invocation")
    )

    MAP.put(
      ComposeErrors.COMPOSABLE_EXPECTED,
      ComposeBundle.message("errors.composable_expected")
    )

    MAP.put(
      ComposeErrors.CAPTURED_COMPOSABLE_INVOCATION,
      @Suppress("InvalidBundleOrProperty")
      ComposeBundle.message("errors.captured_composable_invocation"),
      Renderers.NAME,
      Renderers.COMPACT
    )

    MAP.put(
      ComposeErrors.COMPOSABLE_PROPERTY_BACKING_FIELD,
      ComposeBundle.message("errors.composable_property_backing_field")
    )

    MAP.put(
      ComposeErrors.COMPOSABLE_VAR,
      ComposeBundle.message("errors.composable_var")
    )

    MAP.put(
      ComposeErrors.COMPOSABLE_SUSPEND_FUN,
      ComposeBundle.message("errors.composable_suspend_fun")
    )

    MAP.put(
      ComposeErrors.ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE,
      ComposeBundle.message("errors.illegal_try_catch_around_composable")
    )

    MAP.put(
      ComposeErrors.COMPOSABLE_FUNCTION_REFERENCE,
      ComposeBundle.message("errors.composable_function_reference")
    )

    MAP.put(
      ComposeErrors.CONFLICTING_OVERLOADS,
      @Suppress("InvalidBundleOrProperty")
      ComposeBundle.message("errors.conflicting_overloads"),
      CommonRenderers.commaSeparated(
        Renderers.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS
      )
    )

    MAP.put(
      ComposeErrors.TYPE_MISMATCH,
      @Suppress("InvalidBundleOrProperty")
      ComposeBundle.message("errors.type_mismatch"),
      RENDER_TYPE_WITH_ANNOTATIONS,
      RENDER_TYPE_WITH_ANNOTATIONS
    )

    MAP.put(
      ComposeErrors.MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION,
      @Suppress("InvalidBundleOrProperty")
      ComposeBundle.message("errors.missing_disallow_composable_calls_annotation"),
      Renderers.NAME,
      Renderers.NAME,
      Renderers.NAME
    )

    MAP.put(
      ComposeErrors.NONREADONLY_CALL_IN_READONLY_COMPOSABLE,
      ComposeBundle.message("errors.nonreadonly_call_in_readonly_composable")
    )

    MAP.put(
      ComposeErrors.COMPOSABLE_FUN_MAIN,
      ComposeBundle.message("errors.composable_fun_main")
    )
  }
}