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

package androidx.compose.integration.demos.common

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

/**
 * Generic demo with a [title] that will be displayed in the list of demos.
 */
sealed class Demo(val title: String) {
    override fun toString() = title
}

/**
 * Demo that launches an [Activity] when selected.
 *
 * This should only be used for demos that need to customize the activity, the large majority of
 * demos should just use [ComposableDemo] instead.
 *
 * @property activityClass the KClass (Foo::class) of the activity that will be launched when
 * this demo is selected.
 */
class ActivityDemo<T : ComponentActivity>(title: String, val activityClass: KClass<T>) : Demo(title)

class FragmentDemo<T : Fragment>(title: String, val fragmentClass: KClass<T>) : Demo(title)

/**
 * Demo that displays [Composable] [content] when selected.
 */
class ComposableDemo(title: String, val content: @Composable () -> Unit) : Demo(title)

/**
 * A category of [Demo]s, that will display a list of [demos] when selected.
 */
class DemoCategory(title: String, val demos: List<Demo>) : Demo(title)

/**
 * Flattened recursive DFS [List] of every demo in [this].
 */
fun DemoCategory.allDemos(): List<Demo> {
    val allDemos = mutableListOf<Demo>()
    fun DemoCategory.addAllDemos() {
        demos.forEach { demo ->
            allDemos += demo
            if (demo is DemoCategory) {
                demo.addAllDemos()
            }
        }
    }
    addAllDemos()
    return allDemos
}

/**
 * Flattened recursive DFS [List] of every launchable demo in [this].
 */
fun DemoCategory.allLaunchableDemos(): List<Demo> {
    return allDemos().filter { it !is DemoCategory }
}
