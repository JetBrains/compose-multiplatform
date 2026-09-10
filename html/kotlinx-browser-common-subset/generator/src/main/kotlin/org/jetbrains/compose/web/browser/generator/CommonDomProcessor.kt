/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Orchestrates KSP discovery, model construction, reporting, and source emission.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

public class CommonDomProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        CommonDomProcessor(CommonDomGenerator(environment.codeGenerator, environment.logger))
}

// Main entry point, makes sure CommonDomGenerator.generate is only called once.
private class CommonDomProcessor(
    private val generator: CommonDomGenerator,
) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSClassDeclaration> {
        if (!generated) generated = generator.generate(resolver)
        return emptyList()
    }
}

// Generation orchestration.
internal class CommonDomGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {
    fun generate(resolver: Resolver): Boolean {
        val selection = SelectionPolicy.parse(readSelectionList("/common-dom-selection.txt"))
        val mappings = facadePackageMappings(selection.signatureOnlyPackages)
        val index = DeclarationIndex(resolver, mappings.keys)
        val input = resolveInput(index, selection) ?: return false
        val analysis = resolveCommonAnalysis(
            index,
            input.selected,
            selection.signatureOnlyPackages,
            input.selection.excludedFromClosure,
        )
        val closure = analysis.closure
        val coverage = analysis.coverage
        val model = analysis.model
        val extensions = analysis.extensions
        val values = analysis.values
        val packages = groupFacadePackages(mappings.values, model, extensions, values)
        val sourceFiles = (
            model.mapNotNull(CommonClass::sourceFile) +
                extensions.mapNotNull(CommonExtensionFunction::sourceFile) +
                values.mapNotNull(CommonExtensionValue::sourceFile)
            ).distinct().toTypedArray()
        val dependencies = Dependencies(aggregating = true, *sourceFiles)
        emitReport(dependencies, "coverage", coverage.ledgerFile())
        emitReport(dependencies, "model", modelLedger(closure, model, extensions, values))

        val manifest = buildApiManifest(
            input.files,
            coverage,
            ManifestExclusions.parse(
                readSelectionList("/dom-api-exclusions.txt") + input.selection.deferredExclusionLines,
            ),
        )
        emitReport(dependencies, "api-manifest", manifest.ledgerFile())

        if (manifest.unaccounted.isNotEmpty() || manifest.staleExclusions.isNotEmpty()) {
            reportManifestFailure(manifest)
            return true
        }

        FacadeSourceEmitter(codeGenerator).emit(dependencies, packages, model)
        logger.info(
            "Generated common DOM facade model with ${model.size} declarations, " +
                "${model.sumOf(CommonClass::memberCount)} members and " +
                "${model.sumOf { it.constructors.size }} constructors plus ${extensions.size} operators " +
                "from ${input.selected.size} input classifiers and " +
                "${closure.dependencies.size + closure.identityOnly.size} discovered dependencies",
        )
        return true
    }

    private fun resolveInput(index: DeclarationIndex, selection: SelectionPolicy): GeneratorInput? {
        val resolvedInputs = index.files
            .filter { it.fileName in selection.inputFiles }
            .map { file -> file to index.expectClassNames(file) }
            .filter { (_, classifiers) -> classifiers.isNotEmpty() }
        val inputFiles = resolvedInputs.map { it.first }
        val resolvedFiles = inputFiles.map { it.fileName }
        val classifiers = resolvedInputs.flatMapTo(mutableSetOf()) { it.second }
        val selected = selection.emittedClassifiers(classifiers)

        val errors = validateSelection(index, selection, resolvedFiles, classifiers, selected)
        errors.forEach { logger.error(it) }
        return if (errors.isEmpty()) GeneratorInput(selection, inputFiles, selected) else null
    }

    /** Validates selection before closure construction so missing inputs cannot produce partial output. */
    private fun validateSelection(
        index: DeclarationIndex,
        selection: SelectionPolicy,
        resolvedFiles: List<String>,
        classifiers: Set<String>,
        selected: Set<String>,
    ): List<String> = buildList {
        (selection.inputFiles - resolvedFiles.toSet()).forEach { fileName ->
            add("Common DOM input file does not resolve to an expect source: $fileName")
        }
        resolvedFiles.groupingBy { it }.eachCount().filterValues { it != 1 }.forEach { (fileName, count) ->
            add("Common DOM input file resolves $count times: $fileName")
        }
        addAll(selection.validationErrors(classifiers))
        selection.signatureOnlyPackages.forEach { packageName ->
            when {
                packageName in COMMON_PACKAGE_BY_BROWSER_PACKAGE ->
                    add("Signature-only package is already mapped: $packageName")
                packageName !in EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE &&
                    !packageName.startsWith("org.w3c.") ->
                    add("Signature-only package has no common naming rule: $packageName")
                !index.hasSourcePackage(packageName) ->
                    add("Signature-only package does not resolve to a source package: $packageName")
            }
        }
        selected.forEach { qualifiedName ->
            when {
                qualifiedName.browserPackage() !in COMMON_PACKAGE_BY_BROWSER_PACKAGE ->
                    add("Common DOM input classifier has no package mapping: $qualifiedName")
                !index.isSourceDeclaration(qualifiedName) ->
                    add("Common DOM input classifier is not a source declaration: $qualifiedName")
            }
        }
    }

    private fun reportManifestFailure(manifest: ApiManifest) {
        if (manifest.unaccounted.isNotEmpty()) {
            logger.error(
                "${manifest.unaccounted.size} common DOM declarations are neither emitted nor " +
                    "excluded, the first being ${manifest.unaccounted.first().key}. Every one of them " +
                    "is listed as UNACCOUNTED in api-manifest.txt; port them, or record them in " +
                    "dom-api-exclusions.txt with a reason.",
            )
        }
        if (manifest.staleExclusions.isNotEmpty()) {
            logger.error(
                "Stale common DOM exclusions, covering nothing the generator left out: " +
                    "${manifest.staleExclusions.joinToString()}. Remove them from dom-api-exclusions.txt.",
            )
        }
    }

    private fun emitReport(dependencies: Dependencies, name: String, ledger: LedgerFile) {
        codeGenerator.createNewFile(dependencies, STAGING_ROOT, name, "txt").bufferedWriter().use(ledger::writeTo)
    }

    private fun readSelectionList(resource: String): List<String> =
        checkNotNull(javaClass.getResourceAsStream(resource)) { "Missing selection list $resource" }
            .bufferedReader()
            .useLines { lines ->
                lines.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith('#') }
                    .toList()
            }
}

private data class GeneratorInput(
    val selection: SelectionPolicy,
    val files: List<KSFile>,
    val selected: Set<String>,
)
