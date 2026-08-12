// Emits JVM actual facade source files.
package prototype.dom.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT

// JVM file emitters.

internal fun jvmDeclarationsFile(
    mapping: PortablePackageMapping,
    declarations: List<PortableClass>,
    extensions: List<PortableExtensionFunction>,
    values: JvmStubValues,
    constants: JvmConstantValues,
): FileSpec = facadeFile(mapping.portablePackage, mapping.declarationsFile) {
    jvmSuppressions(declarations, values)
    declarations.forEach { addType(it.jvmType(values, constants)) }
    extensions.forEach { addFunction(it.jvmSpec(values)) }
}

internal fun jvmValuesFile(
    mapping: PortablePackageMapping,
    values: List<PortableExtensionValue>,
): FileSpec = facadeFile(mapping.portablePackage, mapping.valuesFile) {
    values.forEach { value ->
        addType(
            TypeSpec.objectBuilder(value.jvmSingleton)
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(value.portableOwner)
                .build(),
        )
    }
    values.forEach { addProperty(it.jvmSpec()) }
}

internal fun jvmDictionariesFile(
    mapping: PortablePackageMapping,
    dictionaries: List<PortableClass>,
    values: JvmStubValues,
    constants: JvmConstantValues,
): FileSpec = facadeFile(mapping.portablePackage, mapping.dictionariesFile) {
    jvmSuppressions(dictionaries, values)
    dictionaries.forEach { addType(it.jvmType(values, constants)) }
    dictionaries.forEach { addJvmDictionaryState(it, values) }
}

// JVM class and member specs.

/** Emits a JVM actual, applying specialized members before shared inheritance and constructors. */
private fun PortableClass.jvmType(
    values: JvmStubValues,
    constants: JvmConstantValues,
): TypeSpec = typeBuilder()
    .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL)
    .apply {
        shape.modifier?.let { addModifiers(it) }
        this@jvmType.typeVariables.forEach(::addTypeVariable)
        when (shape) {
            ClassShape.INTERFACE -> portableParentName?.let(::addSuperinterface)
            else -> portableParentName?.let(::superclass)
        }
        portableSuperinterfaces.forEach { addSuperinterface(it) }
        if (isJsAny) addSuperinterface(PORTABLE_JS_ANY)
        if (isInterface) {
            properties.forEach { addProperty(if (isDictionary) it.jvmAbstractSpec() else it.jvmInterfaceSpec(values)) }
            functions.forEach { addFunction(it.jvmAbstractSpec(values)) }
        } else {
            if (!addJvmCssStyleDeclarationMembers(this@jvmType, values)) {
                properties.forEach { addProperty(it.jvmSpec(values)) }
                functions.forEach { addFunction(it.jvmSpec(this@jvmType, values)) }
            }
            addJvmInterfaceMembers(this@jvmType, values)
            addJvmConstructors(this@jvmType, values)
        }
        companion?.let { addType(it.jvmSpec(this@jvmType, values, constants)) }
    }
    .build()

/** Selects constructor delegation and adds the fallback required by generated subclasses. */
private fun TypeSpec.Builder.addJvmConstructors(owner: PortableClass, values: JvmStubValues) {
    val primary = owner.jvmPrimaryConstructor ?: return
    primaryConstructor(primary.jvmPrimarySpec(owner, values))
    owner.constructors
        .filterNot { it === primary }
        .forEach { addFunction(it.jvmSecondarySpec(primary, values)) }
    if (owner.needsJvmFallbackConstructor) {
        addFunction(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.PROTECTED)
                .addKdoc(
                    "No-argument constructor used by generated JVM subclasses of %L.",
                    owner.portableName.simpleName,
                )
                .callThisConstructor(primary.jvmArguments(values))
                .build(),
        )
    }
}

/** Adds mixin members not already declared or supplied by a class ancestor. */
private fun TypeSpec.Builder.addJvmInterfaceMembers(owner: PortableClass, values: JvmStubValues) {
    val emittedKeys = buildSet {
        owner.properties.forEach { add(it.key()) }
        owner.functions.forEach { add(it.key()) }
    }
    addJvmStoredMembers(owner, values.jvmStoredInterfaces(owner), values, emittedKeys)
}

/** Materializes inherited mixin members as deduplicated JVM overrides with substituted types. */
internal fun TypeSpec.Builder.addJvmStoredMembers(
    owner: PortableClass,
    mixins: List<PortableClass>,
    values: JvmStubValues,
    emittedKeys: Set<String> = emptySet(),
) {
    val keys = emittedKeys.toMutableSet()
    mixins.forEach { mixin ->
        val substitutions = owner.typeSubstitutionsFor(mixin)
        mixin.properties.forEach { property ->
            if (keys.add(property.key())) {
                val inherited = property.substituteTypes(substitutions)
                addProperty(inherited.jvmStoredSpec(owner, values))
            }
        }
        mixin.functions.forEach { function ->
            if (keys.add(function.key())) {
                addFunction(function.substituteTypes(substitutions).jvmOverrideSpec(owner, values))
            }
        }
    }
}

private fun PortableCompanion.jvmSpec(
    owner: PortableClass,
    values: JvmStubValues,
    constants: JvmConstantValues,
): TypeSpec =
    TypeSpec.companionObjectBuilder()
    .addModifiers(KModifier.ACTUAL)
    .apply {
        properties.forEach { addProperty(it.jvmSpec(constants)) }
        functions.forEach { addFunction(it.jvmSpec(owner, values)) }
    }
    .build()

private fun PortableConstant.jvmSpec(constants: JvmConstantValues): PropertySpec = PropertySpec.builder(name, type)
    .addModifiers(KModifier.ACTUAL)
    .initializer(constants.initializer(this))
    .build()

/** Seeds visible mutable properties from matching primary-constructor parameters. */
private fun PortableConstructor.jvmPrimarySpec(owner: PortableClass, values: JvmStubValues): FunSpec {
    val constructor = this
    return FunSpec.constructorBuilder()
        .addModifiers(KModifier.ACTUAL)
        .apply {
            constructor.jvmParameters(this, values)
            owner.jvmStoredParameters(constructor, values).forEach {
                addStatement("this.%N = %N", it.name, it.name)
            }
        }
        .build()
}

private fun PortableConstructor.jvmSecondarySpec(
    primary: PortableConstructor,
    values: JvmStubValues,
): FunSpec {
    val constructor = this
    return FunSpec.constructorBuilder()
        .addModifiers(KModifier.ACTUAL)
        .apply { constructor.jvmParameters(this, values) }
        .callThisConstructor(primary.jvmArguments(values))
        .build()
}

private fun PortableConstructor.jvmParameters(builder: FunSpec.Builder, values: JvmStubValues) {
    parameters.forEach { builder.addParameter(it.spec(if (it.hasDefault) values.value(it.type) else null)) }
}

private fun PortableConstructor.jvmArguments(values: JvmStubValues): List<CodeBlock> =
    if (callableWithoutArguments) emptyList() else parameters.map { values.value(it.type) }

/** Finds constructor parameters that seed matching mutable properties visible on this class. */
private fun PortableClass.jvmStoredParameters(
    constructor: PortableConstructor,
    values: JvmStubValues,
): List<PortableParameter> {
    val chain = listOf(this) + ancestors.mapNotNull(values.classes::get)
    val visible = chain + chain.flatMap { values.mixinClosure(it).mapNotNull(values.classes::get) }
    val mutable = visible.flatMap(PortableClass::properties).filter(PortableProperty::mutable)
    return constructor.parameters.filter { parameter ->
        !parameter.isVararg && mutable.any { it.name == parameter.name && it.type == parameter.type }
    }
}

internal fun jvmActualModifiers(overrides: Boolean, open: Boolean): List<KModifier> = when {
    overrides -> listOf(KModifier.ACTUAL, KModifier.OVERRIDE)
    open -> listOf(KModifier.ACTUAL, KModifier.OPEN)
    else -> listOf(KModifier.ACTUAL)
}

internal fun PortableProperty.jvmSpec(values: JvmStubValues): PropertySpec =
    jvmSpec(
        values,
        jvmActualModifiers(overrides, open),
    )

private fun PortableProperty.jvmAbstractSpec(): PropertySpec = jvmSpec(values = null, listOf(KModifier.ACTUAL))

/** Gives browser default accessors inert JVM bodies while true abstract members stay abstract. */
private fun PortableProperty.jvmInterfaceSpec(values: JvmStubValues): PropertySpec =
    if (abstractInBrowser) {
        jvmAbstractSpec()
    } else {
        PropertySpec.builder(name, type)
            .mutable(mutable)
            .addModifiers(KModifier.ACTUAL)
            .getter(FunSpec.getterBuilder().addStatement("return %L", values.value(type)).build())
            .apply { if (mutable) setter(FunSpec.setterBuilder().addParameter("value", type).build()) }
            .build()
    }

internal fun PortableProperty.jvmOverrideSpec(values: JvmStubValues): PropertySpec =
    jvmSpec(values, listOf(KModifier.OVERRIDE))

/** The shared JVM property shape: [modifiers], and an inert initializer unless it stays abstract. */
private fun PortableProperty.jvmSpec(values: JvmStubValues?, modifiers: List<KModifier>): PropertySpec =
    PropertySpec.builder(name, type)
        .mutable(mutable)
        .addModifiers(modifiers)
        .apply { values?.let { initializer(it.value(type)) } }
        .build()

internal fun PortableFunction.jvmSpec(owner: PortableClass, values: JvmStubValues): FunSpec = jvmSpec(
    values,
    jvmActualModifiers(overrides, open),
    resultOwner = owner,
)

private fun PortableFunction.jvmAbstractSpec(values: JvmStubValues): FunSpec =
    jvmSpec(values, listOf(KModifier.ACTUAL, KModifier.ABSTRACT), resultOwner = null)

private fun PortableFunction.jvmOverrideSpec(owner: PortableClass, values: JvmStubValues): FunSpec =
    jvmSpec(values, listOf(KModifier.OVERRIDE), resultOwner = owner, keepDefaults = false)

/** Builds a JVM function with optional defaults and either an inert body or abstract declaration. */
private fun PortableFunction.jvmSpec(
    values: JvmStubValues,
    modifiers: List<KModifier>,
    resultOwner: PortableClass?,
    keepDefaults: Boolean = true,
): FunSpec {
    val function = this
    return specBuilder {
        it.spec(if (keepDefaults && it.hasDefault) values.value(it.type) else null)
    }
        .addModifiers(modifiers)
        .apply {
            if (function.returnType != UNIT) {
                resultOwner?.let { addStatement("return %L", function.jvmResult(it, values)) }
            }
        }
        .build()
}

/** Preserves matching reference identity; strings and unrelated results use inert values. */
private fun PortableFunction.jvmResult(owner: PortableClass, values: JvmStubValues): CodeBlock {
    val bare = returnType.copy(nullable = false)
    val argument = parameters.firstOrNull { bare != STRING && !it.isVararg && it.type == bare }
    if (argument != null) return CodeBlock.of("%N", argument.name)
    if (!returnType.isNullable && owner.isInstanceOf(bare)) return CodeBlock.of("this")
    return values.value(returnType)
}

private fun PortableClass.isInstanceOf(type: TypeName): Boolean =
    portableName == type || ancestors.any { it == type }

/** Delegates JVM operators to a matching member, or returns an inert result. */
private fun PortableExtensionFunction.jvmSpec(values: JvmStubValues): FunSpec {
    val extension = this
    val delegate = jvmDelegate(values)
    val arguments = function.parameters.joinToString { it.name }
    return function.specBuilder { it.spec(default = null) }
        .receiver(receiverType)
        .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL, KModifier.OPERATOR)
        .apply {
            when {
                delegate != null && extension.function.returnType == UNIT ->
                    addStatement("this.%N(%L)", delegate.name, arguments)
                delegate != null -> addStatement("return this.%N(%L)", delegate.name, arguments)
                extension.function.returnType != UNIT ->
                    addStatement("return %L", values.value(extension.function.returnType))
            }
        }
        .build()
}

/** Finds an exact member shape that a generated JVM `get` or `set` operator can delegate to. */
private fun PortableExtensionFunction.jvmDelegate(
    values: JvmStubValues,
): PortableFunction? {
    val receiver = receiverType as? ClassName ?: return null
    val owner = values.classes[receiver] ?: return null
    val candidateNames = when (function.name) {
        "get" -> listOf("item", "namedItem", "getItem", "getNamedItem")
        "set" -> listOf("setItem")
        else -> emptyList()
    }
    val candidateOwners = buildList {
        add(owner)
        owner.ancestors.mapNotNullTo(this, values.classes::get)
        values.mixinClosure(owner).mapNotNullTo(this, values.classes::get)
    }
    val candidates = candidateOwners.flatMap { candidate ->
        val substitutions = owner.typeSubstitutionsFor(candidate)
        candidate.functions.map { it.substituteTypes(substitutions) }
    }
    return candidateNames.firstNotNullOfOrNull { name ->
        candidates.firstOrNull { candidate ->
            candidate.name == name &&
                candidate.returnType == function.returnType &&
                candidate.parameters.map(PortableParameter::type) == function.parameters.map(PortableParameter::type)
        }
    }
}

private fun PortableExtensionValue.jvmSpec(): PropertySpec =
    PropertySpec.builder(name, portableOwner)
        .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL)
        .receiver(portableOwner.companionName())
        .getter(FunSpec.getterBuilder().addStatement("return %T", jvmSingleton).build())
        .build()

/** Adds only the JVM suppressions required by the emitted class shapes. */
private fun FileSpec.Builder.jvmSuppressions(
    classes: List<PortableClass>,
    values: JvmStubValues,
): FileSpec.Builder {
    val names = buildList {
        if (classes.any(PortableClass::hasDefaultArguments)) {
            add(JVM_DEFAULT_ARGUMENTS_SUPPRESSION)
        }
        if (classes.any { values.jvmStoredInterfaces(it).any { mixin -> mixin.instanceMemberCount > 0 } }) {
            add(MODALITY_SUPPRESSION)
        }
    }
    return suppressIfAny(names)
}
