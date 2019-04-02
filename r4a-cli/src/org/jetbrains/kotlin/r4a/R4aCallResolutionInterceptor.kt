package org.jetbrains.kotlin.r4a

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.extensions.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType.annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.uast.values.UNullConstant.source

class R4aCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
    override fun interceptCandidates(candidates: Collection<NewResolutionOldInference.MyCandidate>,
                                     context: BasicCallResolutionContext,
                                     candidateResolver: CandidateResolver,
                                     name: Name,
                                     kind: NewResolutionOldInference.ResolutionKind,
                                     tracing: TracingStrategy
    ): Collection<NewResolutionOldInference.MyCandidate> {
      /*  if(name == Name.identifier("foo")) {

            // TODO: Remove this.
       //     if(true) return candidates

            val originalCandidate = (candidates as ArrayList).get(0)
            val originalResolvedCall = originalCandidate.resolvedCall

            val candidateCall = ResolvedCallImpl(
                originalResolvedCall.call, originalResolvedCall.candidateDescriptor,
                originalResolvedCall.dispatchReceiver, originalResolvedCall.extensionReceiver,
                originalResolvedCall.explicitReceiverKind, null, originalResolvedCall.trace, tracing,
                originalResolvedCall.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )
            candidateCall.addStatus(originalResolvedCall.status)

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                candidateCall, context, candidateTrace, tracing, context.call,
                CandidateResolveMode.EXIT_ON_FIRST_ERROR
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, context.checkArguments) // todo


            val diagnostics = createDiagnosticsForCandidate(towerCandidate, candidateCall)
            return NewResolutionOldInference.MyCandidate(diagnostics, candidateCall) {
                candidateCall.performRemainingTasks()
                createDiagnosticsForCandidate(towerCandidate, candidateCall)
            }

            val candidate = CandidateFactoryImpl(name, context, tracing).createCandidate()
            return listOf(candidate)
        }
        else */return candidates
    }

    override fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {

        /*
        val candidate = candidates.iterator().next().let { if(it is ConstructorDescriptor) it.containingDeclaration else it } as DeclarationDescriptor
        val newCandidates = mutableListOf<FunctionDescriptor>();
        newCandidates.addAll(candidates)

        val element = resolutionContext.call.callElement

        val ctx = ExpressionTypingContext.newContext(
            resolutionContext.trace,
            resolutionContext.scope,
            resolutionContext.dataFlowInfo,
            resolutionContext.expectedType,
            resolutionContext.languageVersionSettings,
            resolutionContext.dataFlowValueFactory)

            */
/*
        val ktxCallResolver = KtxCallResolver((scopeTower as NewResolutionOldInference.ImplicitScopeTowerImpl).callResolver, facade, element.project, ComposableAnnotationChecker.get(element.project))

        ktxCallResolver.resolveComposer(element, ctx)

        val temporaryForKtxCall = TemporaryTraceAndCache.create(context, "trace to resolve ktx call", element)

        val resolvedKtxElementCall = ktxCallResolver.resolve(
            element,
            ctx.replaceTraceAndCache(temporaryForKtxCall)
        )

        temporaryForKtxCall.commit()
*/

/*
        if(name.identifier == "TextView") {
            val descriptor = ComposableInvocationDescriptor(
                candidate.containingDeclaration!!,
                null,
                Annotations.EMPTY,
                name,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
            )

            val valueArgs = mutableListOf<ValueParameterDescriptor>()
            valueArgs.add(ValueParameterDescriptorImpl(descriptor,null, 0,
                                                  Annotations.EMPTY,
                                                  Name.identifier("text"),
                                                  scopeTower.module.builtIns.stringType, false,
                                                  false,
                                                  false, null,
                                                  SourceElement.NO_SOURCE
            ))

            descriptor.initialize(
                null,
                null,
                mutableListOf(),
                valueArgs,
                scopeTower.module.builtIns.unitType,
                Modality.FINAL,
                Visibilities.DEFAULT_VISIBILITY
            )
            newCandidates.add(descriptor)
        }
        */

        return candidates
      //  return newCandidates
    }
}

class ComposableInvocationDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement) : SimpleFunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, source) {

}

