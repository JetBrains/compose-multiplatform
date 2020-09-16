/*
 * Copyright 2019 The Android Open Source Project
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

import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

/**
 * A [RobolectricTestRunner] for [androidx.compose].
 *
 * It has instrumentation turned off for the [androidx.compose] package.
 *
 * Robolectric tries to instrument Kotlin classes, and it throws errors when it encounters
 * companion objects and constructors with default values for parameters. We don't need
 * shadowing of our classes because we want to use the actual objects in our tests.
 *
 * We can also make the argument that no external developer should shadow any of
 * androidx.compose, as they shouldn't be testing library code. This concern is being tracked by
 * b/139828620 which will make a change to Robolectric code to prevent instrumentation of all
 * classes under [androidx.compose].
 */
class ComposeUiRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod?): InstrumentationConfiguration {
        val builder = InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
        // TODO: b/160233169 remove 'androidx.ui' when everything is migrated to androidx.compose
        builder.doNotInstrumentPackage("androidx.ui")
        builder.doNotInstrumentPackage("androidx.compose")
        return builder.build()
    }
}
