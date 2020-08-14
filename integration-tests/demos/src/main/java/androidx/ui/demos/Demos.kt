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

package androidx.ui.demos

import androidx.ui.androidview.demos.AndroidViewDemos
import androidx.compose.animation.demos.AnimationDemos
import androidx.compose.ui.demos.CoreDemos
import androidx.ui.demos.common.DemoCategory
import androidx.compose.foundation.demos.FoundationDemos
import androidx.compose.foundation.layout.demos.LayoutDemos
import androidx.compose.material.demos.MaterialDemos
import androidx.compose.material.studies.MaterialStudies
import androidx.compose.foundation.text.demos.TextDemos

/**
 * [DemoCategory] containing all the top level demo categories.
 */
val AllDemosCategory = DemoCategory(
    "Jetpack Compose Demos",
    listOf(
        AndroidViewDemos,
        AnimationDemos,
        FoundationDemos,
        CoreDemos,
        LayoutDemos,
        MaterialDemos,
        MaterialStudies,
        TextDemos
    )
)