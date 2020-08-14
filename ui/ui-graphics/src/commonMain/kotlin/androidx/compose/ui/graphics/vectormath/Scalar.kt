/*
 * Copyright 2018 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.graphics.vectormath

const val PI = 3.1415926536f
const val HALF_PI = PI * 0.5f
const val TWO_PI = PI * 2.0f
const val FOUR_PI = PI * 4.0f
const val INV_PI = 1.0f / PI
const val INV_TWO_PI = INV_PI * 0.5f
const val INV_FOUR_PI = INV_PI * 0.25f

inline fun degrees(v: Float) = v * (180.0f * INV_PI)

inline fun radians(v: Float) = v * (PI / 180.0f)
