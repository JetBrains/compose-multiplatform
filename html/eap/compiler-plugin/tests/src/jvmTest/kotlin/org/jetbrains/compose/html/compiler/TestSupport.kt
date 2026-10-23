/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import androidx.compose.runtime.Composable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.assertEquals
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

internal fun staticHtmlConstants(file: String, method: String? = null): Set<String> {
    val constants = mutableSetOf<String>()
    // Compose moves stored content lambdas into a separate class.
    val files = if (method == null) listOf(file, "ComposableSingletons\$$file") else listOf(file)
    for (className in files) {
        sampleBytecode(className).accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                if (method != null && method != name) return null
                return object : MethodVisitor(Opcodes.ASM9) {
                    private val strings = mutableMapOf<Int, String>()
                    private var string: String? = null
                    private var encoded: String? = null

                    override fun visitLdcInsn(value: Any) {
                        string = value as? String
                        if (string?.startsWith('<') == true) encoded = string
                    }

                    override fun visitVarInsn(opcode: Int, variable: Int) {
                        when (opcode) {
                            Opcodes.ASTORE -> {
                                strings.remove(variable)
                                string?.let { strings[variable] = it }
                            }
                            Opcodes.ALOAD -> {
                                string = strings[variable]
                                if (string?.startsWith('<') == true) encoded = string
                            }
                        }
                    }

                    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                        if (name == "beginStaticHtml") encoded?.let(constants::add)
                        string = null
                    }
                }
            }
        }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    }
    return constants
}

private object TestResources

internal fun sampleBytecode(file: String): ClassReader {
    val path = "/${TestResources::class.java.packageName.replace('.', '/')}/$file.class"
    return requireNotNull(TestResources::class.java.getResourceAsStream(path)).use { ClassReader(it.readBytes()) }
}

internal fun visitMethods(reader: ClassReader, method: String? = null, visitor: (String) -> MethodVisitor) {
    reader.accept(object : ClassVisitor(Opcodes.ASM9) {
        override fun visitMethod(
            access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?,
        ): MethodVisitor? = if (method == null || method == name) visitor(name) else null
    }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
}

internal fun visitSampleMethod(file: String, method: String, visitor: MethodVisitor) =
    visitMethods(sampleBytecode(file), method) { visitor }

internal fun classMethodCalls(reader: ClassReader, ownerPrefix: String? = null): Map<String, List<String>> {
    val calls = linkedMapOf<String, MutableList<String>>()
    visitMethods(reader) { method ->
        val targets = calls.getOrPut(method) { mutableListOf() }
        object : MethodVisitor(Opcodes.ASM9) {
            override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                if (ownerPrefix == null || owner.startsWith(ownerPrefix)) targets.add(name)
            }
        }
    }
    return calls
}

internal fun methodCalls(file: String, method: String, ownerPrefix: String? = null): List<String> =
    classMethodCalls(sampleBytecode(file), ownerPrefix)[method].orEmpty()

internal fun methodStrings(reader: ClassReader, method: String? = null): Set<String> {
    val strings = mutableSetOf<String>()
    visitMethods(reader, method) {
        object : MethodVisitor(Opcodes.ASM9) {
            override fun visitLdcInsn(value: Any) {
                if (value is String) strings.add(value)
            }
        }
    }
    return strings
}

internal fun methodStrings(file: String, method: String): Set<String> = methodStrings(sampleBytecode(file), method)

internal fun fieldReads(file: String, method: String): Set<Pair<String, String>> {
    val reads = mutableSetOf<Pair<String, String>>()
    visitSampleMethod(file, method, object : MethodVisitor(Opcodes.ASM9) {
        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            if (opcode == Opcodes.GETSTATIC && name.startsWith("__html")) reads.add(owner to name)
        }
    })
    return reads
}

internal inline fun forEachRenderMode(test: (strict: Boolean, hydratable: Boolean, stream: Boolean) -> Unit) {
    for (strict in listOf(false, true)) for (hydratable in listOf(false, true)) for (stream in listOf(false, true)) {
        test(strict, hydratable, stream)
    }
}

internal fun render(strict: Boolean, hydratable: Boolean, stream: Boolean, content: @Composable () -> Unit): String {
    if (!stream) return composeHtmlToString(validateStrictly = strict, hydratable = hydratable, content = content)
    val chunks = StringBuilder()
    composeHtmlToStream(validateStrictly = strict, hydratable = hydratable, chunkSize = 1,
        sink = { chunks.append(it) }, content = content)
    return chunks.toString()
}

/** Runs the same JVM compiler setup for rendering comparisons and runtime-contract diagnostics. */
internal fun compileJvm(
    sources: List<File>,
    output: File,
    classpath: String,
    lowering: Boolean?,
    compose: Boolean = false,
): Pair<ExitCode, String> {
    val arguments = mutableListOf(
        "-no-stdlib", "-no-reflect", "-jvm-target", "21", "-Xverify-ir=error",
        "-classpath", classpath,
        "-d", output.path,
        "-Xplugin=${System.getProperty("htmlLowering.pluginJar")}",
    )
    if (compose) {
        val composePlugin = File(ComposePluginRegistrar::class.java.protectionDomain.codeSource.location.toURI())
        arguments.addAll(listOf(
            "-Xplugin=${composePlugin.path}",
            "-Xcompiler-plugin-order=org.jetbrains.compose.html.lowering>androidx.compose.compiler.plugins.kotlin",
        ))
    }
    if (lowering != null) arguments.addAll(listOf("-P", "plugin:org.jetbrains.compose.html.lowering:enabled=$lowering"))
    arguments.addAll(sources.map { it.path })
    val log = ByteArrayOutputStream()
    val exit = PrintStream(log).use { K2JVMCompiler().exec(it, *arguments.toTypedArray()) }
    return exit to log.toString(Charsets.UTF_8)
}

/** Compiles a suite once in each mode and retains its outputs after deleting the temporary files. */
internal class LoweringComparison(
    private val cases: Map<String, String>,
    private val matrixCases: Set<String> = cases.keys,
    declarations: String = "",
    extraSources: Map<String, String> = emptyMap(),
) {
    val ordinary: Map<String, String>
    private val actual: Map<String, String>
    private val classes: Map<String, ByteArray>
    val bytecode: ClassReader get() = bytecode("sample.SampleKt")
    fun bytecode(className: String): ClassReader = ClassReader(classes.getValue(className))

    init {
        val directory = Files.createTempDirectory("html-lowering-comparison").toFile()
        try {
            val source = directory.resolve("Sample.kt").apply { writeText(source(declarations)) }
            val sources = listOf(source) + extraSources.map { (name, body) ->
                directory.resolve(name).apply { writeText(body) }
            }
            fun compile(lowering: Boolean): File {
                val output = directory.resolve(if (lowering) "lowered" else "ordinary")
                val (exit, log) = compileJvm(sources, output, System.getProperty("java.class.path"), lowering, compose = true)
                assertEquals(ExitCode.OK, exit, log)
                return output
            }
            ordinary = render(compile(false))
            val lowered = compile(true)
            actual = render(lowered)
            classes = lowered.walkTopDown().filter { it.isFile && it.extension == "class" }.associate {
                it.relativeTo(lowered).path.removeSuffix(".class").replace(File.separatorChar, '.') to it.readBytes()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    val writerCalls: Map<String, Map<String, Int>> by lazy {
        classMethodCalls(bytecode, "org/jetbrains/compose/web/dom/HtmlStringWriterContext").mapValues { (_, calls) ->
            calls.filter { it != "flushChunk\$default" }.groupingBy { it }.eachCount()
        }
    }

    fun assertEquivalent(names: Collection<String> = cases.keys) {
        for (name in names) {
            assertEquals(ordinary.getValue(name), actual.getValue(name), "$name: ${cases.getValue(name).take(200)}")
        }
    }

    private fun source(declarations: String): String = buildString {
        appendLine("""
            @file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class,
                org.jetbrains.compose.web.ExperimentalComposeWebSvgApi::class)
            @file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
            package sample
            import androidx.compose.runtime.Composable
            import kotlinx.browser.dom.Element
            import org.jetbrains.compose.web.composeHtmlToStream
            import org.jetbrains.compose.web.composeHtmlToString
            import org.jetbrains.compose.web.dom.*
            import org.jetbrains.compose.web.svg.*
            import org.jetbrains.compose.web.attributes.*
            import org.jetbrains.compose.web.css.*

            private val events = mutableListOf<String>()
            fun Mark(value: String): String { events.add(value); return value }
            // Inspect test-only state without publishing runtime accessors for the IR verifier.
            private fun writerField(writer: Any, name: String): Any? =
                writer.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(writer)
            @Composable fun Probe() {
                val writer = currentHtmlStringWriter()
                Text("[${'$'}{writerField(writer, "currentTagName")}|${'$'}{writerField(writer, "currentNamespace")}|${'$'}{writerField(writer, "previousSiblingWasText")}]")
            }
        """.trimIndent())
        appendLine(declarations)
        for ((name, body) in cases) {
            appendLine("@Composable fun $name(name: String) {")
            appendLine(body.prependIndent("    "))
            appendLine("}")
        }
        appendLine("""
            fun render(case: String, strict: Boolean, hydratable: Boolean, stream: Boolean): String {
                events.clear()
                val observed = StringBuilder()
                val content: @Composable () -> Unit = { sample(case, "dynamic<&é😀") }
                val result = try {
                    if (!stream) composeHtmlToString(validateStrictly = strict, hydratable = hydratable, content = content)
                    else {
                        composeHtmlToStream(validateStrictly = strict, hydratable = hydratable, chunkSize = 1,
                            sink = { observed.append(it) }, content = content)
                        observed.toString()
                    }
                } catch (t: Throwable) {
                    t.javaClass.name + ":" + t.message + "|" + observed
                }
                return result + "|events=" + events
            }
        """.trimIndent())
        appendLine("@Composable private fun sample(case: String, name: String) {")
        appendLine("    when (case) {")
        for (name in cases.keys) appendLine("        \"$name\" -> $name(name)")
        appendLine("    }")
        appendLine("}")
    }

    private fun render(output: File): Map<String, String> =
        URLClassLoader(arrayOf(output.toURI().toURL()), javaClass.classLoader).use { loader ->
            val method = loader.loadClass("sample.SampleKt").getMethod(
                "render", String::class.java, Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            )
            cases.keys.associateWith { name ->
                buildList {
                    if (name !in matrixCases) add(method.invoke(null, name, true, false, false).toString())
                    else forEachRenderMode { strict, hydratable, stream ->
                        add("strict=$strict hydratable=$hydratable stream=$stream: " +
                            method.invoke(null, name, strict, hydratable, stream))
                    }
                }.joinToString("\n")
            }
        }
}
