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

package androidx.compose.material.catalog.model

data class Specification(
    val id: Int,
    val name: String,
    val artifact: String
)

private const val MaterialTitle = "Material Design 2"
private const val MaterialArtifact = "androidx.compose.material"
val MaterialSpecification = Specification(
    id = 1,
    name = MaterialTitle,
    artifact = MaterialArtifact
)

private const val Material3Title = "Material Design 3"
private const val Material3Artifact = "androidx.compose.material3"
val Material3Specification = Specification(
    id = 2,
    name = Material3Title,
    artifact = Material3Artifact
)

val Specifications = listOf(
    MaterialSpecification,
    Material3Specification
)
