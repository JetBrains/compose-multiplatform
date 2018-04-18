package org.jetbrains.kotlin.r4a

import com.intellij.debugger.ui.impl.watch.LocalVariableDescriptorImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.js.resolve.JsPlatform.builtIns
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*

class R4ASyntheticIrExtension : SyntheticIrExtension {

    fun DeclarationDescriptor.getModule(): ModuleDescriptor {
        return if(this is ModuleDescriptor) this else this.containingDeclaration!!.getModule()
    }

    override fun visitKtxElement(statementGenerator: StatementGenerator, element: KtxElement): IrStatement {
        val body = element.body?.map { it.accept(statementGenerator, null) }
        val attributes = element.attributes!!.map {
            IrKtxAttribute(it, it.value?.accept(statementGenerator, null) as IrExpression?)
        }
        return IrKtxTag(element, body, attributes)
    }

    /*

    private fun writeCreatorClass(codegen: ImplementationBodyCodegen, classDescriptor: GeneratedViewClassDescriptor) {
        val asmType = codegen.typeMapper.mapType(classDescriptor.defaultType)
        val classBuilderForCreator = codegen.state.factory.newVisitor(JvmDeclarationOrigin.NO_ORIGIN, Type.getObjectType(asmType.internalName), codegen.myClass.containingKtFile)
        val classContextForCreator = ClassContext(codegen.typeMapper, classDescriptor, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
        val codegenForCreator = MyCodegen(codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

        val rerenderableClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/Rerenderable"
        classBuilderForCreator.defineClass(null, Opcodes.V1_6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, asmType.internalName, null, "android/widget/LinearLayout", arrayOf(rerenderableClassName))


        codegenForCreator.generateKotlinMetadataAnnotation()

        classBuilderForCreator.done()

    }

    private fun writeRerenderHelperClass(codegen: ImplementationBodyCodegen, classDescriptor: GeneratedRerenderHelperClassDescriptor) {
        val asmType = codegen.typeMapper.mapType(classDescriptor.defaultType)
        val classBuilderForCreator = codegen.state.factory.newVisitor(JvmDeclarationOrigin.NO_ORIGIN, Type.getObjectType(asmType.internalName), codegen.myClass.containingKtFile)
        val classContextForCreator = ClassContext(codegen.typeMapper, classDescriptor, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
        val codegenForCreator = MyCodegen(codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

        classBuilderForCreator.defineClass(null, Opcodes.V1_6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, asmType.internalName, null, "java/lang/Object", arrayOf("java/lang/Runnable"))

        val componentType = codegen.typeMapper.mapType(codegen.descriptor)
        val viewType = classDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters[0].type.asmType(codegen.typeMapper);
        classBuilderForCreator.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE or Opcodes.ACC_SYNTHETIC, "view", "L"+viewType.internalName+";", null, null)
        codegenForCreator.generateKotlinMetadataAnnotation()

        classBuilderForCreator.done()

    }
     */

    override fun interceptModuleFragment(context: GeneratorContext, ktFiles: Collection<KtFile>, irModuleFragment: IrModuleFragment) {

        val components = irModuleFragment.find { it is IrClass && ComponentMetadata.isR4HComponent(it.descriptor) }.map { it as IrClass }

        for(component in components) {

            generateSyntheticFieldsForComponent(context, component)
            generateSyntheticMethodsOnComponent(context, component)
            generateKtxBytecodeForComponent(context, component)

            val file = irModuleFragment.find { it is IrFile && it.find { it == component }.size > 0 }.single() as IrFile
            val wrapperViewIrClass = generateWrapperView(context, component)
            file.declarations.add(wrapperViewIrClass)

            generateComponentCompanionObject(context, component, wrapperViewIrClass)

        }
    }

    fun generateKtxBytecodeForComponent(context: GeneratorContext, component: IrClass) {
        val compose = component.find { it is IrSimpleFunction && it.name.toString() == "render" && it.descriptor.valueParameters.size == 0 }.single() as IrSimpleFunction

        // Insert some code at the beginning of compose, that initializes the markup builder
        val dslBuilderClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.R4aElementBuilderDSL")))!!
        val dslBuilderConstructor = context.symbolTable.referenceConstructor(dslBuilderClassDescriptor.unsubstitutedPrimaryConstructor!!)
        val dslBuilderIrConstructor = IrCallImpl(-1, -1, dslBuilderClassDescriptor.defaultType,
                                                 dslBuilderConstructor,
                                                 dslBuilderConstructor.descriptor, null)

        val setMarkupBuilderFunction = component.descriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("markupBuilder"), NoLookupLocation.FROM_BACKEND).single().setter!!
        val setMarkupBuilderFunctionSymbol = context.symbolTable.referenceSimpleFunction(setMarkupBuilderFunction)
        val setMarkupBuilderCall = IrSetterCallImpl(-1, -1,
                                                    setMarkupBuilderFunctionSymbol,
                                                    setMarkupBuilderFunction, 0)
        setMarkupBuilderCall.putValueArgument(0, dslBuilderIrConstructor)
        setMarkupBuilderCall.dispatchReceiver = IrGetValueImpl(
                -1, -1,
                component.thisReceiver!!.symbol
        )

        (compose.body as IrBlockBody).statements.add(0, setMarkupBuilderCall)


        // Transform the KTX tags within compose
        compose.body!!.accept(object : IrElementTransformer<Nothing?> {
            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
                val block = IrBlockImpl(-1, -1, context.moduleDescriptor.builtIns.unitType, object: IrStatementOriginImpl("KTX Tag"){}, transform(context, component, compose, expression as IrKtxTag))
                block.accept(this, data)
                return block;
            }
        }, null)
    }

    fun generateSyntheticFieldsForComponent(context: GeneratorContext, component: IrClass) {
        run {
            val property = context.symbolTable.declareField(-1, -1, IrDeclarationOrigin.DEFINED, ComponentMetadata.fromDescriptor(component.descriptor).elementsFieldDescriptor)
            component.declarations.add(property)
        }
    }

    fun generateSyntheticMethodsOnComponent(context: GeneratorContext, component: IrClass) {
        val metadata = ComponentMetadata.fromDescriptor(component.descriptor)
        val functionDescriptor = metadata.renderIntoViewGroupDescriptor
        val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
            .buildWithScope(context) { irFunction ->

                irFunction.createParameterDeclarations()

                val statements = mutableListOf<IrStatement>()


                val renderFunction = context.symbolTable.referenceFunction(component.descriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("render"), NoLookupLocation.FROM_BACKEND).single())
                val renderCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                renderFunction,
                                                renderFunction.descriptor
                                                , null)
                renderCall.dispatchReceiver = IrGetValueImpl(-1, -1, component.thisReceiver!!.symbol)
                statements.add(renderCall)


                val getMarkupBuilderFunction = component.descriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("markupBuilder"), NoLookupLocation.FROM_BACKEND).single().getter!!
                val getMarkupBuilderCall = IrGetterCallImpl(-1, -1,
                                                            context.symbolTable.referenceSimpleFunction(getMarkupBuilderFunction),
                                                            getMarkupBuilderFunction, 0)
                getMarkupBuilderCall.dispatchReceiver = IrGetValueImpl(-1, -1,component.thisReceiver!!.symbol)

                val dslBuilderClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.R4aElementBuilderDSL")))!!

                val renderResultsGetterDescriptor = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("renderResults"), NoLookupLocation.FROM_BACKEND).single().getter!!
                val renderResultsMethodCall = IrGetterCallImpl(-1, -1,
                                                            context.symbolTable.referenceSimpleFunction(renderResultsGetterDescriptor),
                                                               renderResultsGetterDescriptor, 0)
                renderResultsMethodCall.dispatchReceiver = getMarkupBuilderCall

                val r4aDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.R4a")))!!
                val staticRenderIntoDescriptor = r4aDescriptor.staticScope.getContributedFunctions(Name.identifier("renderIntoWrapperView"), NoLookupLocation.FROM_BACKEND).single()
                val staticRenderIntoCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                      context.symbolTable.referenceSimpleFunction(staticRenderIntoDescriptor),
                                                      staticRenderIntoDescriptor, null)
                staticRenderIntoCall.putValueArgument(0, IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(metadata.elementsFieldDescriptor), IrGetValueImpl(-1, -1, component.thisReceiver!!.symbol)))
                staticRenderIntoCall.putValueArgument(1, renderResultsMethodCall)
                staticRenderIntoCall.putValueArgument(2, IrGetValueImpl(-1, -1, irFunction.valueParameters[0].symbol))
                staticRenderIntoCall.putValueArgument(3, IrGetValueImpl(-1, -1, irFunction.valueParameters[1].symbol))
                staticRenderIntoCall.putValueArgument(4, IrGetValueImpl(-1, -1, irFunction.valueParameters[2].symbol))
                statements.add(staticRenderIntoCall)

                statements.add(IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(metadata.elementsFieldDescriptor), IrGetValueImpl(-1, -1, component.thisReceiver!!.symbol), renderResultsMethodCall))



                irFunction.body = IrBlockBodyImpl(-1, -1, statements)
            }
        component.declarations.add(irFunction)
    }

    fun generateComponentCompanionObject(context: GeneratorContext, component: IrClass, wrapperViewIrClass: IrClass) {
        val companion = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, component.descriptor.unsubstitutedMemberScope.getContributedClassifier(Name.identifier("Companion2"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor)
        component.declarations.add(companion)
        val createInstanceFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, companion.descriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("createInstance"), NoLookupLocation.FROM_BACKEND).single())
            .buildWithScope(context) { irFunction ->

                irFunction.createParameterDeclarations()

                val wrapperViewInstance = IrCallImpl(-1, -1, context.symbolTable.referenceConstructor(wrapperViewIrClass.descriptor.unsubstitutedPrimaryConstructor!!))
                wrapperViewInstance.putValueArgument(0, IrGetValueImpl(-1, -1, irFunction.valueParameters[0].symbol))
                irFunction.body = IrBlockBodyImpl(-1, -1, listOf(IrReturnImpl(-1, -1, irFunction.symbol, wrapperViewInstance)))
            }
        companion.declarations.add(createInstanceFunction)
    }

    fun generateWrapperView(context: GeneratorContext, component: IrClass): IrClass {
        val syntheticClassDescriptor = context.bindingContext.get(
                R4AWritableSlices.WRAPPER_VIEW,
                component.descriptor.source.getPsi() as KtClass
        )!!
        val wrapperViewIrClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor)
        wrapperViewIrClass.createParameterDeclarations()

        run {
            val property = context.symbolTable.declareField(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("componentInstance"), NoLookupLocation.FROM_BACKEND).single())
             wrapperViewIrClass.declarations.add(property)
        }

        run {
            val constructor = context.symbolTable.declareConstructor(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor.unsubstitutedPrimaryConstructor!!)
                .buildWithScope(context) { constructor ->

                    constructor.createParameterDeclarations()

                    val statements = mutableListOf<IrStatement>()
                    val superConstructor = context.symbolTable.referenceConstructor(wrapperViewIrClass.descriptor.getSuperClassNotAny()!!.constructors.single{it.valueParameters.size == 1})
                    val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0)
                    superCall.putValueArgument(0, IrGetValueImpl(-1, -1, constructor.valueParameters[0].symbol))
                    statements.add(superCall)
                    val linearLayoutClass = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.widget.LinearLayout")))!!
                    val linearLayoutParamsClass = linearLayoutClass.unsubstitutedMemberScope.getContributedClassifier(Name.identifier("LayoutParams"), NoLookupLocation.FROM_BACKEND)!! as ClassDescriptor
                    val linearLayoutParamsConstructor = context.symbolTable.referenceConstructor(linearLayoutParamsClass.constructors.single { it.valueParameters.size == 2 && it.valueParameters[0].type == context.moduleDescriptor.builtIns.intType }!!)

                    val layoutParams = IrCallImpl(-1, -1, linearLayoutParamsClass.defaultType,
                                                             linearLayoutParamsConstructor,
                                                             linearLayoutParamsConstructor.descriptor, null)
                    layoutParams.putValueArgument(0, IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(linearLayoutParamsClass.staticScope.getContributedVariables(Name.identifier("MATCH_PARENT"), NoLookupLocation.FROM_BACKEND).single())))
                    layoutParams.putValueArgument(1, IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(linearLayoutParamsClass.staticScope.getContributedVariables(Name.identifier("WRAP_CONTENT"), NoLookupLocation.FROM_BACKEND).single())))



                    val topLevelComponentCompanion = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.Component")))!!.companionObjectDescriptor!!
                    val addWrapperFunction = topLevelComponentCompanion.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("addWrapper"), NoLookupLocation.FROM_BACKEND).single()
                    val addWrapperCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                         context.symbolTable.referenceSimpleFunction(addWrapperFunction),
                                                         addWrapperFunction, null)
                    addWrapperCall.dispatchReceiver = IrGetObjectValueImpl(-1, -1, topLevelComponentCompanion.defaultType, context.symbolTable.referenceClass(topLevelComponentCompanion))
                    addWrapperCall.putValueArgument(0, IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol))
                    statements.add(addWrapperCall)



                    val setLayoutParamsFunction = linearLayoutClass.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("setLayoutParams"), NoLookupLocation.FROM_BACKEND).single()
                    statements.add(IrInstanceInitializerCallImpl(-1, -1, wrapperViewIrClass.symbol))
                    val setLayoutParamsCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                            context.symbolTable.referenceSimpleFunction(setLayoutParamsFunction),
                                                            setLayoutParamsFunction, null)
                    setLayoutParamsCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol)
                    setLayoutParamsCall.putValueArgument(0, layoutParams)
                    statements.add(setLayoutParamsCall)



                    val componentInstanceProperty = wrapperViewIrClass.declarations.filter { it.descriptor is PropertyDescriptor && it.descriptor.name.identifier == "componentInstance" }.single().descriptor as PropertyDescriptor
                    val componentConstructor = component.declarations.filter { it is IrConstructor }.single() as IrConstructor
                    val componentConstructorCall = IrCallImpl(-1, -1, component.descriptor.defaultType,
                                                              componentConstructor.symbol,
                                                              componentConstructor.descriptor, null)
                    statements.add(IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(componentInstanceProperty), IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol), componentConstructorCall))

                    constructor.body = IrBlockBodyImpl(-1, -1, statements)
                }
            wrapperViewIrClass.declarations.add(constructor)

        }

        run {
            val functionDescriptor = syntheticClassDescriptor.onAttachDescriptor
            val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
                .buildWithScope(context) { irFunction ->

                    irFunction.createParameterDeclarations()


                    val flushFunction = context.symbolTable.referenceFunction(syntheticClassDescriptor.flushComponentRerenderDescriptor)
                    val flushCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                               flushFunction,
                                               flushFunction.descriptor
                                                , null)
                    flushCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol)

                    val linearLayoutClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.widget.LinearLayout")))!!
                    val superFunction = linearLayoutClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("onAttachedToWindow"), NoLookupLocation.FROM_BACKEND).single()
                    val superCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                               context.symbolTable.referenceSimpleFunction(superFunction),
                                               superFunction
                                               , null, null, context.symbolTable.referenceClass(syntheticClassDescriptor.getSuperClassNotAny()!!))
                    superCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol)

                    irFunction.body = IrBlockBodyImpl(-1, -1, listOf(flushCall, superCall))
                }
            wrapperViewIrClass.declarations.add(irFunction)
        }

        run {
            val functionDescriptor = syntheticClassDescriptor.flushComponentRerenderDescriptor
            val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
                .buildWithScope(context) { irFunction ->



                    val childCountDescriptor = wrapperViewIrClass.descriptor.getSuperClassNotAny()!!.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("getChildCount"), NoLookupLocation.FROM_BACKEND).single {it.valueParameters.size == 0}
                    val childCountCall = IrGetterCallImpl(-1, -1,
                                                                   context.symbolTable.referenceSimpleFunction(childCountDescriptor),
                                                          childCountDescriptor, 0)
                    childCountCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol)

                    val renderIntoDescriptor = ComponentMetadata.fromDescriptor(component.descriptor).renderIntoViewGroupDescriptor
                    val renderIntoCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                          context.symbolTable.referenceSimpleFunction(renderIntoDescriptor),
                                                    renderIntoDescriptor, null)
                    val componentInstanceField = syntheticClassDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("componentInstance"), NoLookupLocation.FROM_BACKEND).single()
                    renderIntoCall.putValueArgument(0, IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol))
                    renderIntoCall.putValueArgument(1, IrConstImpl.int(-1, -1, context.builtIns.intType, 0))
                    renderIntoCall.putValueArgument(2, childCountCall)
                    renderIntoCall.dispatchReceiver = IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(componentInstanceField), IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol))

                    val exceptionClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.lang.Exception")))!!
                    val catchVariableDescriptor = LocalVariableDescriptor(irFunction.descriptor, Annotations.EMPTY, Name.identifier("e"), exceptionClassDescriptor.defaultType, SourceElement.NO_SOURCE)
                    val ctch = IrCatchImpl(-1, -1, context.symbolTable.declareVariable(-1, -1, IrDeclarationOrigin.CATCH_PARAMETER, catchVariableDescriptor))

                    val throwableClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.Throwable")))!!
                    val runtimeExceptionDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.lang.RuntimeException")))!!
                    val runtimeExceptionConstructor = runtimeExceptionDescriptor.constructors.single { it.valueParameters.size == 1 && (it.valueParameters[0].type.constructor.declarationDescriptor as ClassDescriptor).classId == throwableClassDescriptor.classId }
                    val runtimeExceptionCall = IrCallImpl(-1, -1, runtimeExceptionDescriptor.defaultType,
                                                      context.symbolTable.referenceConstructor(runtimeExceptionConstructor),
                                                          runtimeExceptionConstructor, null)
                    runtimeExceptionCall.putValueArgument(0, IrGetValueImpl(-1, -1, context.symbolTable.referenceVariable(catchVariableDescriptor)))

                    val irThrow = IrThrowImpl(-1, -1, context.builtIns.nothingType, runtimeExceptionCall)

                    ctch.result = IrBlockImpl(-1, -1, context.builtIns.unitType, null, listOf(irThrow))

                    val tri = IrTryImpl(-1, -1, context.builtIns.unitType, IrBlockImpl(-1, -1, context.builtIns.unitType, null, listOf(renderIntoCall)), listOf(ctch), null)

                    irFunction.body = IrBlockBodyImpl(-1, -1, listOf(tri))
                }
            wrapperViewIrClass.declarations.add(irFunction)
        }

        val renderHelperClassDescriptor = GeneratedRerenderHelperClassDescriptor(
                syntheticClassDescriptor.fqNameSafe.shortName().identifier + "\$RenderHelper",
                syntheticClassDescriptor.containingDeclaration,
                syntheticClassDescriptor
        )

        run {
            val functionDescriptor = syntheticClassDescriptor.getRerenderMethodDescriptor()
            val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
                .buildWithScope(context) { irFunction ->

                    val looperClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.os.Looper")))!!
                    val mainLooperFunction = looperClassDescriptor.staticScope.getContributedFunctions(Name.identifier("getMainLooper"), NoLookupLocation.FROM_BACKEND).single()
                    val mainLooperFunctionCall = IrCallImpl(-1, -1, looperClassDescriptor.defaultType,
                                                            context.symbolTable.referenceSimpleFunction(mainLooperFunction),
                                                            mainLooperFunction, null)

                    val handlerClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.os.Handler")))!!
                    val handlerConstructor = handlerClassDescriptor.constructors.filter { it.valueParameters.size == 1 && (it.valueParameters[0].type.constructor.declarationDescriptor as ClassDescriptor).classId == looperClassDescriptor.classId}.single()
                    val handlerConstructorCall = IrCallImpl(-1, -1, handlerClassDescriptor.defaultType,
                                                            context.symbolTable.referenceConstructor(handlerConstructor),
                                                            handlerConstructor, null)
                    handlerConstructorCall.putValueArgument(0, mainLooperFunctionCall)

                    val helperConstructor = renderHelperClassDescriptor.unsubstitutedPrimaryConstructor!!
                    val helperConstructorCall = IrCallImpl(-1, -1, renderHelperClassDescriptor.defaultType,
                                                            context.symbolTable.referenceConstructor(helperConstructor),
                                                            helperConstructor, null)
                    helperConstructorCall.putValueArgument(0, IrGetValueImpl(-1, -1, wrapperViewIrClass.thisReceiver!!.symbol))

                    val postFunction = handlerClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("post"), NoLookupLocation.FROM_BACKEND).single()
                    val postFunctionCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.booleanType,
                                                           context.symbolTable.referenceSimpleFunction(postFunction),
                                                      postFunction, null)
                    postFunctionCall.putValueArgument(0, helperConstructorCall)
                    postFunctionCall.dispatchReceiver = handlerConstructorCall


                    irFunction.body = IrBlockBodyImpl(-1, -1, listOf(postFunctionCall))
                }
            wrapperViewIrClass.declarations.add(irFunction)
        }

        run {
            val renderHelperIrClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, renderHelperClassDescriptor)
            renderHelperIrClass.createParameterDeclarations()
            wrapperViewIrClass.declarations.add(renderHelperIrClass)

            run {
                val propertyDescriptor = PropertyDescriptorImpl.create(component.descriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("componentInstance"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false)
                propertyDescriptor.setType(KotlinTypeFactory.simpleType(Annotations.EMPTY, component.descriptor.typeConstructor, emptyList<TypeProjection>(), true), emptyList<TypeParameterDescriptor>(), component.descriptor.thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
                renderHelperIrClass.declarations.add(IrFieldImpl(-1, -1, IrDeclarationOrigin.DEFINED, propertyDescriptor))
            }

            // TODO: Move into descriptor
            val viewFieldDescriptor = PropertyDescriptorImpl.create(renderHelperIrClass.descriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("view"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false)
            viewFieldDescriptor.setType(KotlinTypeFactory.simpleType(Annotations.EMPTY, wrapperViewIrClass.descriptor.typeConstructor, emptyList<TypeProjection>(), true), emptyList<TypeParameterDescriptor>(), renderHelperIrClass.descriptor.thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
            renderHelperIrClass.declarations.add(context.symbolTable.declareField(-1, -1, IrDeclarationOrigin.DEFINED, viewFieldDescriptor))

            run {
                val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, renderHelperClassDescriptor.getRunMethodDescriptor())
                    .buildWithScope(context) { irFunction ->
                        irFunction.createParameterDeclarations()
                        val flushFunction = context.symbolTable.referenceFunction(syntheticClassDescriptor.flushComponentRerenderDescriptor)
                        val flushCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                   flushFunction,
                                                   flushFunction.descriptor
                                                   , null)
                        flushCall.dispatchReceiver = IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(viewFieldDescriptor), IrGetValueImpl(-1, -1, renderHelperIrClass.thisReceiver!!.symbol))

                        irFunction.body = IrBlockBodyImpl(-1, -1, listOf(flushCall))
                    }
                renderHelperIrClass.declarations.add(irFunction)
            }

            run {
                val helperConstructor = renderHelperClassDescriptor.unsubstitutedPrimaryConstructor!!
                val irConstructor = context.symbolTable.declareConstructor(-1, -1, IrDeclarationOrigin.DEFINED, helperConstructor)
                    .buildWithScope(context) { irConstructor ->

                        irConstructor.createParameterDeclarations()

                        val superConstructor = context.symbolTable.referenceConstructor(renderHelperIrClass.descriptor.getSuperClassOrAny().constructors.single { it.valueParameters.size == 0})
                        val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0)

                        val setField = IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(viewFieldDescriptor), IrGetValueImpl(-1, -1, renderHelperIrClass.thisReceiver!!.symbol), IrGetValueImpl(-1, -1, irConstructor.valueParameters.first().symbol))

                        irConstructor.body = IrBlockBodyImpl(-1, -1, listOf(superCall, setField))
                    }
                renderHelperIrClass.declarations.add(irConstructor)
            }

        }


        return wrapperViewIrClass
    }

    fun generateWriteStringInvocation(context: GeneratorContext, outputStream: IrExpression, string: IrExpression): IrStatement {
        val KotlinStringToByteArrayWriteMethodDescriptor = context.moduleDescriptor.getPackage(FqName.topLevel(Name.identifier("kotlin.text"))).memberScope.getContributedFunctions(Name.identifier("toByteArray"), NoLookupLocation.FROM_BACKEND).single()
        val KotlinStringToByteArrayWriteMethodSymbol = context.symbolTable.referenceSimpleFunction(KotlinStringToByteArrayWriteMethodDescriptor)
        val KotlinStringToByteArrayCall = IrCallImpl(string.startOffset, string.endOffset, context.moduleDescriptor.builtIns.unitType,
                                                     KotlinStringToByteArrayWriteMethodSymbol,
                                                     KotlinStringToByteArrayWriteMethodDescriptor, null)
        KotlinStringToByteArrayCall.extensionReceiver = string

        val outputStreamClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.io.OutputStream")))!!
        val outputStreamWriteMethodDescriptor = outputStreamClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("write"), NoLookupLocation.FROM_BACKEND).filter { it.name == Name.identifier("write") && it.valueParameters.size == 1 && it.valueParameters[0].type is FlexibleType }.single()
        val outputStreamWriteMethodSymbol = context.symbolTable.referenceSimpleFunction(outputStreamWriteMethodDescriptor)
        val outputStreamReference = outputStream
        val outputStreamWriteCall = IrCallImpl(string.startOffset, string.endOffset, context.moduleDescriptor.builtIns.unitType,
                                               outputStreamWriteMethodSymbol,
                                               outputStreamWriteMethodDescriptor, null)
        outputStreamWriteCall.putValueArgument(0, KotlinStringToByteArrayCall)
        outputStreamWriteCall.dispatchReceiver = outputStreamReference
        return outputStreamWriteCall
    }

    fun transform(context: GeneratorContext, component: IrClass, function: IrFunction, tag: IrKtxTag): List<IrStatement> {
        val output: MutableList<IrStatement> = mutableListOf()


        val element = tag.element // TODO: Should transform pure IR rather than relying on the element

        val tagName = element.simpleTagName?.text
        val isSelfClosing = false

        val tagType = context.bindingContext.get(
                BindingContext.KTX_TAG_TYPE_DESCRIPTOR,
                element
        ) as ClassifierDescriptor? ?: throw NullPointerException("KTX tag does not know descriptor: " + element.text)





        val getMarkupBuilderFunction = component.descriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("markupBuilder"), NoLookupLocation.FROM_BACKEND).single().getter!!
        val getMarkupBuilderFunctionSymbol = context.symbolTable.referenceSimpleFunction(getMarkupBuilderFunction)
        val getMarkupBuilderCall = IrGetterCallImpl(-1, -1,
                                                    getMarkupBuilderFunctionSymbol,
                                                    getMarkupBuilderFunction, 0)
        getMarkupBuilderCall.dispatchReceiver = IrGetValueImpl(
                -1, -1,
                component.thisReceiver!!.symbol
        )

        val klassConstructor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.reflect.KClass")))!!.typeConstructor
        val klassSymbol = context.symbolTable.referenceClass(tagType as ClassDescriptor)
        val tagKlassReference = IrClassReferenceImpl(-1, -1, KotlinTypeFactory.simpleType(Annotations.EMPTY, klassConstructor, listOf(TypeProjectionImpl(tagType.defaultType)), false), klassSymbol, tagType.defaultType)


        val dslBuilderClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.R4aElementBuilderDSL")))!!

        val startComponentMethod = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("startComponent"), NoLookupLocation.FROM_BACKEND).single {(it as? SimpleFunctionDescriptor)?.valueParameters!![0].type.constructor.declarationDescriptor!!.name.identifier == "KClass"}
        val startComponentMethodCall = IrCallImpl(tag.startOffset, tag.endOffset, context.moduleDescriptor.builtIns.unitType,
                                               context.symbolTable.referenceSimpleFunction(startComponentMethod),
                                               startComponentMethod, null)
        startComponentMethodCall.putValueArgument(0, tagKlassReference)
        startComponentMethodCall.dispatchReceiver = getMarkupBuilderCall
        output.add(startComponentMethodCall)


        val attributeMethod = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("attribute"), NoLookupLocation.FROM_BACKEND).single()
        for(attribute in tag.attributes) {

            val attributeMethodCall = IrCallImpl(tag.startOffset, tag.endOffset, context.moduleDescriptor.builtIns.unitType,
                                                 context.symbolTable.referenceSimpleFunction(attributeMethod),
                                                 attributeMethod, null)
            attributeMethodCall.putValueArgument(0, IrConstImpl.string(-1, -1, context.builtIns.stringType, attribute.element.key!!.text))
            val value =
                    if(attribute.element.isStaticStringValue) {
                        val stringConstant = attribute.element.value!!.text.let { it.substring(1, it.length-1) };
                        IrConstImpl.string(-1, -1, context.builtIns.stringType, stringConstant)
                    }
                    else attribute.value
            attributeMethodCall.putValueArgument(0, IrConstImpl.string(-1, -1, context.builtIns.stringType, attribute.element.key!!.text))
            attributeMethodCall.putValueArgument(1, value)
            attributeMethodCall.dispatchReceiver = getMarkupBuilderCall

            output.add(attributeMethodCall)
        }

        if(tag.body != null)
            for(statement in tag.body) {
                if(statement is IrKtxTag) output.addAll(transform(context, component, function, statement))
                else output.add(statement)
            }

        val endComponentMethod = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("endComponent"), NoLookupLocation.FROM_BACKEND).single()
        val endComponentMethodCall = IrCallImpl(tag.startOffset, tag.endOffset, context.moduleDescriptor.builtIns.unitType,
                                                  context.symbolTable.referenceSimpleFunction(endComponentMethod),
                                                  endComponentMethod, null)
        endComponentMethodCall.dispatchReceiver = getMarkupBuilderCall
        output.add(endComponentMethodCall)

        // TODO: Ask Yan why this is getting rewritten
        val dslBuilderInternalClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/R4aElementBuilderDSL"

/*
        v.visitLdcInsn(Type.getType("L" + this.asmType(tagType.defaultType).internalName + ";"))
        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, dslBuilderInternalClassName, "startComponent", "(Ljava/lang/Class;)V", false)
        for (attribute in element.attributes!!) {
            v.visitInsn(Opcodes.DUP)
            v.visitLdcInsn(attribute.key!!.text)
            if (attribute.isStaticStringValue) {
                var text = attribute.value!!.text
                text = text.substring(1, text.length - 1) // Strip off the open/close quotes
                v.visitLdcInsn(text)
            } else
                gen(attribute.value as KtElement?, Type.getObjectType("java/lang/Object"))
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, dslBuilderInternalClassName, "attribute", "(Ljava/lang/String;Ljava/lang/Object;)V", false)
        }

        if (element.body != null) {
            generateBlock(element.body!!, true, null, null).put(Type.VOID_TYPE, v)
        }

        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, dslBuilderInternalClassName, "endComponent", "()V", false)





*/





        /*


        val outputStream = IrGetValueImpl(-1, -1, function.valueParameters.first().symbol)



        when(type) {
            ComponentType.TEXT_NODE -> {
                val attribute = element.attributes!!.single { attribute -> attribute.key!!.text == "text" }

                if (attribute.isStaticStringValue) {
                    var text = attribute.value!!.text
                    text = text.substring(1, text.length - 1) // Strip off the open/close quotes
                    output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, text)))
                } else {
                    val value = tag.attributes.single { it.element.key!!.text == "text" }.value
                    val toStringDescriptor = context.builtIns.any.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("toString"), NoLookupLocation.FROM_BACKEND).single { it.valueParameters.size == 0 }
                    val toStringSymbol = context.symbolTable.referenceSimpleFunction(toStringDescriptor)
                    val toStringCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                           toStringSymbol,
                                                           toStringDescriptor, null)
                    toStringCall.dispatchReceiver = value
                    output.add(generateWriteStringInvocation(context, outputStream, toStringCall))
                }
            }
            ComponentType.NATIVE_VIEW -> {
                output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, "<"+tagName)))

                for (attribute in tag.attributes) {

                    output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, " "+attribute.element.key!!.text+"=\"")))

                    if (attribute.element.isStaticStringValue) {
                        var text = attribute.element.value!!.text
                        text = text.substring(1, text.length - 1) // Strip off the open/close quotes
                        output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, text)))

                    } else {
                        val toStringDescriptor = context.builtIns.any.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("toString"), NoLookupLocation.FROM_BACKEND).single { it.valueParameters.size == 0 }
                        val toStringSymbol = context.symbolTable.referenceSimpleFunction(toStringDescriptor)
                        val toStringCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                      toStringSymbol,
                                                      toStringDescriptor, null)
                        toStringCall.dispatchReceiver = attribute.value
                        output.add(generateWriteStringInvocation(context, outputStream, toStringCall))
                    }

                    output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, "\"")))
                }

                output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, if(isSelfClosing) " />" else ">")))
            }
            ComponentType.COMPOSITE_COMPONENT -> {
                val componentMetadata = ComponentMetadata.fromDescriptor(tagType)
                val composeFunction = context.symbolTable.referenceSimpleFunction(componentMetadata.renderStreamDescriptor)
                val toStringCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                              composeFunction,
                                              composeFunction.descriptor, null)
                toStringCall.dispatchReceiver = IrGetObjectValueImpl(-1, -1, componentMetadata.getCompanion().defaultType, context.symbolTable.referenceClass(componentMetadata.getCompanion()))
                output.add(toStringCall)
                toStringCall.putValueArgument(0, outputStream)

                val parameterSlots = composeFunction.descriptor.valueParameters
                for(index in 1..parameterSlots.size-1) {  // Skip the first slot (output stream)
                    val attribute = tag.attributes.singleOrNull { it.element.key!!.text == parameterSlots[index].name.identifier } ?: continue
                    toStringCall.putValueArgument(index, attribute.value)
                }
            }
        }

        if (tag.body != null) {
            for(statement in tag.body) {
                // TODO: Do we need to push a new scope (would IrBlockBody create a scope?) or does it not matter because everything is already resolved to IR
                if(statement is IrKtxTag) output.addAll(transform(context, function, statement))
                else output.add(statement)
            }
        }

        when(type) {
            ComponentType.TEXT_NODE -> {
            }
            ComponentType.NATIVE_VIEW -> {
                if(!isSelfClosing) {
                    output.add(generateWriteStringInvocation(context, outputStream, IrConstImpl.string(-1, -1, context.builtIns.stringType, "</" + tagName + ">")))
                }
            }
            ComponentType.COMPOSITE_COMPONENT -> {
            }
        }

        */

        return output
    }

    fun IrElement.find(filter: (descriptor: IrElement) -> Boolean): Collection<IrElement> {
        val elements: MutableList<IrElement> = mutableListOf()
        accept(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                if(filter(element)) elements.add(element)
                element.acceptChildren(this,null)
            }
            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?) {
                expression.acceptChildren(this, null)
            }
        }, null)
        return elements
    }

    inline fun <T : IrDeclaration> T.buildWithScope(context: GeneratorContext, builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withScope(irDeclaration.descriptor) {
                builder(irDeclaration)
            }
        }
}

class IrKtxTag(val element: KtxElement, val body: Collection<IrStatement>?, val attributes: Collection<IrKtxAttribute>) : IrKtxStatement {
    override val startOffset = element.startOffset
    override val endOffset = element.endOffset
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitKtxStatement(this, data);
    }
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        for(attribute in attributes) attribute.accept(visitor, data)
        if(body != null) for(statement in body) statement.accept(visitor, data)
    }
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        throw UnsupportedOperationException("This temporary placeholder node must be manually transformed, not visited")
    }
}

class IrKtxAttribute(val element: KtxAttribute, val value: IrExpression?) : IrKtxStatement {
    override val startOffset = element.startOffset
    override val endOffset = element.endOffset
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitKtxStatement(this, data)
    }
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        if(value != null) value.accept(visitor, data);
    }
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        throw UnsupportedOperationException("This temporary placeholder node must be manually transformed, not visited")
    }
}
