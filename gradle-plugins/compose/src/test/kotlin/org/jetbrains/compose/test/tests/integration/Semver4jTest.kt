package org.jetbrains.compose.test.tests.integration

import com.vdurmont.semver4j.Semver
import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.junit.jupiter.api.Test

class Semver4jTest : GradlePluginTestBase() {

    @Test
    // we use an external library to compare version numbers, so we have this test to be sure that it works as expected
    fun testSemver4j() {
        assertTrue(Semver("1.8.0").isGreaterThan(Semver("1.8.0-RC2")))
        assertTrue(Semver("1.8.0-RC2").isLowerThan(Semver("1.8.0")))
        assertTrue(Semver("1.7.3").isLowerThan(Semver("1.8.0-RC2")))
        assertTrue(Semver("1.8.0-RC2").isLowerThan(Semver("1.8.0-RC3")))
    }
}