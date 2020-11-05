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

@file:Suppress("UnstableApiUsage")

package androidx.compose.ui.lint

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.CURRENT_API
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiLintVersionsTest {

    @Test
    fun versionsCheck() {
        LintClient.clientName = LintClient.CLIENT_UNIT_TESTS

        val registry = UiIssueRegistry()
        // we hardcode version registry.api to the version that is used to run tests
        assertThat(registry.api).isEqualTo(CURRENT_API)
        // Intentionally fails in IDE, because we use different API version in
        // studio and command line
        assertThat(registry.minApi).isEqualTo(3)
    }
}
