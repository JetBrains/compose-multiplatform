/*
 * Copyright 2023 The Android Open Source Project
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

package org.jetbrains.compose.ui.tooling.preview

/**
 * [Preview] can be applied to either of the following:
 * - @[Composable] methods with no parameters to show them in the IDE preview.
 * - Annotation classes, that could then be used to annotate @[Composable] methods or other
 *   annotation classes, which will then be considered as indirectly annotated with that [Preview].
 *
 * The annotation contains a number of parameters that allow to define the way the @[Composable]
 * will be rendered within the preview.
 *
 * The passed parameters are only read by IDE when rendering the preview.
 *
 * @param name Display name of this preview allowing to identify it in the panel.
 * @param group Group name for this @[Preview]. This allows grouping them in the UI and display only
 *   one or more of them.
 * @param widthDp Max width in DP the annotated @[Composable] will be rendered in. Use this to
 *   restrict the size of the rendering viewport.
 * @param heightDp Max height in DP the annotated @[Composable] will be rendered in. Use this to
 *   restrict the size of the rendering viewport.
 * @param locale Current user preference for the locale, corresponding to
 *   [locale](https://d.android.com/guide/topics/resources/providing-resources.html#LocaleQualifier)
 *   resource qualifier. By default, the `default` folder will be used. To preview an RTL layout use
 *   a locale that uses right to left script, such as `ar` (or the `ar-rXB` pseudo locale).
 * @param showBackground If true, the @[Composable] will use a default background color.
 * @param backgroundColor The 32-bit ARGB color int for the background or 0 if not set
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
@Deprecated(
    "Use androidx.compose.ui.tooling.preview.Preview from " +
            "org.jetbrains.compose.ui:ui-tooling-preview module instead",
    ReplaceWith("Preview", "androidx.compose.ui.tooling.preview.Preview")
)
annotation class Preview(
    val name: String = "",
    val group: String = "",
    val widthDp: Int = -1,
    val heightDp: Int = -1,
    val locale: String = "",
    val showBackground: Boolean = false,
    val backgroundColor: Long = 0,
)
