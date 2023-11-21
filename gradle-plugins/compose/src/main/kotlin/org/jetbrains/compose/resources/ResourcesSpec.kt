package org.jetbrains.compose.resources

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.withIndent
import java.nio.file.Path

internal enum class ResourceType(val typeName: String) {
    IMAGE("images"),
    STRING("strings"),
    FONT("fonts");

    companion object {
        fun fromString(str: String) = when (str) {
            "images" -> ResourceType.IMAGE
            "strings" -> ResourceType.STRING
            "fonts" -> ResourceType.FONT
            else -> error("Unknown resource type: $str")
        }
    }
}

internal data class ResourceItem(
    val type: ResourceType,
    val qualifiers: Set<String>,
    val name: String,
    val path: Path
)

private fun ResourceItem.getClassName(): ClassName = when (type) {
    ResourceType.IMAGE -> ClassName("org.jetbrains.compose.resources", "ImageResource")
    ResourceType.STRING -> ClassName("org.jetbrains.compose.resources", "StringResource")
    ResourceType.FONT -> ClassName("org.jetbrains.compose.resources", "FontResource")
}

internal fun getResFileSpec(
    //type -> id -> items
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>,
    packageName: String
): FileSpec = FileSpec.builder(packageName, "Res").apply {
    addType(TypeSpec.objectBuilder("Res").apply {
        addModifiers(KModifier.INTERNAL)
        val types = resources.map { (type, idToResources) ->
            getResourceTypeObject(type, idToResources)
        }
        addTypes(types)
    }.build())
}.build()

private fun getResourceTypeObject(type: ResourceType, nameToResources: Map<String, List<ResourceItem>>) =
    TypeSpec.objectBuilder(type.typeName).apply {
        nameToResources.forEach { (name, items) ->
            addResourceProperty(name, items)
        }
    }.build()

private fun TypeSpec.Builder.addResourceProperty(name: String, items: List<ResourceItem>) {
    val resourceItemClass = ClassName("org.jetbrains.compose.resources", "ResourceItem")

    val first = items.first()
    val propertyClassName = first.getClassName()
    val resourceId = first.let { "${it.type}:${it.name}" }

    val initializer = CodeBlock.builder()
        .add("%T(\n", propertyClassName).withIndent {
            add("\"$resourceId\",\n")
            if (first.type == ResourceType.STRING) {
                add("\"${first.name}\",\n")
            }
            add("setOf(\n").withIndent {
                items.forEach { item ->
                    val qualifiers = item.qualifiers.joinToString { "\"$it\"" }
                    add("%T(setOf($qualifiers), \"${item.path}\"),\n", resourceItemClass)
                }
            }
            add(")\n")
        }
        .add(")")
        .build()

    addProperty(
        PropertySpec.builder(name, propertyClassName)
            .initializer(initializer)
            .build()
    )
}