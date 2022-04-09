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

package androidx.build.testConfiguration

class ConfigBuilder {
    var appApkName: String? = null
    lateinit var applicationId: String
    var isBenchmark: Boolean = false
    var isPostsubmit: Boolean = true
    lateinit var minSdk: String
    var runAllTests: Boolean = true
    var cleanupApks: Boolean = true
    val tags: MutableList<String> = mutableListOf()
    lateinit var testApkName: String
    lateinit var testRunner: String

    fun appApkName(appApkName: String) = apply { this.appApkName = appApkName }
    fun applicationId(applicationId: String) = apply { this.applicationId = applicationId }
    fun isBenchmark(isBenchmark: Boolean) = apply { this.isBenchmark = isBenchmark }
    fun isPostsubmit(isPostsubmit: Boolean) = apply { this.isPostsubmit = isPostsubmit }
    fun minSdk(minSdk: String) = apply { this.minSdk = minSdk }
    fun runAllTests(runAllTests: Boolean) = apply { this.runAllTests = runAllTests }
    fun cleanupApks(cleanupApks: Boolean) = apply { this.cleanupApks = cleanupApks }
    fun tag(tag: String) = apply { this.tags.add(tag) }
    fun testApkName(testApkName: String) = apply { this.testApkName = testApkName }
    fun testRunner(testRunner: String) = apply { this.testRunner = testRunner }

    fun build(): String {
        val sb = StringBuilder()
        sb.append(XML_HEADER_AND_LICENSE)
            .append(CONFIGURATION_OPEN)
            .append(MIN_API_LEVEL_CONTROLLER_OBJECT.replace("MIN_SDK", minSdk))
        tags.forEach { tag ->
            sb.append(TEST_SUITE_TAG_OPTION.replace("TEST_SUITE_TAG", tag))
        }
        sb.append(MODULE_METADATA_TAG_OPTION.replace("APPLICATION_ID", applicationId))
            .append(WIFI_DISABLE_OPTION)
        if (isBenchmark) {
            if (isPostsubmit) {
                sb.append(BENCHMARK_POSTSUBMIT_OPTIONS)
            } else {
                sb.append(BENCHMARK_PRESUBMIT_OPTION)
            }
        }
        sb.append(SETUP_INCLUDE)
            .append(TARGET_PREPARER_OPEN.replace("CLEANUP_APKS", cleanupApks.toString()))
            .append(APK_INSTALL_OPTION.replace("APK_NAME", testApkName))
        if (!appApkName.isNullOrEmpty())
            sb.append(APK_INSTALL_OPTION.replace("APK_NAME", appApkName!!))
        // Temporary hardcoded hack for b/181810492
        else if (applicationId == "androidx.benchmark.macro.test") {
            sb.append(
                APK_INSTALL_OPTION.replace(
                    "APK_NAME",
                    /* ktlint-disable max-line-length */
                    "benchmark-integration-tests-macrobenchmark-target_macrobenchmark-target-release.apk"
                    /* ktlint-enable max-line-length */
                )
            )
        }
        sb.append(TARGET_PREPARER_CLOSE)
            .append(TEST_BLOCK_OPEN)
            .append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
            .append(PACKAGE_OPTION.replace("APPLICATION_ID", applicationId))
        if (runAllTests) {
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(TEST_BLOCK_CLOSE)
        } else {
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(SMALL_TEST_OPTIONS)
                .append(TEST_BLOCK_CLOSE)
                .append(TEST_BLOCK_OPEN)
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
                .append(PACKAGE_OPTION.replace("APPLICATION_ID", applicationId))
                .append(MEDIUM_TEST_OPTIONS)
                .append(TEST_BLOCK_CLOSE)
        }
        sb.append(CONFIGURATION_CLOSE)
        return sb.toString()
    }
}

class MediaConfigBuilder {
    lateinit var clientApkName: String
    lateinit var clientApplicationId: String
    var isClientPrevious: Boolean = true
    var isPostsubmit: Boolean = true
    var isServicePrevious: Boolean = true
    lateinit var minSdk: String
    var runAllTests: Boolean = true
    lateinit var serviceApkName: String
    lateinit var serviceApplicationId: String
    var tags: MutableList<String> = mutableListOf()
    lateinit var testRunner: String

    fun clientApkName(clientApkName: String) = apply { this.clientApkName = clientApkName }
    fun clientApplicationId(clientApplicationId: String) =
        apply { this.clientApplicationId = clientApplicationId }
    fun isPostsubmit(isPostsubmit: Boolean) = apply { this.isPostsubmit = isPostsubmit }
    fun isClientPrevious(isClientPrevious: Boolean) = apply {
        this.isClientPrevious = isClientPrevious
    }
    fun isServicePrevious(isServicePrevious: Boolean) = apply {
        this.isServicePrevious = isServicePrevious
    }
    fun minSdk(minSdk: String) = apply { this.minSdk = minSdk }
    fun runAllTests(runAllTests: Boolean) = apply { this.runAllTests = runAllTests }
    fun serviceApkName(serviceApkName: String) = apply { this.serviceApkName = serviceApkName }
    fun serviceApplicationId(serviceApplicationId: String) =
        apply { this.serviceApplicationId = serviceApplicationId }
    fun tag(tag: String) = apply { this.tags.add(tag) }
    fun testRunner(testRunner: String) = apply { this.testRunner = testRunner }

    private fun mediaInstrumentationArgs(): String {
        return if (isClientPrevious) {
            if (isServicePrevious) {
                CLIENT_PREVIOUS + SERVICE_PREVIOUS
            } else {
                CLIENT_PREVIOUS + SERVICE_TOT
            }
        } else {
            if (isServicePrevious) {
                CLIENT_TOT + SERVICE_PREVIOUS
            } else {
                CLIENT_TOT + SERVICE_TOT
            }
        }
    }

    fun build(): String {
        val sb = StringBuilder()
        sb.append(XML_HEADER_AND_LICENSE)
            .append(CONFIGURATION_OPEN)
            .append(MIN_API_LEVEL_CONTROLLER_OBJECT.replace("MIN_SDK", minSdk))
        tags.forEach { tag ->
            sb.append(TEST_SUITE_TAG_OPTION.replace("TEST_SUITE_TAG", tag))
        }
        sb.append(
            MODULE_METADATA_TAG_OPTION.replace(
                "APPLICATION_ID", "$clientApplicationId;$serviceApplicationId"
            )
        )
            .append(WIFI_DISABLE_OPTION)
            .append(SETUP_INCLUDE)
            .append(MEDIA_TARGET_PREPARER_OPEN)
            .append(APK_INSTALL_OPTION.replace("APK_NAME", clientApkName))
            .append(APK_INSTALL_OPTION.replace("APK_NAME", serviceApkName))
        sb.append(TARGET_PREPARER_CLOSE)
            .append(TEST_BLOCK_OPEN)
            .append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
            .append(PACKAGE_OPTION.replace("APPLICATION_ID", clientApplicationId))
            .append(mediaInstrumentationArgs())
        if (runAllTests) {
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(TEST_BLOCK_CLOSE)
                .append(TEST_BLOCK_OPEN)
                .append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
                .append(PACKAGE_OPTION.replace("APPLICATION_ID", serviceApplicationId))
                .append(mediaInstrumentationArgs())
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(TEST_BLOCK_CLOSE)
        } else {
            // add the small and medium test runners for both client and service apps
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(SMALL_TEST_OPTIONS)
                .append(TEST_BLOCK_CLOSE)
                .append(TEST_BLOCK_OPEN)
                .append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
                .append(PACKAGE_OPTION.replace("APPLICATION_ID", clientApplicationId))
                .append(mediaInstrumentationArgs())
                .append(MEDIUM_TEST_OPTIONS)
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(TEST_BLOCK_CLOSE)
                .append(TEST_BLOCK_OPEN)
                .append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
                .append(PACKAGE_OPTION.replace("APPLICATION_ID", serviceApplicationId))
                .append(mediaInstrumentationArgs())
                .append(SMALL_TEST_OPTIONS)
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(TEST_BLOCK_CLOSE)
                .append(TEST_BLOCK_OPEN)
                .append(RUNNER_OPTION.replace("TEST_RUNNER", testRunner))
                .append(PACKAGE_OPTION.replace("APPLICATION_ID", serviceApplicationId))
                .append(mediaInstrumentationArgs())
                .append(MEDIUM_TEST_OPTIONS)
            if (!isPostsubmit) {
                sb.append(FLAKY_TEST_OPTION)
            }
            sb.append(TEST_BLOCK_CLOSE)
        }
        sb.append(CONFIGURATION_CLOSE)
        return sb.toString()
    }
}

/**
 * These constants are the building blocks of the xml configs, but
 * they aren't very readable as separate chunks. Look to
 * the golden examples at the bottom of
 * {@link androidx.build.testConfiguration.XmlTestConfigVerificationTest}
 * for examples of what the full xml will look like.
 */

private val XML_HEADER_AND_LICENSE = """
    <?xml version="1.0" encoding="utf-8"?>
    <!-- Copyright (C) 2020 The Android Open Source Project
    Licensed under the Apache License, Version 2.0 (the "License")
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions
    and limitations under the License.-->

""".trimIndent()

private val CONFIGURATION_OPEN = """
    <configuration description="Runs tests for the module">

""".trimIndent()

private val CONFIGURATION_CLOSE = """
    </configuration>
""".trimIndent()

private val MIN_API_LEVEL_CONTROLLER_OBJECT = """
    <object type="module_controller" class="com.android.tradefed.testtype.suite.module.MinApiLevelModuleController">
    <option name="min-api-level" value="MIN_SDK" />
    </object>

""".trimIndent()

private val TEST_SUITE_TAG_OPTION = """
    <option name="test-suite-tag" value="TEST_SUITE_TAG" />

""".trimIndent()

private val MODULE_METADATA_TAG_OPTION = """
    <option name="config-descriptor:metadata" key="applicationId" value="APPLICATION_ID" />

""".trimIndent()

private val WIFI_DISABLE_OPTION = """
    <option name="wifi:disable" value="true" />

""".trimIndent()

private val SETUP_INCLUDE = """
    <include name="google/unbundled/common/setup" />

""".trimIndent()

/**
 * Specify the following options on the APK installer:
 * - Pass the -t argument when installing APKs. This allows testonly APKs to be installed, which
 *   includes all APKs built against a pre-release SDK. See b/205571374.
 */
private val TARGET_PREPARER_OPEN = """
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="CLEANUP_APKS" />
    <option name="install-arg" value="-t" />

""".trimIndent()

/**
 * Differs from [TARGET_PREPARER_OPEN] in that Media target can remove APKs after testing.
 */
private val MEDIA_TARGET_PREPARER_OPEN = """
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="install-arg" value="-t" />

""".trimIndent()

private val TARGET_PREPARER_CLOSE = """
    </target_preparer>

""".trimIndent()

private val APK_INSTALL_OPTION = """
    <option name="test-file-name" value="APK_NAME" />

""".trimIndent()

private val TEST_BLOCK_OPEN = """
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">

""".trimIndent()

private val TEST_BLOCK_CLOSE = """
    </test>

""".trimIndent()

private val RUNNER_OPTION = """
    <option name="runner" value="TEST_RUNNER"/>

""".trimIndent()

private val PACKAGE_OPTION = """
    <option name="package" value="APPLICATION_ID" />

""".trimIndent()

private val BENCHMARK_PRESUBMIT_OPTION = """
    <option name="instrumentation-arg" key="androidx.benchmark.dryRunMode.enable" value="true" />

""".trimIndent()

private val BENCHMARK_POSTSUBMIT_OPTIONS = """
    <option name="instrumentation-arg" key="androidx.benchmark.output.enable" value="true" />
    <option name="instrumentation-arg" key="listener" value="androidx.benchmark.junit4.InstrumentationResultsRunListener" />

""".trimIndent()

private val FLAKY_TEST_OPTION = """
    <option name="instrumentation-arg" key="notAnnotation" value="androidx.test.filters.FlakyTest" />

""".trimIndent()

private val SMALL_TEST_OPTIONS = """
    <option name="size" value="small" />

""".trimIndent()

private val MEDIUM_TEST_OPTIONS = """
    <option name="size" value="medium" />

""".trimIndent()

private val CLIENT_PREVIOUS = """
    <option name="instrumentation-arg" key="client_version" value="previous" />

""".trimIndent()

private val CLIENT_TOT = """
    <option name="instrumentation-arg" key="client_version" value="tot" />

""".trimIndent()

private val SERVICE_PREVIOUS = """
    <option name="instrumentation-arg" key="service_version" value="previous" />

""".trimIndent()

private val SERVICE_TOT = """
    <option name="instrumentation-arg" key="service_version" value="tot" />

""".trimIndent()