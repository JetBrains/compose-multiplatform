/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.internal.ComposeCompilerArtifactProvider
import org.jetbrains.compose.internal.copy
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ComposeCompilerArtifactProviderTest {
    @Test
    fun customVersion() {
        assertArtifactEquals(
            Expected.jbCompiler.copy(version = "10.20.30"),
            Actual.compiler("10.20.30")
        )
    }

    @Test
    fun customCompiler() {
        assertArtifactEquals(
            Expected.googleCompiler.copy(version = "1.3.1"),
            Actual.compiler("androidx.compose.compiler:compiler:1.3.1")
        )
    }

    @Test
    fun customCompilerHosted() {
        // check that we don't replace artifactId for non-jb compiler
        assertArtifactEquals(
            Expected.googleCompiler.copy(version = "1.3.1"),
            Actual.compilerHosted("androidx.compose.compiler:compiler:1.3.1")
        )
    }

    @Test
    fun illegalCompiler() {
        testIllegalCompiler("androidx.compose.compiler:compiler")
        testIllegalCompiler("a:b:c:d")
        testIllegalCompiler("")
    }

    private fun testIllegalCompiler(pluginString: String) {
        try {
            Actual.compiler(pluginString)
        } catch (e: Exception) {
            return
        }

        error("Expected error, but illegal value was accepted: '$pluginString'")
    }

    object Actual {
        fun compiler(pluginString: String) =
            ComposeCompilerArtifactProvider { pluginString }.compilerArtifact

        fun compilerHosted(pluginString: String) =
            ComposeCompilerArtifactProvider { pluginString }.compilerHostedArtifact
    }

    object Expected {
        val jbCompiler: SubpluginArtifact
            get() = SubpluginArtifact(
                groupId = "org.jetbrains.compose.compiler",
                artifactId = "compiler",
                version = "1.9.20"
            )

        val googleCompiler: SubpluginArtifact
            get() = jbCompiler.copy(groupId = "androidx.compose.compiler")
    }

    private fun assertArtifactEquals(
        expected: SubpluginArtifact,
        actual: SubpluginArtifact
    ) {
        assertEquals(expected.asString(), actual.asString())
    }

    private fun SubpluginArtifact.asString(): String =
        "SubpluginArtifact(groupId = '$groupId', artifactId = '$artifactId', version = '$version')"
}