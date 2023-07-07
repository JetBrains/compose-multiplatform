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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier

/**
 * This is a modifier that can be used to send relocation requests.
 *
 * @param relocationRequester an instance of [RelocationRequester]. This hoisted object can be
 * used to send relocation requests to parents of the current composable.
 */
@ExperimentalComposeUiApi
@Suppress("UNUSED_PARAMETER")
@Deprecated(
    message = "Please use bringIntoViewRequester instead.",
    replaceWith = ReplaceWith(
        "bringIntoViewRequester",
        "androidx.compose.foundation.relocation.bringIntoViewRequester"

    ),
    level = DeprecationLevel.ERROR
)
fun Modifier.relocationRequester(relocationRequester: Any): Modifier = this
