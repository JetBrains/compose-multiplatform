package org.jetbrains.kotlin.r4a

import com.intellij.psi.PsiElement
import com.sun.jndi.ldap.LdapPoolManager.trace
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices.COMPOSABLE_ANALYSIS
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinedArgument
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.upperIfFlexible

class ComposableAnnotationChecker(val mode: Mode = DEFAULT_MODE) : CallChecker, DeclarationChecker,
    AdditionalTypeChecker, AdditionalAnnotationChecker, StorageComponentContainerContributor {

    enum class Mode {
        /** @Composable annotations are enforced when inferred/specified **/
        CHECKED,
        /** @Composable annotations are explicitly required on all composables **/
        STRICT,
        /** @Composable annotations are explicitly required and are not subtypes of their unannotated types **/
        PEDANTIC
    }

    companion object {
        val COMPOSABLE_ANNOTATION_NAME = R4aUtils.r4aFqName("Composable")
        val CHILDREN_ANNOTATION_NAME = R4aUtils.r4aFqName("Children")

        val DEFAULT_MODE = Mode.STRICT
    }

    enum class Composability { NOT_COMPOSABLE, INFERRED, MARKED }

    fun hasComposableAnnotation(type: KotlinType): Boolean {
        if(type === TypeUtils.NO_EXPECTED_TYPE || type === TypeUtils.UNIT_EXPECTED_TYPE) return false
        return type.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
    }

    fun hasComposableAnnotation(trace: BindingTrace, resolvedCall: ResolvedCall<*>): Composability {
        if (resolvedCall is VariableAsFunctionResolvedCall) {
            return analyze(trace, resolvedCall.variableCall.candidateDescriptor)
        }
        if (resolvedCall is ResolvedCallImpl) {
            return analyze(trace, resolvedCall.candidateDescriptor)
        }
        throw Error("unexpected")
    }

    fun analyze(trace: BindingTrace, descriptor: DeclarationDescriptor): Composability {
        if (descriptor is FunctionDescriptor && descriptor.name == Name.identifier("compose") && descriptor.containingDeclaration is ClassDescriptor && ComponentMetadata.isR4AComponent(
                descriptor.containingDeclaration
            )
        ) return Composability.MARKED
        val psi = descriptor.findPsi() as? KtElement
        val type = when (descriptor) {
            is PropertyGetterDescriptor -> descriptor.returnType
            is VariableDescriptor -> descriptor.returnType
            else -> null
        }
        if (psi != null) return analyze(trace, psi, type)
        val hasAnnotation = when (descriptor) {
            is VariableDescriptor -> descriptor.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME) || descriptor.type.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
            is ConstructorDescriptor -> descriptor.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
            is JavaMethodDescriptor -> descriptor.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
            else -> descriptor.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
        }
        return if(hasAnnotation) Composability.MARKED else Composability.NOT_COMPOSABLE
    }

    /**
     * Analyze a KtElement
     *  - Determine if it is @Compoasble (eg. the element or inferred type has an @Composable annotation)
     *  - Update the binding context to cache analysis results
     *  - Report errors (eg. KTX tags occur in a non-composable, invocations of an @Composable, etc)
     *  - Return true if element is @Composable, else false
     */
    fun analyze(trace: BindingTrace, element: KtElement, type: KotlinType?): Composability {
        trace.bindingContext.get(COMPOSABLE_ANALYSIS, element)?.let { return it }

        if (element is KtClass) {
            var descriptor = trace.bindingContext.get(BindingContext.CLASS, element) ?: element.getResolutionFacade().resolveToDescriptor(element, BodyResolveMode.FULL) as ClassDescriptor
            val annotationEntry = element.annotationEntries.singleOrNull {
                trace.bindingContext.get(BindingContext.ANNOTATION, it)?.fqName == COMPOSABLE_ANNOTATION_NAME
            }
            if (annotationEntry != null && !ComponentMetadata.isR4AComponent(descriptor)) {
                trace.report(Errors.WRONG_ANNOTATION_TARGET.on(annotationEntry, "class which does not extend com.google.r4a.Component"))
            }
            if(ComponentMetadata.isR4AComponent(descriptor)) Composability.MARKED else Composability.NOT_COMPOSABLE
        }
        if(element is KtProperty) {
            var descriptor = trace.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element) ?: element.getResolutionFacade().resolveToDescriptor(element, BodyResolveMode.FULL)
            val type = when(descriptor) {
                is LocalVariableDescriptor -> descriptor.type
                is PropertyDescriptor -> descriptor.type
                else -> throw Error("Unknown type: $descriptor")
            }
        }
        //if (candidateDescriptor.type.arguments.size != 1 || !candidateDescriptor.type.arguments[0].type.isUnit()) return false
        if (type != null && type !== TypeUtils.NO_EXPECTED_TYPE && type.annotations.findAnnotation(ComposableAnnotationChecker.COMPOSABLE_ANNOTATION_NAME) != null) return Composability.MARKED
        val parent = element.parent
        val annotations =
            if (element is KtNamedFunction) element.annotationEntries
            else if (parent is KtAnnotatedExpression) parent.annotationEntries
            else if (element is KtProperty) element.annotationEntries
            else emptyList()

        for (entry in annotations) {
            val descriptor = trace.bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
            if (descriptor.fqName == COMPOSABLE_ANNOTATION_NAME) {
                trace.record(COMPOSABLE_ANALYSIS, element, Composability.MARKED)
                return Composability.MARKED
            }
            if (descriptor.fqName == CHILDREN_ANNOTATION_NAME) {
                val composableValueArgument = descriptor.argumentValue("composable")?.value
                if(composableValueArgument == null || composableValueArgument == true) {
                    trace.record(COMPOSABLE_ANALYSIS, element, Composability.MARKED)
                    return Composability.MARKED
                }
            }
        }

        var localKtx = false
        var isInlineLambda = false
        element.accept(object : KtTreeVisitorVoid() {
            override fun visitKtxElement(element: KtxElement) {
                localKtx = true
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                if (dcl == element) {
                    super.visitDeclaration(dcl)
                }
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                val isInlineable = isInlinedArgument(lambdaExpression.functionLiteral, trace.bindingContext, true)
                if(isInlineable && lambdaExpression == element) isInlineLambda = true
                if(isInlineable || lambdaExpression == element) super.visitLambdaExpression(lambdaExpression)
            }
        }, null)
        if (localKtx && !isInlineLambda && (mode == Mode.STRICT || mode == Mode.PEDANTIC)) {
            val reportElement = when (element) {
                is KtNamedFunction -> element.nameIdentifier ?: element
                else -> element
            }
            trace.reportFromPlugin(R4AErrors.KTX_IN_NON_COMPOSABLE.on(reportElement), R4ADefaultErrorMessages)
        }
        val composability = if(localKtx) Composability.INFERRED else Composability.NOT_COMPOSABLE
        trace.record(COMPOSABLE_ANALYSIS, element, composability)
        return composability
    }

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return
        container.useInstance(this)
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        analyze(context.trace, descriptor)
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {

        val composability = hasComposableAnnotation(context.trace, resolvedCall)
        if (reportOn.parent is KtxElement) {
            if (composability == Composability.NOT_COMPOSABLE && resolvedCall is ResolvedCallImpl) {
                val callee = resolvedCall.candidateDescriptor
                if (callee is SimpleFunctionDescriptor && callee.annotations.hasAnnotation(R4aUtils.r4aFqName("Children"))) {
                    // Class components can have a setter function for setting children, which is resolved on the tag.
                    // This children setter function is not a SFC, so it shouldn't be reported.
                    return
                }
                if (mode == Mode.PEDANTIC && (callee is LocalVariableDescriptor || callee is PropertyDescriptor)) {
                    context.trace.reportFromPlugin(
                        R4AErrors.NON_COMPOSABLE_INVOCATION.on(
                            reportOn as KtElement,
                            "Lambda variable",
                            resolvedCall.candidateDescriptor.name.asString()
                        ), R4ADefaultErrorMessages
                    )
                } else if (callee is SimpleFunctionDescriptor) {
                    context.trace.reportFromPlugin(
                        R4AErrors.NON_COMPOSABLE_INVOCATION.on(
                            reportOn as KtElement,
                            "function",
                            resolvedCall.candidateDescriptor.name.asString()
                        ), R4ADefaultErrorMessages
                    )
                }
            }
            return
        } else if (context.resolutionContext is CallResolutionContext && resolvedCall is ResolvedCallImpl && (context.resolutionContext as CallResolutionContext).call.callElement is KtCallExpression) {
            if (composability != Composability.NOT_COMPOSABLE) {
                context.trace.reportFromPlugin(
                    R4AErrors.SVC_INVOCATION.on(
                        reportOn as KtElement,
                        resolvedCall.candidateDescriptor.name.asString()
                    ), R4ADefaultErrorMessages
                )
            }
        }
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        if(mode != Mode.PEDANTIC) return
        if(expression is KtLambdaExpression) {
            val expectedType = c.expectedType
            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            val expectedComposable = expectedType.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
            val composability= analyze(c.trace, expression, c.expectedType)
            if ((expectedComposable && composability == Composability.NOT_COMPOSABLE) || (!expectedComposable && composability == Composability.MARKED)) {
                val isInlineable = isInlinedArgument(expression.functionLiteral, c.trace.bindingContext, true)
                if (isInlineable) return;

                val reportOn = if (expression.parent is KtAnnotatedExpression) expression.parent as KtExpression else expression
                c.trace.report(Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(reportOn, expectedType, expressionTypeWithSmartCast))
            }
            return;
        } else {
            val expectedType = c.expectedType
            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            if (expectedType === TypeUtils.UNIT_EXPECTED_TYPE) return
            if (expectedType.builtIns.anyType.equals(expectedType.lowerIfFlexible()) && expectedType.builtIns.nullableAnyType.equals(expectedType.upperIfFlexible())) return
            val expectedComposable = expectedType.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
            val anyType = expectedType.builtIns.nullableAnyType
            val isComposable = expressionType.annotations.hasAnnotation(COMPOSABLE_ANNOTATION_NAME)
            if (expectedComposable != isComposable) {
                val reportOn = if (expression.parent is KtAnnotatedExpression) expression.parent as KtExpression else expression
                c.trace.report(Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(reportOn, expectedType, expressionTypeWithSmartCast))
            }
            return;
        }
    }


    override fun checkEntries(entries: List<KtAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace) {
        val entry = entries.singleOrNull {
            trace.bindingContext.get(BindingContext.ANNOTATION, it)?.fqName == COMPOSABLE_ANNOTATION_NAME
        }
        if((entry?.parent as? KtAnnotatedExpression)?.baseExpression is KtObjectLiteralExpression) {
            trace.report(Errors.WRONG_ANNOTATION_TARGET.on(entry, "class which does not extend com.google.r4a.Component"))
        }
    }
}
