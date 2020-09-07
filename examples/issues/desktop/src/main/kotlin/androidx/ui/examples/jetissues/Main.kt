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

package androidx.ui.examples.jetissues

import androidx.compose.desktop.Window
import androidx.compose.runtime.Providers
import androidx.compose.ui.unit.IntSize
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl

val repo = IssuesRepositoryImpl("ktorio", "ktor", System.getenv("GITHUB_TOKEN") ?: "fa7774306d1b9ab4af76652599841ee69a88fb6f")

fun main() = Window(
    title = "JetIssues",
    size = IntSize(1440, 768)
) {
    Providers(Repository provides repo) {
        JetIssuesView()
    }
}
