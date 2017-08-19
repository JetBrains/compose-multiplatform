package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtPureClassOrObject

class MyCodegen(
        aClass: KtPureClassOrObject,
        context: ClassContext,
        v: ClassBuilder,
        state: GenerationState,
        parentCodegen: MemberCodegen<*>?,
        isLocal: Boolean
) : ImplementationBodyCodegen(aClass, context, v, state, parentCodegen, isLocal) {

    public override fun generateKotlinMetadataAnnotation() {
        super.generateKotlinMetadataAnnotation()
    }
}
