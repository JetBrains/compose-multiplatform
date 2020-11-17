/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime.lint

import org.jetbrains.uast.UMethod

// TODO: KotlinUMethodWithFakeLightDelegate.hasAnnotation() returns null for some reason, so just
// look at the annotations directly
// TODO: annotations is deprecated but the replacement uAnnotations isn't available on the
// version of lint / uast we compile against
@Suppress("DEPRECATION")
val UMethod.isComposable get() = annotations.any { it.qualifiedName == ComposableFqn }

const val ComposableFqn = "androidx.compose.runtime.Composable"
val ComposableShortName = ComposableFqn.split(".").last()