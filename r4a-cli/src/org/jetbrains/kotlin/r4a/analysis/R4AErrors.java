package org.jetbrains.kotlin.r4a.analysis;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtxAttribute;
import org.jetbrains.kotlin.psi.KtxElement;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;

public interface R4AErrors {
    DiagnosticFactory0<PsiElement> DUPLICATE_ATTRIBUTE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> OPEN_COMPONENT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<KtxAttribute, DeclarationDescriptor, String, KotlinType> UNRESOLVED_ATTRIBUTE_KEY = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtxAttribute, String, KotlinType, KotlinType> MISMATCHED_ATTRIBUTE_TYPE = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<KtxAttribute, SimpleFunctionDescriptor>
            MISMATCHED_ATTRIBUTE_TYPE_NO_SINGLE_PARAM_SETTER_FNS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtxElement, Collection<DeclarationDescriptor>> MISSING_REQUIRED_ATTRIBUTES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, Collection<KotlinType>> INVALID_TAG_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, Collection<KotlinType>> INVALID_TAG_DESCRIPTOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtElement> SUSPEND_FUNCTION_USED_AS_SFC = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> INVALID_TYPE_SIGNATURE_SFC = DiagnosticFactory0.create(ERROR);


    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(R4AErrors.class);
        }
    };

}
