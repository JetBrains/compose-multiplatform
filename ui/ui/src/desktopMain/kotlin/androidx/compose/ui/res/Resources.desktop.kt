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

package androidx.compose.ui.res

import java.io.InputStream

/**
 * Open [InputStream] from a resource stored in resources for the application, calls the [block]
 * callback giving it a InputStream and closes stream once the processing is
 * complete.
 *
 * @return object that was returned by [block]
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
inline fun <T> useResource(
    resourcePath: String,
    block: (InputStream) -> T
): T = openResource(resourcePath).use(block)

/**
 * Open [InputStream] from a resource stored in resources for the application.
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@PublishedApi
internal fun openResource(resourcePath: String): InputStream {
    // TODO(https://github.com/JetBrains/compose-jb/issues/618): probably we shouldn't use
    //  contextClassLoader here, as it is not defined in threads created by non-JVM
    val classLoader = Thread.currentThread().contextClassLoader!!

    return requireNotNull(classLoader.getResourceAsStream(resourcePath)) {
        "Resource $resourcePath not found"
    }
}