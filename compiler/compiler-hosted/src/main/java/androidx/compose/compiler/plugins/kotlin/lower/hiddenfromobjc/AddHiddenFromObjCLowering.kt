/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc

import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import androidx.compose.compiler.plugins.kotlin.lower.ComposableSymbolRemapper
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.konan.isNative

/**
 *  AddHiddenFromObjCLowering looks for functions and properties with @Composable types and
 *  adds the `kotlin.native.HiddenFromObjC` annotation to them.
 *  @see https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native/-hidden-from-obj-c/
 */
class AddHiddenFromObjCLowering(
    pluginContext: IrPluginContext,
    symbolRemapper: ComposableSymbolRemapper,
    metrics: ModuleMetrics,
    private val hideFromObjCDeclarationsSet: HideFromObjCDeclarationsSet
) : AbstractComposeLowering(pluginContext, symbolRemapper, metrics) {

    private val hiddenFromObjCAnnotation: IrClassSymbol by lazy {
        getTopLevelClass(ClassId.fromString("kotlin/native/HiddenFromObjC"))
    }

    override fun lower(module: IrModuleFragment) {
        require(context.platform.isNative()) {
            "AddHiddenFromObjCLowering is expected to run only for k/native. " +
                "The platform - ${context.platform}"
        }
        module.transformChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val f = super.visitFunction(declaration) as IrFunction
        if (f.isLocal || f.visibility != DescriptorVisibilities.PUBLIC) return f

        val shouldAdd = f.hasComposableAnnotation() ||
            f.typeParameters.any { it.defaultType.hasComposable() } ||
            f.valueParameters.any { it.type.hasComposable() } ||
            f.returnType.hasComposable()

        if (shouldAdd) {
            f.addHiddenFromObjCAnnotation()
            hideFromObjCDeclarationsSet.addToHide(f)
        }

        return f
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val p = super.visitProperty(declaration) as IrProperty
        if (p.isLocal || p.visibility != DescriptorVisibilities.PUBLIC) return p

        val shouldAdd = p.getter?.hasComposableAnnotation() == true ||
            p.getter?.returnType?.hasComposable() == true ||
            p.backingField?.type?.hasComposable() == true

        if (shouldAdd) {
            p.addHiddenFromObjCAnnotation()
            hideFromObjCDeclarationsSet.addToHide(p)
        }

        return p
    }

    private fun IrMutableAnnotationContainer.addHiddenFromObjCAnnotation() {
        annotations = annotations + IrConstructorCallImpl.fromSymbolOwner(
            type = hiddenFromObjCAnnotation.defaultType,
            constructorSymbol = hiddenFromObjCAnnotation.constructors.first()
        )
    }

    private fun IrType.hasComposable(): Boolean {
        if (hasComposableAnnotation()) {
            return true
        }

        return when (this) {
            is IrSimpleType -> arguments.any { (it as? IrType)?.hasComposable() == true }
            else -> false
        }
    }
}
