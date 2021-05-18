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
package org.jetbrains.compose.common.core.graphics

public data class Color(val red: Int, val green: Int, val blue: Int) {

    companion object {
        val Black = Color(0, 0, 0)
        val DarkGray = Color(0x44, 0x44, 0x44)
        val Gray = Color(0x88, 0x88, 0x88)
        val LightGray = Color(0xCC, 0xCC, 0xCC)
        val White = Color(0xFF, 0xFF, 0xFF)
        val Red = Color(0xFF, 0, 0)
        val Green = Color(0, 0xFF, 0)
        val Blue = Color(0, 0, 0xFF)
        val Yellow = Color(0xFF, 0xFF, 0x00)
        val Cyan = Color(0, 0xFF, 0xFF)
        val Magenta = Color(0xFF, 0, 0xFF)
    }
}