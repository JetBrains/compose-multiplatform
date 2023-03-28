/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.ComposerParamTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ModuleLoweringPass
import androidx.compose.compiler.plugins.kotlin.lower.changedParamCount
import androidx.compose.compiler.plugins.kotlin.lower.function
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.Variance

/**
 * Replaces all decoys references to their implementations created in [CreateDecoysTransformer].
 */
class SubstituteDecoyCallsTransformer(
    pluginContext: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    signatureBuilder: IdSignatureSerializer,
    metrics: ModuleMetrics,
) : AbstractDecoysLowering(
    pluginContext = pluginContext,
    symbolRemapper = symbolRemapper,
    metrics = metrics,
    signatureBuilder = signatureBuilder
), ModuleLoweringPass {
    private val decoysTransformer = CreateDecoysTransformer(
        pluginContext, symbolRemapper, signatureBuilder, metrics
    )
    private val lazyDeclarationsCache = mutableMapOf<IrFunctionSymbol, IrFunction>()

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid()

        module.patchDeclarationParents()
    }

    /*
    * Some properties defined in classes get initialized by using values provided in
    * constructors:
    *   class TakesComposable(name: String, age: Int, val c: @Composable () -> Unit) {
    *        var nameAndAge: String = "$name,$age"
    *   }
    *
    * Since [ComposerTypeRemapper].remapType() skips decoys (including decoy constructors),
    * the value getters (e.g `name` or `age` above) keep references to old value parameter symbols
    * from decoy constructors.
    * Therefore, getters for values from constructors need to be substituted by getters for values
    * from constructors with DecoyImplementation annotation.
    */
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val originalGetValue = super.visitGetValue(expression)

        val valueParameter = expression.symbol.owner as? IrValueParameter
            ?: return originalGetValue

        val constructorParent = valueParameter.parent as? IrConstructor
            ?: return originalGetValue

        if (!constructorParent.isDecoy()) {
            return originalGetValue
        }

        val targetConstructor = constructorParent.getComposableForDecoy().owner as IrConstructor
        val targetValueParameter = targetConstructor.valueParameters[valueParameter.index]

        return irGet(targetValueParameter)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        // Apart from function / constructor calls, decoys can surface in overridden symbols,
        // so we need to replace them as well.
        // They are replaced only for decoy implementations however, as decoys should match
        // original descriptors.

        if (declaration.isDecoy()) {
            return super.visitSimpleFunction(declaration)
        }

        val newOverriddenSymbols = declaration.overriddenSymbols.map {
            // It can be an overridden symbol from another module, so access it via `decoyOwner`
            val maybeDecoy = it.decoyOwner
            if (maybeDecoy.isDecoy()) {
                maybeDecoy.getComposableForDecoy() as IrSimpleFunctionSymbol
            } else {
                it
            }
        }

        declaration.overriddenSymbols = newOverriddenSymbols
        return super.visitSimpleFunction(declaration)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val callee = expression.symbol.decoyOwner
        if (!callee.isDecoy()) {
            return super.visitConstructorCall(expression)
        }

        val actualConstructor = callee.getComposableForDecoy().owner as IrConstructor

        val updatedCall = IrConstructorCallImpl(
            symbol = actualConstructor.symbol,
            origin = expression.origin,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualConstructor),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            constructorTypeArgumentsCount = expression.constructorTypeArgumentsCount
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualConstructor)
        }

        return super.visitConstructorCall(updatedCall)
    }

    override fun visitDelegatingConstructorCall(
        expression: IrDelegatingConstructorCall
    ): IrExpression {
        val callee = expression.symbol.decoyOwner
        if (!callee.isDecoy()) {
            return super.visitDelegatingConstructorCall(expression)
        }

        val actualConstructor = callee.getComposableForDecoy().owner as IrConstructor

        val updatedCall = IrDelegatingConstructorCallImpl(
            symbol = actualConstructor.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualConstructor),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualConstructor)
        }

        return super.visitDelegatingConstructorCall(updatedCall)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.decoyOwner
        if (!callee.isDecoy()) {
            return super.visitCall(expression)
        }

        val actualFunction = callee.getComposableForDecoy().owner as IrSimpleFunction

        val updatedCall = IrCallImpl(
            symbol = actualFunction.symbol,
            origin = expression.origin,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualFunction),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            superQualifierSymbol = expression.superQualifierSymbol
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualFunction)
        }
        return super.visitCall(updatedCall)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val callee = expression.symbol.decoyOwner
        if (!callee.isDecoy()) {
            return super.visitFunctionReference(expression)
        }

        val actualFunction = callee.getComposableForDecoy().owner as IrSimpleFunction

        val updatedReference = IrFunctionReferenceImpl(
            symbol = actualFunction.symbol,
            origin = expression.origin,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualFunction),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            reflectionTarget = expression.reflectionTarget
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualFunction)
        }
        return super.visitFunctionReference(updatedReference)
    }

    private val addComposerParameterInplace = object : IrElementTransformerVoid() {
        private val composerParamTransformer = ComposerParamTransformer(
            context, symbolRemapper, true, metrics
        )

        private fun IrType.isComposable(): Boolean {
            return annotations.hasAnnotation(ComposeFqNames.Composable)
        }

        private val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()

        private fun IrConstructorCall.isComposableAnnotation() =
            this.symbol.owner.parent.fqNameForIrSerialization == ComposeFqNames.Composable

        val typeRemapper = object : TypeRemapper {
            override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}
            override fun leaveScope() {}

            private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
                if (typeArgument is IrTypeProjection)
                    makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
                else
                    typeArgument

            override fun remapType(type: IrType): IrType {
                if (type !is IrSimpleType) return type
                if (!type.isFunction()) return type
                if (!type.isComposable()) return type

                val oldIrArguments = type.arguments
                val realParams = oldIrArguments.size - 1
                var extraArgs = listOf(
                    // composer param
                    makeTypeProjection(
                        composerType,
                        Variance.INVARIANT
                    )
                )
                val changedParams = changedParamCount(realParams, 1)
                extraArgs = extraArgs + (0 until changedParams).map {
                    makeTypeProjection(context.irBuiltIns.intType, Variance.INVARIANT)
                }
                val newIrArguments =
                    oldIrArguments.subList(0, oldIrArguments.size - 1) +
                        extraArgs +
                        oldIrArguments.last()

                val newArgSize = oldIrArguments.size - 1 + extraArgs.size
                val functionCls = context.function(newArgSize)

                return IrSimpleTypeImpl(
                    null,
                    functionCls,
                    type.nullability,
                    newIrArguments.map { remapTypeArgument(it) },
                    type.annotations.filter { !it.isComposableAnnotation() },
                    null
                )
            }
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
            return composerParamTransformer.visitSimpleFunction(declaration)
        }
    }

    /**
     * Since kotlin 1.7.20, k/js started to use LazyIr (stubs).
     * Decoys logic used to rely on full deserialized IR of the module dependencies.
     *
     * This extension adjusts LazyIr for decoys needs:
     * generates decoy implementation declarations and adds composer parameter to ensure matching
     * signatures.
     *
     * To consider: LazyIr allows to get rid of decoys logic. But it's going to be a breaking
     * change for the compose users, since all published klibs will need to be recompiled in order
     * to be compatible with new-no-decoys compiler plugin. So we'll need to choose
     * a good moment for such a breaking change.
     */
    private val IrFunctionSymbol.decoyOwner: IrFunction
        get() = if (owner is IrLazyFunctionBase && !owner.isDecoy()) {
            lazyDeclarationsCache.getOrPut(this) {
                val declaration = owner
                if (declaration.shouldBeRemapped()) {
                    when (declaration) {
                        is IrSimpleFunction -> decoysTransformer.visitSimpleFunction(declaration)
                        is IrConstructor -> decoysTransformer.visitConstructor(declaration)
                        else -> decoysTransformer.visitFunction(declaration)
                    }.also {
                        decoysTransformer.updateParents()
                        (it as IrFunction).getComposableForDecoy().also {
                            it.owner.remapTypes(addComposerParameterInplace.typeRemapper)
                        }
                        owner.parent.transformChildrenVoid(addComposerParameterInplace)
                    } as IrFunction
                } else owner
            }
        } else owner
}