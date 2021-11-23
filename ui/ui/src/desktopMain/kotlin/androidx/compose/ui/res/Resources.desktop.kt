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

import androidx.compose.ui.ExperimentalComposeUiApi
import java.io.InputStream
import java.io.File
import java.io.FileInputStream

/**
 * Open [InputStream] from a resource stored in resources for the application, calls the [block]
 * callback giving it a InputStream and closes stream once the processing is
 * complete.
 *
 * @param resourcePath  path of resource in loader
 * @param loader  resource loader
 * @return object that was returned by [block]
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@ExperimentalComposeUiApi
internal inline fun <T> useResource(
    resourcePath: String,
    loader: ResourceLoader,
    block: (InputStream) -> T
): T = openResource(resourcePath, loader).use(block)

/**
 * Open [InputStream] from a resource stored in resources for the application, calls the [block]
 * callback giving it a InputStream and closes stream once the processing is
 * complete.
 *
 * @param resourcePath  path of resource
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
 * @param resourcePath  path of resource in loader
 * @param loader  resource loader
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@PublishedApi
@ExperimentalComposeUiApi
internal fun openResource(
    resourcePath: String,
    loader: ResourceLoader
): InputStream {
    return loader.load(resourcePath)
}

/**
 * Open [InputStream] from a resource stored in resources for the application.
 *
 * @param resourcePath  path of resource
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@PublishedApi
@OptIn(ExperimentalComposeUiApi::class)
internal fun openResource(
    resourcePath: String,
): InputStream {
    return ResourceLoader.Default.load(resourcePath)
}

/**
 * Abstraction for loading resources. This API is intended for use in synchronous cases,
 * where resource is expected to be loaded quick during the first composition, and so potentially
 * slow operations like network access is not recommended. For such scenarious use functions
 * [loadSvgPainter] and [loadXmlImageVector] instead on IO dispatcher.
 * Also the resource should be always available to load, and if you need to handle exceptions,
 * it is better to use these functions as well.
 */
@ExperimentalComposeUiApi
interface ResourceLoader {
    companion object {
        /**
         * Resource loader which is capable to load resources from `resources` folder in an application's
         * project. Ability to load from dependent modules resources is not guaranteed in the future.
         * Use explicit `ClassLoaderResourceLoader` instance if such guarantee is needed.
         */
        @ExperimentalComposeUiApi
        val Default = ClassLoaderResourceLoader()
    }
    fun load(resourcePath: String): InputStream
}

/**
 * Resource loader based on JVM current context class loader.
 */
@ExperimentalComposeUiApi
class ClassLoaderResourceLoader : ResourceLoader {
    override fun load(resourcePath: String): InputStream {
        // TODO(https://github.com/JetBrains/compose-jb/issues/618): probably we shouldn't use
        //  contextClassLoader here, as it is not defined in threads created by non-JVM
        return Thread.currentThread().contextClassLoader!!.getResourceAsStream(resourcePath)
    }
}

/**
 * Resource loader from the file system relative to a certain root location.
 */
@ExperimentalComposeUiApi
class FileResourceLoader(val root: File) : ResourceLoader {
    override fun load(resourcePath: String): InputStream {
        return FileInputStream(File(root, resourcePath))
    }
}
