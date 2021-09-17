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

@file:Suppress("COMPOSABLE_FUNCTION_REFERENCE")

package androidx.compose.material3.catalog.library.model

import androidx.compose.material3.catalog.library.util.SampleSourceUrl
import androidx.compose.material3.samples.ColorSchemeSample
import androidx.compose.runtime.Composable

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val content: @Composable () -> Unit
)

private const val ColorExampleDescription = "Color examples"
private const val ColorExampleSourceUrl = "$SampleSourceUrl/ColorSamples.kt"
val ColorExamples =
    listOf(
        Example(
            name = ::ColorSchemeSample.name,
            description = ColorExampleDescription,
            sourceUrl = ColorExampleSourceUrl,
        ) { ColorSchemeSample() },
    )
