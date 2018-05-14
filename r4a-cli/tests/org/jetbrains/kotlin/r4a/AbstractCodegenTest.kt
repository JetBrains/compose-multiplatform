package org.jetbrains.kotlin.r4a

import assertExists
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.util.*

abstract class AbstractCodeGenTest: CodegenTestCase() {
    override fun setUp() {
        super.setUp()
        val classPath = mutableListOf<File>(
            KotlinTestUtils.getAnnotationsJar(),
            assertExists(File("dist/kotlinc/lib/r4a-runtime.jar")),
            assertExists(File("dependencies/android.jar"))
        )
        val configuration = createConfiguration(
            ConfigurationKind.ALL,
            TestJdkKind.MOCK_JDK,
            classPath,
            Collections.emptyList(),
            Collections.emptyList()
        )
        updateConfiguration(configuration)

        additionalDependencies = listOf(
            File("dist/kotlinc/lib/r4a-runtime.jar"),
            File("dependencies/android.jar")
        )

        myEnvironment = KotlinCoreEnvironment.createForTests(
            getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)
    }

    override fun extractConfigurationKind(files: MutableList<TestFile>): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    protected fun loadClass(className: String, source: String): Class<*> {
        myFiles = CodegenTestFiles.create("file.kt", source, myEnvironment!!.project)
        val loader = createClassLoader()
        return loader.loadClass(className)
    }

    protected fun createClassFile(className: String, source: String): ByteArray {
        myFiles = CodegenTestFiles.create("file.kt", source, myEnvironment!!.project)
        val classFiles = generateClassesInFile().asList()
        val classFileForObject = classFiles.first { it.relativePath.endsWith("$className.class") }
        return classFileForObject.asByteArray()
    }
}