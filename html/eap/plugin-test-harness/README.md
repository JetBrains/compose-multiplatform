# Compiler plugin IR tests

Compile Kotlin sources with your plugin and compare **post-plugin, pre-backend-lowering IR**
with a golden file. K2/JVM only; Kotlin stdlib is included automatically.

## Add to a JVM test project

In the consuming project's `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation(project(":compiler-plugin-test-harness"))
}
```

Enable [EAP projects](../README.md) to make the harness subproject available.
Keep your existing test framework configuration; the harness and base class do not depend on JUnit.

### Optional: JUnit 5 setup

The examples below use JUnit 5. If your project already runs JUnit 5 tests, skip this setup.
Otherwise, add:

```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir // Golden paths below are relative to this directory.
}
```

## Write a test

Replace `MyPluginRegistrar` with your plugin's registrar.

```kotlin
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.plugin.testing.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class MyPluginTests : AbstractCompilerPluginIrTest() {
    override val registrars = listOf(MyPluginRegistrar())

    @Test
    fun transformsFunction() {
        verify(
            sources = listOf(KotlinSource("Example.kt", "fun answer(): Int = 1")),
            goldenFile = File("src/test/resources/golden/function.txt"),
        )
    }
}
```

On the first run, the missing golden causes a failure and writes `function.txt.actual`.
See the [sample golden](testData/replaceOne.txt) for the source-and-IR format.

## Review and accept a golden

From the test project's directory:

```shell
# When an existing golden differs:
diff -u src/test/resources/golden/function.txt src/test/resources/golden/function.txt.actual

# After reviewing the actual output:
cp src/test/resources/golden/function.txt.actual src/test/resources/golden/function.txt
```

Alternatively, temporarily request an update inside the test:

```kotlin
verify(
    sources = listOf(KotlinSource("Example.kt", "fun answer(): Int = 1")),
    goldenFile = File("src/test/resources/golden/function.txt"),
    mode = GoldenMode.UPDATE,
)
```

Remove `mode = GoldenMode.UPDATE` after reviewing the new golden. Normal verification
never rewrites expectations; missing, empty, or mismatched goldens fail when output differs.
A successful verification removes stale `.actual` files. Commit goldens, not `.actual` files.

## Share runtime libraries and compiler settings

Add your runtime as a Gradle test dependency, then locate it with a marker class.
Replace the registrar, marker, and configuration key with your plugin's types.

```kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.plugin.testing.*

@OptIn(ExperimentalCompilerApi::class)
abstract class MyPluginIrTest : AbstractCompilerPluginIrTest() {
    override val registrars = listOf(MyPluginRegistrar())
    override val additionalClasspath = listOf(classpathEntryFor<MyRuntimeMarker>())

    override fun CompilerConfiguration.configureCompiler() {
        put(MyPluginConfiguration.ENABLED, true)
    }
}
```

`classpathEntryFor<T>()` returns only the JAR or classes directory containing `T`.
Add entries for any other required libraries; it does not inherit the test runner's classpath
or discover transitive dependencies.

## Read source files and add per-test settings

Inside a subclass of `MyPluginIrTest`:

```kotlin
@Test
fun transformsMultipleFiles() {
    verify(
        sources = listOf(
            KotlinSource.fromFile(File("testData/Example.kt")),
            KotlinSource.fromFile(File("testData/Helper.kt"), name = "helpers/Helper.kt"),
        ),
        goldenFile = File("src/test/resources/golden/multipleFiles.txt"),
        additionalClasspath = listOf(classpathEntryFor<ExtraRuntimeMarker>()),
        configure = {
            put(MyPluginConfiguration.ENABLED, false)
        },
    )
}
```

Per-test classpath entries **append to** suite defaults. Per-test configuration runs **after**
`configureCompiler()`, so it can override suite settings.

## Compile a dependency module first

Inside a subclass of `MyPluginIrTest`:

```kotlin
@Test
fun transformsConsumer() {
    // Compiled without this test suite's plugin or runtime defaults.
    val library = PluginTestModule(
        name = "library",
        sources = listOf(
            KotlinSource("Library.kt", "package lib\nfun value(): Int = 1"),
        ),
    )

    verify(
        sources = listOf(KotlinSource("Consumer.kt", "fun answer(): Int = lib.value()")),
        dependencies = listOf(library),
        goldenFile = File("src/test/resources/golden/consumer.txt"),
    )
}
```

To deliberately apply suite defaults to the dependency, use the base class's `module` helper instead:

```kotlin
val library = module(
    name = "library",
    sources = listOf(KotlinSource("Library.kt", "package lib\nfun value(): Int = 1")),
)
```

Dependency modules are compiled to JVM classes before the consumer. Only the consumer's IR
is rendered; all modules' sources appear in the golden. Module names must be unique.

## Use the harness without inheritance

```kotlin
@OptIn(ExperimentalCompilerApi::class)
@Test
fun transformsWithoutBaseClass() {
    val module = PluginTestModule(
        name = "example",
        sources = listOf(KotlinSource("Example.kt", "fun answer(): Int = 1")),
        registrars = listOf(MyPluginRegistrar()),
        classpath = listOf(classpathEntryFor<MyRuntimeMarker>()),
        configure = {
            put(MyPluginConfiguration.ENABLED, true)
        },
    )

    CompilerPluginTestHarness().verify(
        module,
        File("src/test/resources/golden/function.txt"),
    )
}
```

Omit `classpath` when the source and generated code need no libraries beyond stdlib.

## Customize the output

The default output is Kotlin-like IR without absolute file paths. To use the structural IR dump:

```kotlin
import org.jetbrains.kotlin.ir.util.dump

val harness = CompilerPluginTestHarness { ir -> ir.dump() }
val actual = harness.render(module)
GoldenFiles.assertMatches(File("src/test/resources/golden/structural.txt"), actual)
```

Or override the renderer in your test base class:

```kotlin
override val renderer = IrRenderer { ir -> ir.dump() }
```

Renderers run before compiler resources are disposed; return text rather than retaining IR objects.
Comparisons normalize line endings and the final newline, but preserve indentation and trailing spaces.

## Omit or replace stdlib

Inside a base-class test:

```kotlin
verify(
    sources = listOf(KotlinSource.fromFile(File("testData/WithoutStdlib.kt"))),
    goldenFile = File("src/test/resources/golden/withoutStdlib.txt"),
    includeStdlib = false,
)
```

To supply your own stdlib, also pass it via `additionalClasspath` (or `classpath` on
`PluginTestModule`). To change the suite default:

```kotlin
override val includeStdlib = false
```

This disables automatic inclusion only; explicit and dependency classpath entries remain available.

## Run the harness's own tests

From the compose-multiplatform repository root, using JDK 17:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :compiler-plugin-test-harness:test
```

Regenerate this module's example golden explicitly:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :compiler-plugin-test-harness:test \
    -Pkotlin.plugin.tests.update.goldens=true
```

That property updates this module's sample test only; it is not a global update switch for consumers.

Working examples:
- [Plugin transformation, cross-module compilation, and classpaths](test/org/jetbrains/kotlin/plugin/testing/CompilerPluginTestHarnessTest.kt)
- [Base-class defaults and per-test overrides](test/org/jetbrains/kotlin/plugin/testing/AbstractCompilerPluginIrTestTest.kt)
- [Golden comparison and update behavior](test/org/jetbrains/kotlin/plugin/testing/GoldenFilesTest.kt)

## Compatibility

- Tested with Kotlin **2.4.20**; compiler and stdlib dependencies follow the parent Kotlin Gradle plugin.
- Plugins must target `kotlin-compiler-embeddable`; do not mix it with `kotlin-compiler`.
- `-Pharness.compiler.version=<version>` overrides compiler/stdlib artifacts, but API changes may require source and golden updates.
- Run compiler invocations sequentially; the underlying compiler CLI has process-global state.
- No standalone settings/wrapper or released Maven coordinate; use the EAP subproject.
