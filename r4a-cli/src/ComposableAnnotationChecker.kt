package org.jetbrains.kotlin.r4a

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class ComposableAnnotationChecker : CallChecker, DeclarationChecker, StorageComponentContainerContributor {

    override fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor) {
        if (platform != JvmPlatform) return
        container.useInstance(ComposableAnnotationChecker())
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if(descriptor !is FunctionDescriptor) return
        if(descriptor.name == Name.identifier("compose") && descriptor.containingDeclaration is ClassDescriptor && ComponentMetadata.isR4AComponent(descriptor.containingDeclaration)) return
        if(descriptor.annotations.findAnnotation(FqName(R4aUtils.generateR4APackageName()+".Composable")) != null) return

        var containsKtxTag = false;
        declaration.accept(object : KtTreeVisitorVoid() {
            override fun visitKtxElement(element: KtxElement) {
                containsKtxTag = true
            }
        }, null)
        if(containsKtxTag) {
            val reportElement = (declaration as? KtNamedFunction)?.nameIdentifier ?: declaration
            context.trace.reportFromPlugin(R4AErrors.KTX_IN_NON_COMPOSABLE.on(reportElement), R4ADefaultErrorMessages)
        }
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val annotations = (resolvedCall as? VariableAsFunctionResolvedCall)?.variableCall?.candidateDescriptor?.annotations ?: return
        val isComponent = annotations.findAnnotation(FqName(R4aUtils.generateR4APackageName()+".Composable")) != null
        if(!isComponent) return
        else context.trace.reportFromPlugin(R4AErrors.SVC_INVOCATION.on(reportOn as KtElement, resolvedCall.candidateDescriptor.name.identifier), R4ADefaultErrorMessages)
    }
}
