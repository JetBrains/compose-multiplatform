/*
 * Copyright 2021 The Android Open Source Project
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

package org.jetbrains.compose.common.ui.unit

// TODO: this have to be in a separate package otherwise there's an error for in cross-module usage (for JVM target)
val Int.dp: Dp
    get() = Dp(this.toFloat())

val Int.em: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Em)

val Float.em: TextUnit
    get() = TextUnit(this, TextUnitType.Em)

val Int.sp: TextUnit
    get() = TextUnit(toFloat(), TextUnitType.Sp)

val Float.sp: TextUnit
    get() = TextUnit(this, TextUnitType.Sp)
