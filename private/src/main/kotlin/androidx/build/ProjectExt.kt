/**
 * Copyright 2018 The Android Open Source Project
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
package androidx.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Holder class used for lazily registering tasks using the new Lazy task execution API.
 */
data class LazyTaskRegistry(
    private val names: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
) {
    fun <T : Any?> once(name: String, f: () -> T): T? {
        if (names.add(name)) {
            return f()
        }
        return null
    }

    companion object {
        private const val KEY = "AndroidXAutoRegisteredTasks"
        private val lock = ReentrantLock()
        fun get(project: Project): LazyTaskRegistry {
            val existing = project.extensions.findByName(KEY) as? LazyTaskRegistry
            if (existing != null) {
                return existing
            }
            return lock.withLock {
                project.extensions.findByName(KEY) as? LazyTaskRegistry
                    ?: LazyTaskRegistry().also {
                        project.extensions.add(KEY, it)
                    }
            }
        }
    }
}

inline fun <reified T : Task> Project.maybeRegister(
    name: String,
    crossinline onConfigure: (T) -> Unit,
    crossinline onRegister: (TaskProvider<T>) -> Unit
): TaskProvider<T> {
    @Suppress("UNCHECKED_CAST")
    return LazyTaskRegistry.get(project).once(name) {
        tasks.register(name, T::class.java) {
            onConfigure(it)
        }.also(onRegister)
    } ?: tasks.named(name) as TaskProvider<T>
}