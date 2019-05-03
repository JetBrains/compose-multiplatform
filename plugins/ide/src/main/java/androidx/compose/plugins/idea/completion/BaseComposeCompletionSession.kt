package androidx.compose.plugins.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.idea.completion.CompletionSession
import org.jetbrains.kotlin.idea.completion.CompletionSessionConfiguration
import org.jetbrains.kotlin.idea.completion.ToFromOriginalFileMapper
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.isExcludedFromAutoImport
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.inspections.collections.isFunctionOfAnyKind
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import androidx.compose.plugins.kotlin.ComposeUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType

abstract class BaseComposeCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    toFromOriginalFileMapper: ToFromOriginalFileMapper,
    resultSet: CompletionResultSet
) : CompletionSession(configuration, parameters, toFromOriginalFileMapper, resultSet) {
    override val descriptorKindFilter: DescriptorKindFilter?
        get() = TODO("not implemented")
    override val expectedInfos: Collection<ExpectedInfo>
        get() = emptyList()

    protected val cursorElement = expression ?: error("expression cannot be null")
    protected val module = resolutionFacade.moduleDescriptor

    protected val scope = cursorElement.getResolutionScope(bindingContext, resolutionFacade)

    protected val composeComponentDescriptor =
        module.findClassAcrossModuleDependencies(
            ClassId.topLevel(ComposeUtils.composeFqName("Component"))
        )
    protected val androidViewDescriptor =
        module.findClassAcrossModuleDependencies(
            ClassId.topLevel(FqName("android.view.View"))
        )
    protected val androidViewGroupDescriptor =
        module.findClassAcrossModuleDependencies(
            ClassId.topLevel(FqName("android.view.ViewGroup"))
        )

    protected val composeChildrenAnnotationFqName = ComposeUtils.composeFqName("Children")
    protected val composeComposableAnnotationFqName = ComposeUtils.composeFqName("Composable")

    protected val psiFacade = JavaPsiFacade.getInstance(project)

    protected val androidViewPsiClass = psiFacade.findClass("android.view.View", searchScope)
    protected val androidViewGroupPsiClass =
        psiFacade.findClass("android.view.ViewGroup", searchScope)

    protected fun DeclarationDescriptor.isVisibleDescriptor(): Boolean {
        if (this is TypeParameterDescriptor && !isTypeParameterVisible(this))
            return false

        if (this is DeclarationDescriptorWithVisibility) {
            return isVisible(
                cursorElement,
                callTypeAndReceiver.receiver as? KtExpression,
                bindingContext,
                resolutionFacade
            )
        }

        if (isExcludedFromAutoImport(file.project, file)) return false

        return true
    }

    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
        val owner = typeParameter.containingDeclaration
        var parent: DeclarationDescriptor? = inDescriptor
        while (parent != null) {
            if (parent == owner) return true
            if (parent is ClassDescriptor && !parent.isInner) return false
            parent = parent.containingDeclaration
        }
        return true
    }

    protected fun Annotated.hasChildrenAnnotation() =
        annotations.hasAnnotation(composeChildrenAnnotationFqName)
    protected fun Annotated.hasComposableAnnotation() =
        annotations.hasAnnotation(composeComposableAnnotationFqName)

    protected fun DeclarationDescriptor.allowsChildren(): Boolean {
        return when (this) {
            is ClassDescriptor -> when {
                androidViewGroupDescriptor != null && isSubclassOf(androidViewGroupDescriptor) ->
                    true
                unsubstitutedMemberScope.getContributedDescriptors().any {
                    it.hasChildrenAnnotation()
                } -> true
                unsubstitutedPrimaryConstructor?.valueParameters?.any {
                    it.hasChildrenAnnotation()
                } ?: false -> true
                else -> false
            }
            is FunctionDescriptor -> when {
                valueParameters.any { it.hasChildrenAnnotation() } -> true
                else -> false
            }
            is VariableDescriptor -> when {
                type.isFunctionOfAnyKind() && type.arguments.any {
                    it.type.hasChildrenAnnotation()
                } -> true
                else -> false
            }
            else -> false
        }
    }

    protected fun PsiClass.allowsChildren(): Boolean {
        // TODO(lmr): if it's a subclass of ViewGroup is a reasonable first order approximation of whether or not it
        // accepts children or not, but it is not a perfect test by any means. Many "leaf" views subclass ViewGroups
        // and handle laying out their own children. Perhaps we should have a manually curated list of these?
        return isSubclassOf(androidViewGroupPsiClass)
    }

    protected fun PsiClass.isSubclassOf(other: PsiClass?): Boolean {
        if (other == null) return false
        var cls: PsiClass? = this
        while (cls != null) {
            if (cls == other) return true
            cls = cls.superClass
        }
        return false
    }

    protected fun KotlinType.functionParameterNames(): List<String> =
        if (isFunctionOfAnyKind()) arguments.mapNotNull {
            it.type.extractParameterNameFromFunctionTypeArgument()?.asString()
        } else emptyList()
}