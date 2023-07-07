/*
 * Copyright 2022 The Android Open Source Project
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

package androix.compose.ui.test.manifest.lint

import androidx.compose.ui.test.manifest.lint.TestManifestIssueRegistry
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.CURRENT_API
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiLintVersionsTest {
    @Test
    fun versionsCheck() {
        LintClient.clientName = LintClient.CLIENT_UNIT_TESTS

        val registry = TestManifestIssueRegistry()
        // We hardcode version registry.api to the version that is used to run tests.
        Assert.assertEquals(
            "registry.api matches version used to run tests",
            CURRENT_API,
            registry.api
        )
        // Since we don't publish this lint registry, minApi can match the api version.
        Assert.assertEquals("registry.minApi matches registry.api", 10, registry.minApi)
    }
}