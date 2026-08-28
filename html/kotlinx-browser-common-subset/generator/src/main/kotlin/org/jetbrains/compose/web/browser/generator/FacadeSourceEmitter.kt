// Groups the common model into facade packages and stages the generated source sets.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.FileSpec

/** The declarations and extensions emitted for one common package. */
internal data class FacadePackageModel(
    val mapping: CommonPackageMapping,
    val declarations: List<CommonClass>,
    val dictionaries: List<CommonClass>,
    val extensions: List<CommonExtensionFunction>,
    val values: List<CommonExtensionValue>,
)

/** Partitions the common model by output package and declaration kind. */
internal fun groupFacadePackages(
    mappings: Collection<CommonPackageMapping>,
    model: List<CommonClass>,
    extensions: List<CommonExtensionFunction>,
    values: List<CommonExtensionValue>,
): List<FacadePackageModel> = mappings.distinct().mapNotNull { mapping ->
    val declarations = model.filter { it.commonName.packageName == mapping.commonPackage }
    val packageModel = FacadePackageModel(
        mapping,
        declarations.filterNot(CommonClass::isDictionary),
        declarations.filter(CommonClass::isDictionary),
        extensions.filter { it.commonPackage == mapping.commonPackage },
        values.filter { it.commonPackage == mapping.commonPackage }
            .sortedBy { it.commonOwner.simpleName },
    )
    packageModel.takeIf {
        it.declarations.isNotEmpty() || it.dictionaries.isNotEmpty() ||
            it.extensions.isNotEmpty() || it.values.isNotEmpty()
    }
}

/** Builds and stages every generated facade source set. */
internal class FacadeSourceEmitter(private val codeGenerator: CodeGenerator) {
    /** Emits common, web, target-specific, and JVM sources from [packages]. */
    fun emit(
        dependencies: Dependencies,
        packages: List<FacadePackageModel>,
        model: List<CommonClass>,
    ) {
        val commonFiles = packages.flatMap { p ->
            listOfNotNull(
                commonDeclarationsFile(p.mapping, p.declarations, p.extensions),
                p.dictionaries.ifAny { commonDictionariesFile(p.mapping, it) },
                p.values.ifAny { commonValuesFile(p.mapping, it) },
            )
        }
        emitSourceSet("commonMain", dependencies, listOf(commonCoreFile()) + commonFiles)

        val browserLeafFiles = packages.flatMap { p ->
            listOfNotNull(
                browserLeafDeclarationsFile(
                    p.mapping,
                    p.declarations,
                    p.extensions.filterNot(CommonExtensionFunction::usesInterop),
                ),
                p.dictionaries.ifAny { browserLeafDictionariesFile(p.mapping, it) },
            )
        }
        val targetFiles = packages.flatMap { p ->
            val extensions = p.extensions.filter(CommonExtensionFunction::usesInterop)
            val dictionaries = p.dictionaries.filter { it.factory?.usesInterop == true }
            listOfNotNull(
                if (extensions.isEmpty() && dictionaries.isEmpty()) null
                else targetDeclarationsFile(p.mapping, extensions, dictionaries),
                p.values.ifAny { targetValuesFile(p.mapping, it) },
            )
        }
        // A facade alias can inherit common JsAny, whose handwritten actual differs by target.
        // Keep every browser-facing actual in the leaf source set where that actual is known.
        emitSourceSet("jsMain", dependencies, browserLeafFiles + targetFiles)
        emitSourceSet(
            "wasmJsMain",
            dependencies,
            browserLeafFiles + targetFiles,
        )

        val jvmValues = JvmStubValues(model.associateBy(CommonClass::commonName))
        val jvmConstants = JvmConstantValues(model.flatMap { it.companion?.properties.orEmpty() })
        val jvmFiles = packages.flatMap { p ->
            listOfNotNull(
                jvmDeclarationsFile(p.mapping, p.declarations, p.extensions, jvmValues, jvmConstants),
                p.dictionaries.ifAny {
                    jvmDictionariesFile(p.mapping, it, jvmValues, jvmConstants)
                },
                p.values.ifAny { jvmValuesFile(p.mapping, it) },
            )
        }
        // Singleton files are rendered last: emitting the JVM files above is what requests them.
        emitSourceSet("jvmMain", dependencies, jvmFiles + jvmValues.singletonFiles())
    }

    /** Writes [files] as staged KSP resources for [sourceSet]. */
    private fun emitSourceSet(sourceSet: String, dependencies: Dependencies, files: List<FileSpec>) {
        files.forEach { file ->
            val stagingPackage = listOf(STAGING_ROOT, sourceSet, "kotlin", file.packageName)
                .filter(String::isNotEmpty)
                .joinToString(".")
            codeGenerator.createNewFile(
                dependencies,
                stagingPackage,
                file.name,
                "kt.txt",
            ).bufferedWriter().use { writer ->
                writer.append(COPYRIGHT_HEADER).append("\n\n")
                file.writeTo(writer)
            }
        }
    }
}

private val COPYRIGHT_HEADER = """
    /*
     * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
     * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
     */
""".trimIndent()

private inline fun <T, R> List<T>.ifAny(transform: (List<T>) -> R): R? =
    if (isEmpty()) null else transform(this)
