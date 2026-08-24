// Emits stateful JVM stubs for option dictionaries.
package prototype.dom.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
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
