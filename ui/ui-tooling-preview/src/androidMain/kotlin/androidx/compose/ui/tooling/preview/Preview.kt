/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.tooling.preview

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable

/**
 * [Preview] can be applied to either of the following:
 * - @[Composable] methods with no parameters to show them in the Android Studio preview.
 * - Annotation classes, that could then be used to annotate @[Composable] methods or other
 * annotation classes, which will then be considered as indirectly annotated with that [Preview].
 *
 * The annotation contains a number of parameters that allow to define the way the @[Composable]
 * will be rendered within the preview.
 *
 * The passed parameters are only read by Studio when rendering the preview.
 *
 * @param name Display name of this preview allowing to identify it in the panel.
 * @param group Group name for this @[Preview]. This allows grouping them in the UI and display only
 * one or more of them.
 * @param apiLevel API level to be used when rendering the annotated @[Composable]
 * @param widthDp Max width in DP the annotated @[Composable] will be rendered in. Use this to
 * restrict the size of the rendering viewport.
 * @param heightDp Max height in DP the annotated @[Composable] will be rendered in. Use this to
 * restrict the size of the rendering viewport.
 * @param locale Current user preference for the locale, corresponding to
 * [locale](https://d.android.com/guide/topics/resources/providing-resources.html#LocaleQualifier) resource
 * qualifier. By default, the `default` folder will be used.
 * @param fontScale User preference for the scaling factor for fonts, relative to the base
 * density scaling.
 * @param showSystemUi If true, the status bar and action bar of the device will be displayed.
 * The @[Composable] will be render in the context of a full activity.
 * @param showBackground If true, the @[Composable] will use a default background color.
 * @param backgroundColor The 32-bit ARGB color int for the background or 0 if not set
 * @param uiMode Bit mask of the ui mode as per [android.content.res.Configuration.uiMode]
 * @param device Device string indicating the device to use in the preview. See the available
 * devices in [Devices].
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@Repeatable
annotation class Preview(
    val name: String = "",
    val group: String = "",
    @IntRange(from = 1) val apiLevel: Int = -1,
    // TODO(mount): Make this Dp when they are inline classes
    val widthDp: Int = -1,
    // TODO(mount): Make this Dp when they are inline classes
    val heightDp: Int = -1,
    val locale: String = "",
    @FloatRange(from = 0.01) val fontScale: Float = 1f,
    val showSystemUi: Boolean = false,
    val showBackground: Boolean = false,
    val backgroundColor: Long = 0,
    @UiMode val uiMode: Int = 0,
    @Device val device: String = Devices.DEFAULT
)
