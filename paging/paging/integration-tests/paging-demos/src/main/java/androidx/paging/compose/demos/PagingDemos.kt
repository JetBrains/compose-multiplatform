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

package androidx.paging.compose.demos

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.paging.compose.demos.room.PagingRoomDemo
import androidx.paging.compose.samples.PagingBackendSample

val PagingDemos = DemoCategory(
    "Paging",
    listOf(
        ComposableDemo("Paging Backend Demo") { PagingBackendSample() },
        ComposableDemo("Paging Room Demo") { PagingRoomDemo() }
    )
)
