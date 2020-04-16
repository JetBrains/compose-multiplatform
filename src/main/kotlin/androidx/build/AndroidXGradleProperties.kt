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

package androidx.build

import org.gradle.api.Project

/**
 * Setting this property makes Test tasks succeed even if there
 * are some failing tests. Useful when running tests in CI where build
 * passes test results as XML to test reporter.
 */
const val TEST_FAILURES_DO_NOT_FAIL_TEST_TASK = "androidx.ignoreTestFailures"

/**
 * Setting this property turns javac and kotlinc warnings into errors that fail the build.
 */
const val ALL_WARNINGS_AS_ERRORS = "androidx.allWarningsAsErrors"

/**
 * Returns whether the project should generate versioned API files, e.g. 1.1.0-alpha01.txt.
 * <p>
 * When set to true, updateApi will generate API files as both current.txt and <version>.txt. When
 * set to false, only current.txt will be generated. The default value is true.
 */
fun Project.shouldGenerateVersionedApiFiles(): Boolean {
    return rootProject.findProperty("androidx.writeVersionedApiFiles") as? Boolean ?: true
}
