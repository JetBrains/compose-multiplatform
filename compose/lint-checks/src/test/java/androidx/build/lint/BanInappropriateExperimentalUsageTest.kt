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

@file:Suppress("UnstableApiUsage", "GroovyUnusedAssignment")

package androidx.build.lint

import androidx.build.lint.BanInappropriateExperimentalUsage.Companion.getMavenCoordinatesFromPath
import androidx.build.lint.BanInappropriateExperimentalUsage.Companion.isAnnotationAlwaysAllowed
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Important:
 *
 * [BanInappropriateExperimentalUsage] uses the atomic library groups list generated from
 * production data.  For tests, we want to overwrite this to provide a different list of
 * atomic library groups; the file location is
 * lint-checks/src/test/resources/atomic-library-groups.txt.
 *
 * Note that the filename must match
 * [BanInappropriateExperimentalUsage.ATOMIC_LIBRARY_GROUPS_FILENAME].
 */
@RunWith(JUnit4::class)
class BanInappropriateExperimentalUsageTest : AbstractLintDetectorTest(
    useDetector = BanInappropriateExperimentalUsage(),
    useIssues = listOf(BanInappropriateExperimentalUsage.ISSUE),
    stubs = arrayOf(Stubs.OptIn),
) {

    @Test
    fun `Check if annotation is always allowed`() {
        /* ktlint-disable max-line-length */

        // These annotations are used in AndroidX
        assertTrue(isAnnotationAlwaysAllowed("com.google.devtools.ksp.KspExperimental"))
        assertTrue(isAnnotationAlwaysAllowed("kotlin.contracts.ExperimentalContracts"))
        assertTrue(isAnnotationAlwaysAllowed("kotlin.ExperimentalStdlibApi"))
        assertTrue(isAnnotationAlwaysAllowed("kotlin.experimental.ExperimentalTypeInference"))
        assertTrue(isAnnotationAlwaysAllowed("kotlinx.coroutines.DelicateCoroutinesApi"))
        assertTrue(isAnnotationAlwaysAllowed("kotlinx.coroutines.ExperimentalCoroutinesApi"))
        assertTrue(isAnnotationAlwaysAllowed("org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints"))
        assertTrue(isAnnotationAlwaysAllowed("org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"))

        assertFalse(isAnnotationAlwaysAllowed("androidx.foo.bar"))
        assertFalse(isAnnotationAlwaysAllowed("com.google.foo.bar"))
        /* ktlint-enable max-line-length */
    }

    @Test
    fun `getLibraryFromPath should return correct Maven coordinates`() {
        /* ktlint-disable max-line-length */
        val paging = getMavenCoordinatesFromPath("/path/to/checkout/out/androidx/paging/paging-common/build/libs/paging-common-3.2.0-alpha01.jar")
        val room = getMavenCoordinatesFromPath("/path/to/checkout/out/androidx/room/room-compiler-processing/build/libs/room-compiler-processing-2.5.0-alpha02.jar")
        /* ktlint-enable max-line-length */

        assertNotNull(paging!!)
        assertEquals("androidx.paging", paging.groupId)
        assertEquals("paging-common", paging.artifactId)
        assertEquals("3.2.0-alpha01", paging.version)

        assertNotNull(room!!)
        assertEquals("androidx.room", room.groupId)
        assertEquals("room-compiler-processing", room.artifactId)
        assertEquals("2.5.0-alpha02", room.version)

        val invalid = getMavenCoordinatesFromPath("/foo/bar/baz")
        assertNull(invalid)
    }

    @Test
    fun `Test same atomic module Experimental usage via Gradle model`() {
        val provider = project()
            .name("provider")
            .files(
                ktSample("sample.annotation.provider.WithinGroupExperimentalAnnotatedClass"),
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(provider).expect(expected)
    }

    @Test
    fun `Test same non-atomic module Experimental usage via Gradle model`() {
        val provider = project()
            .name("provider")
            .files(
                ktSample("sample.annotation.provider.WithinGroupExperimentalAnnotatedClass"),
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, testModes = listOf(TestMode.DEFAULT)).expect(expected)
    }

    @Test
    fun `Test cross-module Experimental usage via Gradle model`() {

        /* ktlint-disable max-line-length */
        val provider = project()
            .name("provider")
            .type(ProjectDescription.Type.LIBRARY)
            .report(false)
            .files(
                ANDROIDX_REQUIRES_OPT_IN_KT,
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
                javaSample("sample.annotation.provider.ExperimentalSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresOptInSampleAnnotationJavaDuplicate"),
                javaSample("sample.annotation.provider.RequiresAndroidXOptInSampleAnnotationJava"),
                javaSample("sample.annotation.provider.RequiresAndroidXOptInSampleAnnotationJavaDuplicate"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=sample.annotation.provider
                    """
                ).indented(),
            )
        /* ktlint-enable max-line-length */

        val consumer = project()
            .name("consumer")
            .type(ProjectDescription.Type.LIBRARY)
            .dependsOn(provider)
            .files(
                ANDROIDX_OPT_IN_KT,
                ktSample("androidx.sample.consumer.OutsideGroupExperimentalAnnotatedClass"),
                gradle(
                    """
                    apply plugin: 'com.android.library'
                    group=androidx.sample.consumer
                    """
                ).indented(),
            )

        /* ktlint-disable max-line-length */
        val expected = """
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:35: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @ExperimentalSampleAnnotationJava
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:40: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @RequiresOptInSampleAnnotationJava
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:45: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @kotlin.OptIn(RequiresOptInSampleAnnotationJava::class)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:50: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @kotlin.OptIn(
    ^
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:59: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @kotlin.OptIn(RequiresOptInSampleAnnotationJava::class, RequiresOptInSampleAnnotationJavaDuplicate::class)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:65: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @androidx.annotation.OptIn(RequiresAndroidXOptInSampleAnnotationJava::class)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:70: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @androidx.annotation.OptIn(
    ^
../consumer/src/main/kotlin/androidx/sample/consumer/OutsideGroupExperimentalAnnotatedClass.kt:79: Error: Experimental and RequiresOptIn APIs may only be used within the same-version group where they were defined. [IllegalExperimentalApiUsage]
    @androidx.annotation.OptIn(RequiresAndroidXOptInSampleAnnotationJava::class, RequiresAndroidXOptInSampleAnnotationJavaDuplicate::class)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
8 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        // TODO: Using TestMode.DEFAULT due to b/188814760; remove testModes once bug is resolved
        check(provider, consumer, testModes = listOf(TestMode.DEFAULT)).expect(expected)
    }

    companion object {
        /**
         * [TestFile] containing OptIn.kt from the AndroidX experimental annotation library.
         */
        val ANDROIDX_OPT_IN_KT: TestFile = kotlin(
            """
package androidx.annotation

import kotlin.annotation.Retention
import kotlin.annotation.Target
import kotlin.reflect.KClass

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FILE,
    AnnotationTarget.TYPEALIAS
)
public annotation class OptIn(
    @get:Suppress("ArrayReturn")
    vararg val markerClass: KClass<out Annotation>
)
            """.trimIndent()
        )

        /**
         * [TestFile] containing RequiresOptIn.kt from the AndroidX experimental annotation library.
         */
        val ANDROIDX_REQUIRES_OPT_IN_KT: TestFile = kotlin(
            """
package androidx.annotation

import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class RequiresOptIn(
    val level: Level = Level.ERROR
) {
    public enum class Level {
        WARNING,
        ERROR
    }
}
            """.trimIndent()
        )
    }
}
