/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies that browser facade actuals are staged beside each target's interop actuals.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.UNIT
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class FacadeSourceEmitterTest {
    @Test
    fun browserAliasesAreEmittedAfterEachTargetActualizesJsAny() {
        val worker = portableInterface("AbstractWorker")
        val options = portableInterface("EventListenerOptions", isDictionary = true)
        val packageModel = FacadePackageModel(
            mapping = DOM,
            declarations = listOf(worker),
            dictionaries = listOf(options),
            extensions = emptyList(),
            values = emptyList(),
        )
        val generator = RecordingCodeGenerator()

        FacadeSourceEmitter(generator).emit(
            Dependencies(aggregating = false),
            packages = listOf(packageModel),
            model = listOf(worker, options),
        )

        val common = generator["portableDom.commonMain.kotlin.kotlinx.browser.dom.Core.kt.txt"] +
            generator["portableDom.commonMain.kotlin.kotlinx.browser.dom.PortableDom.kt.txt"] +
            generator["portableDom.commonMain.kotlin.kotlinx.browser.dom.OptionDictionaries.kt.txt"]
        val js = generator["portableDom.jsMain.kotlin.kotlinx.browser.PortableInterop.kt.txt"] +
            generator["portableDom.jsMain.kotlin.kotlinx.browser.dom.PortableDom.kt.txt"] +
            generator["portableDom.jsMain.kotlin.kotlinx.browser.dom.OptionDictionaries.kt.txt"]
        val wasmJs = generator["portableDom.wasmJsMain.kotlin.kotlinx.browser.PortableInterop.kt.txt"] +
            generator["portableDom.wasmJsMain.kotlin.kotlinx.browser.dom.PortableDom.kt.txt"] +
            generator["portableDom.wasmJsMain.kotlin.kotlinx.browser.dom.OptionDictionaries.kt.txt"]

        assertContains(common, "public expect interface AbstractWorker : JsAny")
        assertContains(common, "public expect interface EventListenerOptions : JsAny")
        assertContains(js, "public actual typealias JsAny = Any")
        assertContains(js, "public actual typealias AbstractWorker = AbstractWorker")
        assertContains(js, "public actual typealias EventListenerOptions = EventListenerOptions")
        assertContains(wasmJs, "public actual typealias JsAny = BrowserJsAny")
        assertContains(wasmJs, "public actual typealias AbstractWorker = AbstractWorker")
        assertContains(wasmJs, "public actual typealias EventListenerOptions = EventListenerOptions")
        assertFalse(generator.paths.any { ".webMain." in it }, generator.paths.joinToString())
        assertFalse(generator.paths.any { ".jsTest." in it || ".wasmJsTest." in it }, generator.paths.joinToString())
        assertFalse(generator.paths.any { ".webTest." in it }, generator.paths.joinToString())
    }

    @Test
    fun browserAliasesInheritCompatibilitySuppressionsFromPortableParents() {
        val parent = PortableClass(
            browserName = ClassName(DOM_SVG_PACKAGE, "SVGElement"),
            parentBrowserName = null,
            superinterfaces = emptyList(),
            ancestors = emptyList(),
            shape = ClassShape.ABSTRACT,
            isDictionary = false,
            isJsAny = true,
            properties = emptyList(),
            functions = listOf(
                PortableFunction(
                    name = "scroll",
                    parameters = listOf(
                        PortableParameter(
                            name = "options",
                            type = ClassName(PORTABLE_DOM_PACKAGE, "ScrollToOptions"),
                            isVararg = false,
                            hasDefault = true,
                        ),
                    ),
                    returnType = UNIT,
                    open = true,
                    abstractInBrowser = false,
                ),
            ),
            constructors = emptyList(),
            companion = null,
            factory = null,
            sourceFile = null,
        )
        val child = parent.copy(
            browserName = ClassName(CSS_MASKING_PACKAGE, "SVGMaskElement"),
            parentBrowserName = parent.browserName,
            ancestors = listOf(parent.portableName),
            functions = emptyList(),
        )
        val mapping = PortablePackageMapping(PORTABLE_CSS_MASKING_PACKAGE, "PortableMasking", "MaskingDictionaries")
        val classes = listOf(parent, child).associateBy(PortableClass::portableName)

        val web = browserLeafDeclarationsFile(mapping, listOf(child), emptyList(), classes).toString()

        assertContains(web, DEFAULT_ARGUMENTS_SUPPRESSION)
        assertContains(web, CLASS_SCOPE_SUPPRESSION)
    }
}

private val DOM = PortablePackageMapping(PORTABLE_DOM_PACKAGE, "PortableDom", "OptionDictionaries")

private fun portableInterface(name: String, isDictionary: Boolean = false): PortableClass = PortableClass(
    browserName = ClassName(DOM_PACKAGE, name),
    parentBrowserName = null,
    superinterfaces = emptyList(),
    ancestors = emptyList(),
    shape = ClassShape.INTERFACE,
    isDictionary = isDictionary,
    isJsAny = true,
    properties = emptyList(),
    functions = emptyList(),
    constructors = emptyList(),
    companion = null,
    factory = null,
    sourceFile = null,
)

private class RecordingCodeGenerator : CodeGenerator {
    private val outputs = linkedMapOf<String, ByteArrayOutputStream>()

    val paths: Set<String>
        get() = outputs.keys

    operator fun get(path: String): String = outputs.getValue(path).toString(Charsets.UTF_8)

    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String,
    ): OutputStream = ByteArrayOutputStream().also { outputs["$packageName.$fileName.$extensionName"] = it }

    override fun createNewFileByPath(
        dependencies: Dependencies,
        path: String,
        extensionName: String,
    ): OutputStream = error("Unexpected path-based output: $path.$extensionName")

    override fun associate(
        sources: List<KSFile>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = Unit

    override fun associateByPath(sources: List<KSFile>, path: String, extensionName: String) = Unit

    override fun associateWithClasses(
        classes: List<KSClassDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = Unit

    override val generatedFile: Collection<File>
        get() = emptyList()
}
