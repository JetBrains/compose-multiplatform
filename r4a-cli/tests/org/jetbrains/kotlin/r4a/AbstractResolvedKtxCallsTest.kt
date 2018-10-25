/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.r4a

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.ast.ResolvedKtxElementCall
import org.jetbrains.kotlin.r4a.ast.print
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractResolvedKtxCallsTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {

        val classPath = listOf(
            KotlinTestUtils.getAnnotationsJar(),
            assertExists(File("dist/kotlinc/lib/r4a-runtime.jar")),
            assertExists(File("custom-dependencies/android-sdk/build/libs/android.jar"))
        )

        val configuration = KotlinTestUtils.newConfiguration(
            ConfigurationKind.ALL,
            TestJdkKind.MOCK_JDK,
            classPath,
            emptyList()
        )

        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)

        val env = KotlinCoreEnvironment.createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        R4AComponentRegistrar().registerProjectComponents(env.project, env.configuration)

        return env
    }

    private fun <T> withNewTypeResolution(block: () -> T): T {
        val prev = R4AFlags.USE_NEW_TYPE_RESOLUTION
        try {
            R4AFlags.USE_NEW_TYPE_RESOLUTION = true
            return block()
        } finally {
            R4AFlags.USE_NEW_TYPE_RESOLUTION = prev
        }
    }

    fun doTest(srcText: String, expected: String) {
        withNewTypeResolution {
            val (text, carets) = extractCarets(srcText)

            setupLanguageVersionSettingsForCompilerTests(srcText, environment)

            val ktFile = KtPsiFactory(project).createFile(text)
            val bindingContext = JvmResolveUtil.analyze(ktFile, environment).bindingContext

            val resolvedCalls = carets.mapNotNull { caret ->
                val (element, ktxElementCall) = buildCachedCallAtIndex(bindingContext, ktFile, caret)
                val elementText = element?.text ?: error("KtxElement expected, but none found")
                val call = ktxElementCall ?: error("ResolvedKtxElementCall expected, but none found")
                elementText to call
            }

            val output = renderOutput(resolvedCalls)


            TestCase.assertEquals(expected.trimIndent(), output.trimIndent())
        }
    }

    protected open fun renderOutput(resolvedCallsAt: List<Pair<String, ResolvedKtxElementCall>>): String =
        resolvedCallsAt.joinToString("\n\n\n") { (_, resolvedCall) ->
            resolvedCall.print()
        }

    protected fun extractCarets(text: String): Pair<String, List<Int>> {
        val parts = text.split("<caret>")
        if (parts.size < 2) return text to emptyList()
        // possible to rewrite using 'scan' function to get partial sums of parts lengths
        val indices = mutableListOf<Int>()
        val resultText = buildString {
            parts.dropLast(1).forEach { part ->
                append(part)
                indices.add(this.length)
            }
            append(parts.last())
        }
        return resultText to indices
    }

    protected open fun buildCachedCallAtIndex(
        bindingContext: BindingContext, jetFile: KtFile, index: Int
    ): Pair<PsiElement?, ResolvedKtxElementCall?> {
        val element = jetFile.findElementAt(index)!!
        val expression = element.parentOfType<KtxElement>()

        val cachedCall = bindingContext[R4AWritableSlices.RESOLVED_KTX_CALL, expression]
        return Pair(element, cachedCall)
    }
}