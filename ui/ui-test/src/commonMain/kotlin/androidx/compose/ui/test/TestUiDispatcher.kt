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

package androidx.compose.ui.test

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@ExperimentalTestApi
object TestUiDispatcher {
    /**
     * The dispatcher to use if you need to dispatch coroutines on the main thread in tests.
     */
    @Deprecated(
        message = "Removed in favor of Dispatchers.Main",
        replaceWith = ReplaceWith("Dispatchers.Main", "kotlinx.coroutines.Dispatchers")
    )
    val Main: CoroutineContext = Dispatchers.Main
}
