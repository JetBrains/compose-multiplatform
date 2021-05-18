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
package org.jetbrains.compose.common.ui

interface Alignment {
    interface Vertical : Alignment
    interface Horizontal : Alignment

    companion object {
        val TopStart = object : Alignment {}
        val TopCenter = object : Alignment {}
        val TopEnd = object : Alignment {}
        val CenterStart = object : Alignment {}
        val Center = object : Alignment {}
        val CenterEnd = object : Alignment {}
        val BoottomStart = object : Alignment {}
        val BoottomCenter = object : Alignment {}
        val BoottomEnd = object : Alignment {}

        val Top = object : Alignment.Vertical {}
        val CenterVertically = object : Alignment.Vertical {}
        val Bottom = object : Alignment.Vertical {}

        val Start = object : Alignment.Horizontal {}
        val CenterHorizontally = object : Alignment.Horizontal {}
        val End = object : Alignment.Horizontal {}
    }
}
