package org.jetbrains.kotlin.r4a.idea.completion

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.types.KotlinType

data class AttributeInfo(
    val name: String,
    val type: KotlinType
) {
    var isImmediate: Boolean = false
    var isRequired: Boolean = false
    var isExtension: Boolean = false
    var isChildren: Boolean = false
    var isPivotal: Boolean = false
    var isImported: Boolean = false
    var descriptor: DeclarationDescriptor? = null
    var contributingDescriptor: ClassifierDescriptor? = null
    var extensionType: KotlinType? = null
}