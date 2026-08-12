// Emits stateful JVM stubs for option dictionaries and CSSStyleDeclaration.
package prototype.dom.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

// Stateful option dictionaries.

private data class JvmDictionaryProperty(
    val property: PortableProperty,
    val parameter: PortableParameter,
)

/** Adds the backing implementation and factory that retain dictionary arguments on JVM. */
internal fun FileSpec.Builder.addJvmDictionaryState(dictionary: PortableClass, values: JvmStubValues) {
    val factory = dictionary.factory ?: return
    val implementation = dictionary.jvmDictionaryImplementationName()
    val properties = dictionary.jvmDictionaryProperties(factory, values)
    addType(dictionary.jvmDictionaryImplementation(implementation, properties))
    addFunction(dictionary.jvmFactory(factory, implementation, properties))
}

/** Pairs visible dictionary properties with factory parameters, including KSP keyword spellings. */
private fun PortableClass.jvmDictionaryProperties(
    factory: PortableFactory,
    values: JvmStubValues,
): List<JvmDictionaryProperty> {
    val visibleDictionaries = listOf(this) + values.mixinClosure(this).mapNotNull(values.classes::get)
    val visibleProperties = visibleDictionaries
        .flatMap(PortableClass::properties)
        .distinctBy { "${it.name}:${it.type}" }

    return visibleProperties.map { property ->
        val parameterNames = setOf(property.name, "${property.name}_", "param_${property.name}")
        val parameter = factory.parameters.singleOrNull {
            it.name in parameterNames && it.type == property.type
        }
        checkNotNull(parameter) {
            "No factory parameter initializes ${portableName.canonicalName}.${property.name}: " +
                "expected one of $parameterNames with type ${property.type}"
        }
        JvmDictionaryProperty(property, parameter)
    }.sortedBy { factory.parameters.indexOf(it.parameter) }
}

private fun PortableClass.jvmDictionaryImplementation(
    implementation: ClassName,
    properties: List<JvmDictionaryProperty>,
): TypeSpec = TypeSpec.classBuilder(implementation)
    .addModifiers(KModifier.PRIVATE)
    .primaryConstructor(
        FunSpec.constructorBuilder()
            .apply { properties.forEach { addParameter(it.parameter.spec(null)) } }
            .build(),
    )
    .addSuperinterface(portableName)
    .apply {
        properties.forEach { stored ->
            addProperty(
                PropertySpec.builder(stored.property.name, stored.property.type)
                    .mutable(stored.property.mutable)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%N", stored.parameter.name)
                    .build(),
            )
        }
    }
    .build()

private fun PortableClass.jvmFactory(
    factory: PortableFactory,
    implementation: ClassName,
    properties: List<JvmDictionaryProperty>,
): FunSpec = FunSpec.builder(portableName.simpleName)
    .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL)
    .returns(portableName)
    .apply { factory.parameters.forEach { addParameter(it.spec(null)) } }
    .addStatement(
        "return %T(%L)",
        implementation,
        CodeBlock.builder()
            .apply {
                properties.forEachIndexed { index, stored ->
                    if (index > 0) add(", ")
                    add("%N", stored.parameter.name)
                }
            }
            .build(),
    )
    .build()

private fun PortableClass.jvmDictionaryImplementationName(): ClassName =
    ClassName(portableName.packageName, "Jvm${portableName.simpleName}")

// Stateful CSSStyleDeclaration.

private const val JVM_CSS_PROPERTY_VALUES = "jvmPropertyValues"
private const val JVM_CSS_PROPERTY_PRIORITIES = "jvmPropertyPriorities"
private val STRING_MAP = ClassName("kotlin.collections", "MutableMap").parameterizedBy(STRING, STRING)

/** Emits the specialized declared members, or returns false for an ordinary JVM facade class. */
internal fun TypeSpec.Builder.addJvmCssStyleDeclarationMembers(
    owner: PortableClass,
    values: JvmStubValues,
): Boolean {
    if (!owner.isCssStyleDeclaration) return false

    addJvmCssStyleState()
    owner.properties.forEach { addProperty(it.jvmCssStyleSpec(values)) }
    owner.functions.forEach { addFunction(it.jvmCssStyleSpec(owner, values)) }
    return true
}

/** Selects the specialized inherited property shape required by CSSStyleDeclaration. */
internal fun PortableProperty.jvmStoredSpec(owner: PortableClass, values: JvmStubValues): PropertySpec =
    if (owner.isCssStyleDeclaration && name == "length") jvmCssStyleLengthSpec() else jvmOverrideSpec(values)

private val PortableClass.isCssStyleDeclaration: Boolean
    get() = browserName.packageName == DOM_CSS_PACKAGE && browserName.simpleName == "CSSStyleDeclaration"

/** Adds ordered value and priority stores shared by the typed and string APIs. */
private fun TypeSpec.Builder.addJvmCssStyleState() {
    addProperty(
        PropertySpec.builder(JVM_CSS_PROPERTY_VALUES, STRING_MAP)
            .addModifiers(KModifier.PRIVATE)
            .initializer("linkedMapOf()")
            .build(),
    )
    addProperty(
        PropertySpec.builder(JVM_CSS_PROPERTY_PRIORITIES, STRING_MAP)
            .addModifiers(KModifier.PRIVATE)
            .initializer("linkedMapOf()")
            .build(),
    )
}

/** Backs typed mutable strings except cssText with the shared CSS state. */
private fun PortableProperty.jvmCssStyleSpec(values: JvmStubValues): PropertySpec {
    if (!mutable || type != STRING || name == "cssText") return jvmSpec(values)

    val propertyName = name.cssPropertyName()
    val modifiers = jvmActualModifiers(overrides, open)
    return PropertySpec.builder(name, type)
        .mutable(true)
        .addModifiers(modifiers)
        .getter(
            FunSpec.getterBuilder()
                .addStatement("return %L[%S].orEmpty()", JVM_CSS_PROPERTY_VALUES, propertyName)
                .build(),
        )
        .setter(
            FunSpec.setterBuilder()
                .addParameter("value", STRING)
                .beginControlFlow("if (value.isEmpty())")
                .addStatement("%L.remove(%S)", JVM_CSS_PROPERTY_VALUES, propertyName)
                .addStatement("%L.remove(%S)", JVM_CSS_PROPERTY_PRIORITIES, propertyName)
                .nextControlFlow("else")
                .addStatement("%L[%S] = value", JVM_CSS_PROPERTY_VALUES, propertyName)
                .addStatement("%L.remove(%S)", JVM_CSS_PROPERTY_PRIORITIES, propertyName)
                .endControlFlow()
                .build(),
        )
        .build()
}

/** Derives the inherited length from the shared declaration order. */
private fun PortableProperty.jvmCssStyleLengthSpec(): PropertySpec = PropertySpec.builder(name, type)
    .addModifiers(KModifier.OVERRIDE)
    .getter(
        FunSpec.getterBuilder()
            .addStatement("return %L.size", JVM_CSS_PROPERTY_VALUES)
            .build(),
    )
    .build()

/** Converts Kotlin camel case to CSS kebab case, including cssFloat to float. */
private fun String.cssPropertyName(): String = when (this) {
    "cssFloat" -> "float"
    else -> buildString {
        this@cssPropertyName.forEach { character ->
            if (character.isUpperCase()) append('-')
            append(character.lowercaseChar())
        }
    }
}

/** Gives stateful CSS string methods real bodies and leaves other methods inert. */
private fun PortableFunction.jvmCssStyleSpec(owner: PortableClass, values: JvmStubValues): FunSpec {
    if (name !in CSS_STYLE_FUNCTIONS) return jvmSpec(owner, values)

    val function = this
    val modifiers = jvmActualModifiers(overrides, open)
    return specBuilder {
        it.spec(if (it.hasDefault) values.value(it.type) else null)
    }
        .addModifiers(modifiers)
        .apply {
            when (function.name) {
                "getPropertyValue" -> addStatement(
                    "return %L[%N].orEmpty()",
                    JVM_CSS_PROPERTY_VALUES,
                    function.parameters[0].name,
                )
                "getPropertyPriority" -> addStatement(
                    "return %L[%N].orEmpty()",
                    JVM_CSS_PROPERTY_PRIORITIES,
                    function.parameters[0].name,
                )
                "setProperty" -> addJvmSetPropertyBody(function.parameters)
                "setPropertyValue" -> addStatement(
                    "setProperty(%N, %N)",
                    function.parameters[0].name,
                    function.parameters[1].name,
                )
                "setPropertyPriority" -> addJvmSetPropertyPriorityBody(function.parameters)
                "removeProperty" -> addJvmRemovePropertyBody(function.parameters[0])
                "item" -> addStatement(
                    "return %L.keys.elementAtOrNull(%N).orEmpty().%M()",
                    JVM_CSS_PROPERTY_VALUES,
                    function.parameters[0].name,
                    TO_JS_STRING,
                )
            }
        }
        .build()
}

/** Keeps value and priority stores consistent when setting or clearing a property. */
private fun FunSpec.Builder.addJvmSetPropertyBody(parameters: List<PortableParameter>) {
    val property = parameters[0].name
    val value = parameters[1].name
    val priority = parameters[2].name
    beginControlFlow("if (%N.isEmpty())", value)
        .addStatement("%L.remove(%N)", JVM_CSS_PROPERTY_VALUES, property)
        .addStatement("%L.remove(%N)", JVM_CSS_PROPERTY_PRIORITIES, property)
        .nextControlFlow("else")
        .addStatement("%L[%N] = %N", JVM_CSS_PROPERTY_VALUES, property, value)
        .beginControlFlow("if (%N.isEmpty())", priority)
        .addStatement("%L.remove(%N)", JVM_CSS_PROPERTY_PRIORITIES, property)
        .nextControlFlow("else")
        .addStatement("%L[%N] = %N", JVM_CSS_PROPERTY_PRIORITIES, property, priority)
        .endControlFlow()
        .endControlFlow()
}

/** Updates priority only for an existing property. */
private fun FunSpec.Builder.addJvmSetPropertyPriorityBody(parameters: List<PortableParameter>) {
    val property = parameters[0].name
    val priority = parameters[1].name
    beginControlFlow("if (%L.containsKey(%N))", JVM_CSS_PROPERTY_VALUES, property)
        .beginControlFlow("if (%N.isEmpty())", priority)
        .addStatement("%L.remove(%N)", JVM_CSS_PROPERTY_PRIORITIES, property)
        .nextControlFlow("else")
        .addStatement("%L[%N] = %N", JVM_CSS_PROPERTY_PRIORITIES, property, priority)
        .endControlFlow()
        .endControlFlow()
}

/** Clears both stores and returns the previous value. */
private fun FunSpec.Builder.addJvmRemovePropertyBody(property: PortableParameter) {
    addStatement("val previous = %L.remove(%N).orEmpty()", JVM_CSS_PROPERTY_VALUES, property.name)
    addStatement("%L.remove(%N)", JVM_CSS_PROPERTY_PRIORITIES, property.name)
    addStatement("return previous")
}

/** String methods that need stateful bodies instead of generic JVM stubs. */
private val CSS_STYLE_FUNCTIONS = setOf(
    "getPropertyValue",
    "getPropertyPriority",
    "setProperty",
    "setPropertyValue",
    "setPropertyPriority",
    "removeProperty",
    "item",
)
