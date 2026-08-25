/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Indexes browser declarations and resolves their expect-side definitions.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

/** Indexes one KSP round and prefers `expect` declarations, which retain defaults and precise types. */
internal class DeclarationIndex(
    private val resolver: Resolver,
    private val facadePackages: Set<String>,
) {
    val files: List<KSFile> = resolver.getAllFiles().toList()

    private val topLevelDeclarations: List<KSDeclaration> by lazy {
        files.asSequence().flatMap(KSFile::declarations).toList()
    }
    private val expectClassesByFile: Map<KSFile, List<KSClassDeclaration>> by lazy {
        files.associateWith { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter(KSClassDeclaration::isExpect)
                .toList()
        }
    }
    private val expects: Map<String, KSClassDeclaration> by lazy {
        expectClassesByFile.values.asSequence()
            .flatten()
            .mapNotNull { declaration -> declaration.qualifiedName?.asString()?.let { it to declaration } }
            .toMap()
    }
    private val topLevelFunctions: Map<String, List<KSFunctionDeclaration>> by lazy {
        topLevelDeclarations.asSequence()
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { it.containingFile != null }
            .mapNotNull { declaration -> declaration.qualifiedName?.asString()?.let { it to declaration } }
            .groupBy({ it.first }, { it.second })
    }
    private val topLevelExtensions: List<Pair<String, KSDeclaration>> by lazy {
        topLevelDeclarations.asSequence()
            // Only expect declarations belong to the browser surface being ported.
            .filter { Modifier.EXPECT in it.modifiers }
            .map { it.extensionReceiverClassifierName() to it }
            .toList()
    }
    private val resolved = mutableMapOf<String, KSClassDeclaration?>()
    private val hierarchies = mutableMapOf<String, PortableHierarchy>()

    /** Qualified names of the expect classes [file] declares, in declaration order. */
    fun expectClassNames(file: KSFile): List<String> =
        expectClassesByFile.getValue(file).mapNotNull { it.qualifiedName?.asString() }

    /** The declaration for [qualifiedName], `expect` side preferred. Misses are cached as well. */
    fun declarationFor(qualifiedName: String): KSClassDeclaration? {
        if (qualifiedName in resolved) return resolved.getValue(qualifiedName)
        val declaration = expects[qualifiedName]
            ?: resolver.getClassDeclarationByName(resolver.getKSNameFromString(qualifiedName))
        resolved[qualifiedName] = declaration
        return declaration
    }

    /** Whether [qualifiedName] resolves to a source declaration the generator can read members from. */
    fun isSourceDeclaration(qualifiedName: String): Boolean =
        declarationFor(qualifiedName)?.containingFile != null

    fun hasSourcePackage(packageName: String): Boolean =
        files.any { it.packageName.asString() == packageName }

    /** Direct portable supertypes and the interop marker, resolved together and cached. */
    fun hierarchy(declaration: KSClassDeclaration): PortableHierarchy {
        val name = declaration.qualifiedName?.asString() ?: return resolveHierarchy(declaration)
        return hierarchies.getOrPut(name) { resolveHierarchy(declaration) }
    }

    /** The source top-level dictionary factory named [qualifiedName], `expect` side preferred. */
    fun topLevelFunctionFor(qualifiedName: String): KSFunctionDeclaration? =
        topLevelFunctions[qualifiedName]?.let { candidates ->
            candidates.firstOrNull(KSFunctionDeclaration::isExpect) ?: candidates.first()
        }

    /** Top-level extension functions/properties whose receiver is one of [classifierNames]. */
    fun topLevelExtensionsFor(classifierNames: Set<String>): List<KSDeclaration> =
        topLevelExtensions.mapNotNull { (owner, declaration) -> declaration.takeIf { owner in classifierNames } }

    private fun resolveHierarchy(declaration: KSClassDeclaration): PortableHierarchy {
        fun KSClassDeclaration.expectSide(): KSClassDeclaration =
            qualifiedName?.asString()?.let(::declarationFor) ?: this

        val supertypes = declaration.superTypes.map { it.resolve() }.filterNot { it.isError }.toList()
        val classifiers = supertypes.mapNotNull { type ->
            (type.declaration as? KSClassDeclaration)?.let { PortableSupertype(it.expectSide(), type) }
        }

        return PortableHierarchy(
            classSupertypes = classifiers,
            parent = classifiers.firstOrNull { candidate ->
                candidate.declaration.classKind == ClassKind.CLASS &&
                    candidate.qualifiedName.browserPackage() in facadePackages
            },
            interfaces = classifiers.filter { candidate ->
                candidate.declaration.classKind == ClassKind.INTERFACE &&
                    candidate.qualifiedName.browserPackage() in facadePackages
            },
            declaresJsAny = supertypes.any {
                it.declaration.qualifiedName?.asString() == BROWSER_JS_ANY.canonicalName
            },
        )
    }
}

internal data class PortableHierarchy(
    val classSupertypes: List<PortableSupertype>,
    val parent: PortableSupertype?,
    val interfaces: List<PortableSupertype>,
    val declaresJsAny: Boolean,
)

/** A direct browser supertype together with its concrete type arguments. */
internal data class PortableSupertype(
    val declaration: KSClassDeclaration,
    val type: KSType,
) {
    val qualifiedName: String?
        get() = declaration.qualifiedName?.asString()
}

/** The classifier a top-level function/property receiver belongs to, companions unwrapped. */
internal fun KSDeclaration.extensionReceiverClassifierName(): String {
    val receiver = when (this) {
        is KSFunctionDeclaration -> extensionReceiver
        is KSPropertyDeclaration -> extensionReceiver
        else -> null
    }?.let { runCatching { it.resolve().declaration as? KSClassDeclaration }.getOrNull() }
    val classifier = if (receiver?.isCompanionObject == true) {
        receiver.parentDeclaration as? KSClassDeclaration
    } else {
        receiver
    }
    return classifier?.qualifiedName?.asString() ?: "<unknown-receiver>"
}

/** Stable coverage/manifest subject for a top-level extension. */
internal fun KSDeclaration.topLevelExtensionSubject(): String =
    "${extensionReceiverClassifierName()}#${coverageSignature()}"
