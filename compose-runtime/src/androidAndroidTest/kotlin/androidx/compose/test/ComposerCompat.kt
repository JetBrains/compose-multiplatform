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

package androidx.compose.test

// NOTE(lmr): This API is no longer needed in any way by the compiler, but we still need this API
// to be here to support versions of Android Studio that are still looking for it. Without it,
// valid composable code will look broken in the IDE. Remove this after we have left some time to
// get all versions of Studio upgraded.
// b/152059242
@Deprecated(
    "This property should not be called directly. It is only used by the compiler.",
    replaceWith = ReplaceWith("currentComposer")
)
val composer: EmittableComposer
    get() = error(
        "This property should not be called directly. It is only used by the compiler."
    )
