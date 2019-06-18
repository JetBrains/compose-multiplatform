package androidx.compose.plugins.idea.quickfix

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import androidx.compose.plugins.kotlin.ComposeUtils
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

data class ImportVariant(
    val descriptor: DeclarationDescriptor,
    val hint: String,
    val fqName: FqName
) {
    companion object {
        private val RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
            parameterNamesInFunctionalTypes = false
        }

        fun forAttribute(descriptor: DeclarationDescriptor): ImportVariant {
            val fqName = descriptor.fqNameSafe
            val type = when (descriptor) {
                is FunctionDescriptor -> descriptor.valueParameters.first().type
                else -> null
            }
            val attrName = when (descriptor) {
                is FunctionDescriptor -> ComposeUtils.propertyNameFromSetterMethod(
                    descriptor.name.asString()
                )
                else -> descriptor.name.asString()
            }
            val hint = "$attrName=(${type?.let { RENDERER.renderType(it) }}) (from ${
                descriptor.containingDeclaration?.fqNameSafe
            })"

            return ImportVariant(descriptor, hint, fqName)
        }

        fun forComponent(descriptor: DeclarationDescriptor): ImportVariant {
            return ImportVariant(
                descriptor,
                descriptor.fqNameSafe.asString(),
                descriptor.fqNameSafe
            )
        }
    }
}