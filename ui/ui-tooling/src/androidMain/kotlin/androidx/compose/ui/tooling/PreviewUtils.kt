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

package androidx.compose.ui.tooling

import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * Tries to find the [Class] of the [PreviewParameterProvider] corresponding to the given FQN.
 */
internal fun String.asPreviewProviderClass(): Class<out PreviewParameterProvider<*>>? {
    try {
        @Suppress("UNCHECKED_CAST")
        return Class.forName(this) as? Class<out PreviewParameterProvider<*>>
    } catch (e: ClassNotFoundException) {
        PreviewLogger.logError("Unable to find PreviewProvider '$this'", e)
        return null
    }
}

/**
 * Returns an array with some values of a [PreviewParameterProvider]. If the given provider class
 * is `null`, returns an empty array. Otherwise, if the given `parameterProviderIndex` is a valid
 * index, returns a single-element array containing the value corresponding to that particular
 * index in the provider's sequence. Finally, returns an array with all the values of the
 * provider's sequence if `parameterProviderIndex` is invalid, e.g. negative.
 */
internal fun getPreviewProviderParameters(
    parameterProviderClass: Class<out PreviewParameterProvider<*>>?,
    parameterProviderIndex: Int
): Array<Any?> {
    if (parameterProviderClass != null) {
        try {
            val constructor = parameterProviderClass.constructors
                .singleOrNull { it.parameterTypes.isEmpty() }
                ?.apply {
                    isAccessible = true
                }
                ?: throw IllegalArgumentException(
                    "PreviewParameterProvider constructor can not" +
                        " have parameters"
                )
            val params = constructor.newInstance() as PreviewParameterProvider<*>
            if (parameterProviderIndex < 0) {
                return params.values.toArray(params.count)
            }
            return arrayOf(params.values.elementAt(parameterProviderIndex))
        } catch (e: KotlinReflectionNotSupportedError) {
            // kotlin-reflect runtime dependency not found. Suggest adding it.
            throw IllegalStateException(
                "Deploying Compose Previews with PreviewParameterProvider " +
                    "arguments requires adding a dependency to the kotlin-reflect library.\n" +
                    "Consider adding 'debugImplementation " +
                    "\"org.jetbrains.kotlin:kotlin-reflect:\$kotlin_version\"' " +
                    "to the module's build.gradle."
            )
        }
    } else {
        return emptyArray()
    }
}

@OptIn(UiToolingDataApi::class)
internal fun Group.firstOrNull(predicate: (Group) -> Boolean): Group? {
    return findGroupsThatMatchPredicate(this, predicate, true).firstOrNull()
}

@OptIn(UiToolingDataApi::class)
internal fun Group.findAll(predicate: (Group) -> Boolean): List<Group> {
    return findGroupsThatMatchPredicate(this, predicate)
}

/**
 * Search [Group]s that match a given [predicate], starting from a given [root]. An optional
 * boolean parameter can be set if we're interested in a single occurrence. If it's set, we
 * return early after finding the first matching [Group].
 */
@OptIn(UiToolingDataApi::class)
private fun findGroupsThatMatchPredicate(
    root: Group,
    predicate: (Group) -> Boolean,
    findOnlyFirst: Boolean = false
): List<Group> {
    val result = mutableListOf<Group>()
    val stack = mutableListOf(root)
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        if (predicate(current)) {
            if (findOnlyFirst) {
                return listOf(current)
            }
            result.add(current)
        }
        stack.addAll(current.children)
    }
    return result
}

private fun Sequence<Any?>.toArray(size: Int): Array<Any?> {
    val iterator = iterator()
    return Array(size) { iterator.next() }
}

/**
 * A simple wrapper to store and throw exception later in a thread-safe way.
 */
internal class ThreadSafeException {
    private var exception: Throwable? = null

    /**
     * A lock to take to access exception.
     */
    private val lock = Any()

    fun set(throwable: Throwable) {
        synchronized(lock) {
            exception = throwable
        }
    }

    fun throwIfPresent() {
        synchronized(lock) {
            exception?.let {
                exception = null
                throw it
            }
        }
    }
}
