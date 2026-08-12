// Collects portable instance members and constructors.
package prototype.dom.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal class ScannedMembers(
    val properties: List<PortableProperty>,
    val functions: List<PortableFunction>,
    val constructors: List<PortableConstructor>,
    val keys: Set<String>,
) {
    companion object {
        val EMPTY = ScannedMembers(emptyList(), emptyList(), emptyList(), emptySet())
    }
}

/** Collects portable members declared by a classifier. */
internal class MemberScanner(
    types: PortableTypeMapper,
    private val coverage: CoverageLedger,
    private val requestedDependencies: MutableSet<String>,
) {
    private val signatures = SignatureAnalyzer(types)

    fun scan(
        declaration: KSClassDeclaration,
        inheritedKeys: Set<String>,
        overrideKeys: Set<String> = emptySet(),
    ): ScannedMembers {
        // Star projection would erase a generic classifier's own T from its declared members.
        val ownType = declaration.takeIf { it.typeParameters.isEmpty() }?.asStarProjectedType()
        val targetName = declaration.qualifiedName?.asString().orEmpty()
        val properties = mutableListOf<PortableProperty>()
        val functions = mutableListOf<PortableFunction>()
        val declaredConstructors = mutableListOf<KSFunctionDeclaration>()
        val keys = mutableSetOf<String>()

        fun <T> accept(
            member: KSDeclaration,
            analysis: SignatureAnalysis<T>,
            signatureOf: (T) -> PortableDeclarationSignature,
            asOverride: (T) -> T,
            output: MutableList<T>,
        ) {
            requestedDependencies += analysis.dependencies
            val subject = member.coverageSubject(targetName)
            val decision = analysis.decision
            if (decision is SignatureDecision.Skipped) {
                coverage.skipped(CoverageKind.MEMBER, subject, decision.reason, decision.detail)
                return
            }
            val value = decision.value()
            val signature = signatureOf(value)
            val key = signature.render()
            when {
                key in inheritedKeys -> coverage.ported(
                    CoverageKind.MEMBER,
                    subject,
                    "provided by a portable supertype",
                )
                !keys.add(key) -> coverage.ported(CoverageKind.MEMBER, subject, "deduplicated on $targetName")
                else -> {
                    output += if (key in overrideKeys) asOverride(value) else value
                    coverage.ported(CoverageKind.MEMBER, subject, "emitted on $targetName")
                }
            }
        }

        fun add(member: KSDeclaration) {
            when (member) {
                is KSPropertyDeclaration -> accept(
                    member,
                    signatures.property(member, ownType),
                    PortableProperty::signatureKey,
                    { it.copy(overrides = true) },
                    properties,
                )
                is KSFunctionDeclaration -> if (!member.isConstructor()) {
                    accept(
                        member,
                        signatures.function(member, ownType, keepDefaults = true),
                        PortableFunction::signatureKey,
                        { it.copy(overrides = true) },
                        functions,
                    )
                } else {
                    declaredConstructors += member
                }
                else -> coverage.skipped(
                    CoverageKind.MEMBER,
                    member.coverageSubject(targetName),
                    SkipReason.UNSUPPORTED_DECLARATION_KIND,
                    member::class.simpleName.orEmpty(),
                )
            }
        }

        declaration.getDeclaredProperties().forEach(::add)
        declaration.getDeclaredFunctions().forEach(::add)
        coverage.auditNestedDeclarations(declaration, targetName)

        return ScannedMembers(properties, functions, constructors(declaration, targetName, declaredConstructors), keys)
    }

    /** Ports constructors, omitting a lone no-argument constructor that Kotlin synthesizes. */
    private fun constructors(
        declaration: KSClassDeclaration,
        targetName: String,
        declared: List<KSFunctionDeclaration>,
    ): List<PortableConstructor> {
        if (declared.isEmpty()) return emptyList()
        val primary = declaration.primaryConstructor

        val ported = mutableListOf<Pair<KSFunctionDeclaration, PortableConstructor>>()
        declared.forEach { member ->
            val subject = member.coverageSubject(targetName)
            val analysis = signatures.constructor(member, primary)
            requestedDependencies += analysis.dependencies
            val decision = analysis.decision
            if (decision is SignatureDecision.Skipped) {
                coverage.skipped(CoverageKind.CONSTRUCTOR, subject, decision.reason, decision.detail)
            } else {
                ported += member to decision.value()
            }
        }

        val redundant = ported.size == 1 && ported.single().second.parameters.isEmpty()
        ported.forEach { (member, _) ->
            val subject = member.coverageSubject(targetName)
            if (redundant) {
                coverage.skipped(
                    CoverageKind.CONSTRUCTOR,
                    subject,
                    SkipReason.IMPLICIT_CONSTRUCTOR,
                    "the constructor Kotlin synthesizes for $targetName is the same one",
                )
            } else {
                coverage.ported(CoverageKind.CONSTRUCTOR, subject, "emitted on $targetName")
            }
        }
        return if (redundant) emptyList() else ported.map { (_, ported) -> ported }
    }
}
