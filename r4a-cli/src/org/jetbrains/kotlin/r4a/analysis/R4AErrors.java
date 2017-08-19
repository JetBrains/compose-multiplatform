package org.jetbrains.kotlin.r4a.analysis;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.Errors;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;

public interface R4AErrors {
    DiagnosticFactory0<PsiElement> DUPLICATE_ATTRIBUTE = DiagnosticFactory0.create(ERROR);


    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(R4AErrors.class);
        }
    };

}
