// Groups the portable model into facade packages and stages the generated source sets.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.FileSpec

/** The declarations and extensions emitted for one portable package. */
internal data class FacadePackageModel(
    val mapping: PortablePackageMapping,
    val declarations: List<PortableClass>,
    val dictionaries: List<PortableClass>,
    val extensions: List<PortableExtensionFunction>,
    val values: List<PortableExtensionValue>,
)

/** Partitions the portable model by output package and declaration kind. */
internal fun groupFacadePackages(
    mappings: Collection<PortablePackageMapping>,
    model: List<PortableClass>,
    extensions: List<PortableExtensionFunction>,
    values: List<PortableExtensionValue>,
): List<FacadePackageModel> = mappings.distinct().mapNotNull { mapping ->
    val declarations = model.filter { it.portableName.packageName == mapping.portablePackage }
    val packageModel = FacadePackageModel(
        mapping,
        declarations.filterNot(PortableClass::isDictionary),
        declarations.filter(PortableClass::isDictionary),
        extensions.filter { it.portablePackage == mapping.portablePackage },
        values.filter { it.portablePackage == mapping.portablePackage }
            .sortedBy { it.portableOwner.simpleName },
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
        model: List<PortableClass>,
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
                    p.extensions.filterNot(PortableExtensionFunction::usesInterop),
                ),
                p.dictionaries.ifAny { browserLeafDictionariesFile(p.mapping, it) },
            )
        }
        val targetFiles = packages.flatMap { p ->
            val extensions = p.extensions.filter(PortableExtensionFunction::usesInterop)
            val dictionaries = p.dictionaries.filter { it.factory?.usesInterop == true }
            listOfNotNull(
                if (extensions.isEmpty() && dictionaries.isEmpty()) null
                else targetDeclarationsFile(p.mapping, extensions, dictionaries),
                p.values.ifAny { targetValuesFile(p.mapping, it) },
            )
        }
        // A facade alias can inherit portable JsAny, whose handwritten actual differs by target.
        // Keep every browser-facing actual in the leaf source set where that actual is known.
        emitSourceSet("jsMain", dependencies, browserLeafFiles + targetFiles)
        emitSourceSet(
            "wasmJsMain",
            dependencies,
            browserLeafFiles + targetFiles,
        )

        val jvmValues = JvmStubValues(model.associateBy(PortableClass::portableName))
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
