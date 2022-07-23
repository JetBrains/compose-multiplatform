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

private const val COMPOSE_PACKAGE = "androidx.compose.ui"

/** Preview element name */
const val COMPOSE_PREVIEW_ANNOTATION_NAME = "Preview"

const val COMPOSABLE_ANNOTATION_NAME = "Composable"

const val COMPOSE_ALIGNMENT = "${COMPOSE_PACKAGE}.Alignment"
const val COMPOSE_ALIGNMENT_HORIZONTAL = "${COMPOSE_ALIGNMENT}.Horizontal"
const val COMPOSE_ALIGNMENT_VERTICAL = "${COMPOSE_ALIGNMENT}.Vertical"

const val COMPOSE_ARRANGEMENT = "androidx.compose.foundation.layout.Arrangement"
const val COMPOSE_ARRANGEMENT_HORIZONTAL = "${COMPOSE_ARRANGEMENT}.Horizontal"
const val COMPOSE_ARRANGEMENT_VERTICAL = "${COMPOSE_ARRANGEMENT}.Vertical"

val COMPOSABLE_FQ_NAMES = setOf(
  "androidx.compose.$COMPOSABLE_ANNOTATION_NAME",
  "androidx.compose.runtime.$COMPOSABLE_ANNOTATION_NAME"
)

/**
 * Represents the Jetpack Compose library package name. The compose libraries will move from
 * `androidx.ui` to `androidx.compose` and this enum encapsulates the naming for the uses in tools.
 */
enum class ComposeLibraryNamespace(
  val packageName: String,
  /** Package containing the API preview definitions. Elements here will be referenced by the user. */
  val apiPreviewPackage: String = "$packageName.tooling.preview"
) {
  ANDROIDX_COMPOSE(COMPOSE_PACKAGE);

  val composeModifierClassName: String = "$packageName.Modifier"

  /** Only composables with this annotations will be rendered to the surface. */
  val previewAnnotationName = "$apiPreviewPackage.$COMPOSE_PREVIEW_ANNOTATION_NAME"

}

