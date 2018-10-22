package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.psi.KtFile

import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind

import java.io.File

abstract class AbstractCodeGenTest : CodegenTestCase() {
    override fun setUp() {
        super.setUp()
        val classPath = listOf(
            KotlinTestUtils.getAnnotationsJar(),
            assertExists(File("dist/kotlinc/lib/r4a-runtime.jar")),
            assertExists(File("custom-dependencies/android-sdk/build/libs/android.jar"))
        )
        val configuration = createConfiguration(
                ConfigurationKind.ALL,
                TestJdkKind.MOCK_JDK,
                classPath,
                emptyList(),
                emptyList()
        )
        updateConfiguration(configuration)

        additionalDependencies = listOf(
            File("dist/kotlinc/lib/r4a-runtime.jar"),
            File("custom-dependencies/android-sdk/build/libs/android.jar")
        )

        myEnvironment = KotlinCoreEnvironment.createForTests(
                testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)
    }

    override fun extractConfigurationKind(files: MutableList<TestFile>): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    protected open fun helperFiles(): List<KtFile> = emptyList()

    protected fun dumpClasses(loader: GeneratedClassLoader) {
        for (file in loader.allGeneratedFiles.filter { it.relativePath.endsWith(".class") }) {
            println("------\nFILE: ${file.relativePath}\n------")
            println(file.asText())
        }
    }

    protected fun classLoader(source: String, fileName: String, dumpClasses: Boolean = false): GeneratedClassLoader {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile(fileName, source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun testFile(source: String, dumpClasses: Boolean = false) {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        val loadedClass = loader.loadClass("Test")
        val instance = loadedClass.newInstance()
        val instanceClass = instance::class.java
        val testMethod = instanceClass.getMethod("test")
        testMethod.invoke(instance)
    }

    protected fun testCompile(source: String, dumpClasses: Boolean = false) {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
    }

    protected fun sourceFile(name: String, source: String): KtFile {
        val result = KotlinTestUtils.createFile(name, source, myEnvironment!!.project)
        val ranges = AnalyzingUtils.getSyntaxErrorRanges(result)
        assert(ranges.isEmpty()) { "Syntax errors found in $name: $ranges" }
        return result
    }

    protected fun loadClass(className: String, source: String): Class<*> {
        myFiles = CodegenTestFiles.create("file.kt", source, myEnvironment!!.project)
        val loader = createClassLoader()
        return loader.loadClass(className)
    }
}
