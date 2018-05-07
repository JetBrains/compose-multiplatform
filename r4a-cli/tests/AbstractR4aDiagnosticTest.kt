import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.checkers.CheckerTestUtil
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.RenderedDiagnostic
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.r4a.ComponentsClosedDeclarationChecker
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class AbstractR4aDiagnosticsTest: TestCase() {
    protected val myTestRootDisposable: Disposable = TestDisposable()

    fun doTest(expectedText: String) {
        // Setup the environment for the analysis
        val environment = createEnvironment()
        setupEnvironment(environment)
        val diagnosedRanges: List<CheckerTestUtil.DiagnosedRange> = ArrayList()
        val clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges)
        val file = KotlinTestUtils.createFile("test.kt", clearText, environment.project)
        val files = listOf(file)
        val languageVersionSettings = CompilerTestLanguageVersionSettings(
                BaseDiagnosticsTest.DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
                LanguageVersionSettingsImpl.DEFAULT.apiVersion,
                LanguageVersionSettingsImpl.DEFAULT.languageVersion
        )

        // Use the JVM version of the analyzer to allow using classes in .jar files
        val moduleTrace = NoScopeRecordCliBindingTrace()
        val result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                environment.project,
                files,
                moduleTrace,
                environment.configuration.copy().apply {
                    this.languageVersionSettings = languageVersionSettings
                    this.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)
                },
                environment::createPackagePartProvider
        )

        // Collect the errors
        val errors = result.bindingContext.diagnostics.all()

        val message = StringBuilder()

        // Ensure all the expected messages are there
        val found = mutableSetOf<Diagnostic>()
        for (range in diagnosedRanges) {
            for (diagnostic in range.diagnostics) {
                val reportedDiagnostic = errors.find { it.factoryName == diagnostic.name }
                if (reportedDiagnostic != null) {
                    val reportedRange = reportedDiagnostic.textRanges.find { it.startOffset == range.start && it.endOffset == range.end }
                    if (reportedRange == null) {
                        val firstRange = reportedDiagnostic.textRanges.first()
                        message.append("  Error ${diagnostic.name} reported at ${firstRange.startOffset} but expected at ${range.start}\n")
                        message.append(sourceInfo(clearText, firstRange.startOffset, firstRange.endOffset, "  "))
                    }
                    found.add(reportedDiagnostic)
                } else {
                    message.append("  Error ${diagnostic.name} not reported, expected at ${range.start}\n")
                    message.append(sourceInfo(clearText, range.start, range.end, "  "))
                }
            }
        }

        // Ensure only the expected errors are reported
        for (diagnostic in errors) {
            if (diagnostic !in found) {
                val range = diagnostic.textRanges.first()
                message.append("  Unexpected error ${diagnostic.factoryName} reported at ${range.startOffset}\n")
                message.append(sourceInfo(clearText, range.startOffset, range.endOffset, "  "))
            }
        }

        // Throw an error if anything was found that was not expected
        if (message.length > 0) throw Exception("Mismatched errors:\n" + message.toString())
    }

    protected fun createEnvironment(): KotlinCoreEnvironment {
        val classPath = listOf(KotlinTestUtils.getAnnotationsJar(),
                               assertExists(File("dist/kotlinc/lib/r4a-runtime.jar")))
        val configuration = KotlinTestUtils.newConfiguration(
                ConfigurationKind.JDK_ONLY,
                TestJdkKind.MOCK_JDK,
                classPath,
                emptyList<File>()
        )

        return KotlinCoreEnvironment.createForTests(myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    fun setupEnvironment(environment: KotlinCoreEnvironment) {
        // Add checkers here
        StorageComponentContainerContributor.registerExtension(environment.project, ComponentsClosedDeclarationChecker())
    }

    override fun tearDown() {
        Disposer.dispose(myTestRootDisposable)
        super.tearDown()
    }

    inner class TestDisposable : Disposable {
        override fun dispose() {}

        override fun toString(): String {
            return this.javaClass.name
        }
    };

}


fun assertExists(file: File): File {
    if (!file.exists()) {
        throw IllegalStateException("'$file' does not exist. Run test from gradle")
    }
    return file
}

// Normalize the factory's name to find the name supplied by a plugin
val Diagnostic.factoryName: String
  inline get() = if (factory.name == "PLUGIN_ERROR")
        (this as DiagnosticWithParameters1<*,RenderedDiagnostic<*>>).a.diagnostic.factory.name else factory.name

fun String.lineStart(offset: Int): Int {
    return this.lastIndexOf('\n', offset) + 1
}

fun String.lineEnd(offset: Int): Int {
    val result = this.indexOf('\n', offset)
    return if (result < 0) this.length else result
}

// Return the source line that contains the given range with the range underlined with '~'s
fun sourceInfo(clearText: String, start: Int, end: Int, prefix: String = ""): String {
    val lineStart = clearText.lineStart(start)
    val lineEnd = clearText.lineEnd(start)
    val displayEnd = if (end > lineEnd) lineEnd else end
    return prefix + clearText.substring(lineStart, lineEnd) + "\n" +
            prefix + " ".repeat(start - lineStart) + "~".repeat(displayEnd- start) + "\n"
}