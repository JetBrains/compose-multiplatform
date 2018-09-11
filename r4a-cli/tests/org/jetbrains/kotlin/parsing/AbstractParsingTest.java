/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PathUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.cli.common.script.CliScriptDefinitionProvider;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.r4a.R4aKtxParsingExtension;
import org.jetbrains.kotlin.script.ScriptDefinitionProvider;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class AbstractParsingTest extends KtParsingTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getProject().registerService(ScriptDefinitionProvider.class, CliScriptDefinitionProvider.class);
        KtxParsingExtension.Companion.registerExtension(getProject(), new R4aKtxParsingExtension());
    }

    @Override
    protected String getTestDataPath() {
        return KotlinTestUtils.getHomeDirectory();
    }

    public AbstractParsingTest() {
        super(".", "kt", new KotlinParserDefinition());
    }

    private static void checkPsiGetters(KtElement elem) throws Throwable {
        Method[] methods = elem.getClass().getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (!methodName.startsWith("get") && !methodName.startsWith("find") ||
                methodName.equals("getReference") ||
                methodName.equals("getReferences") ||
                methodName.equals("getUseScope") ||
                methodName.equals("getPresentation")) {
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.getParameterTypes().length > 0) continue;

            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getName().startsWith("org.jetbrains.kotlin")) continue;

            Object result = method.invoke(elem);
            if (result == null) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation instanceof IfNotParsed) {
                        assertNotNull(
                                "Incomplete operation in parsed OK test, method " + methodName +
                                " in " + declaringClass.getSimpleName() + " returns null. Element text: \n" + elem.getText(),
                                PsiTreeUtil.findChildOfType(elem, PsiErrorElement.class));
                    }
                }
            }
        }
    }

    protected void doParsingTest(@NotNull String filePath) throws Exception {
        doBaseTest(filePath, KtNodeTypes.KT_FILE, null);
    }

    protected void doParsingTest(@NotNull String filePath, Function1<String, String> contentFilter) throws Exception {
        doBaseTest(filePath, KtNodeTypes.KT_FILE, contentFilter);
    }

    protected void doExpressionCodeFragmentParsingTest(@NotNull String filePath) throws Exception {
        doBaseTest(filePath, KtNodeTypes.EXPRESSION_CODE_FRAGMENT, null);
    }

    protected void doBlockCodeFragmentParsingTest(@NotNull String filePath) throws Exception {
        doBaseTest(filePath, KtNodeTypes.BLOCK_CODE_FRAGMENT, null);
    }

    private void doBaseTest(@NotNull String filePath, @NotNull IElementType fileType, Function1<String, String> contentFilter) throws Exception {
        String fileContent = loadFile(filePath);

        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath));
        myFile = createFile(filePath, fileType, contentFilter != null ? contentFilter.invoke(fileContent) : fileContent);

        myFile.acceptChildren(new KtVisitorVoid() {
            @Override
            public void visitKtElement(@NotNull KtElement element) {
                element.acceptChildren(this);
                try {
                    checkPsiGetters(element);
                }
                catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });

        doCheckResult(myFullDataPath, filePath.replaceAll("\\.kts?", ".txt"), toParseTreeText(myFile, false, false).trim());
    }

    private PsiFile createFile(@NotNull String filePath, @NotNull IElementType fileType, @NotNull String fileContent) {
        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(myProject);

        if (fileType == KtNodeTypes.EXPRESSION_CODE_FRAGMENT) {
            return psiFactory.createExpressionCodeFragment(fileContent, null);
        }
        else if (fileType == KtNodeTypes.BLOCK_CODE_FRAGMENT) {
            return psiFactory.createBlockCodeFragment(fileContent, null);
        }
        else {
            return createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileContent);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Temp workaround for setting FileTypeRegistry.ourInstanceGetter to CoreFileTypeRegistry forever
        // Reproducible with pattern test kind:
        // org.jetbrains.kotlin.parsing.JetParsingTestGenerated$Psi||org.jetbrains.kotlin.codegen.TestlibTest||org.jetbrains.kotlin.completion.MultiFileJvmBasicCompletionTestGenerated

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        FileTypeRegistry.ourInstanceGetter = new Getter<FileTypeRegistry>() {
            @Override
            public FileTypeRegistry get() {
                return FileTypeManager.getInstance();
            }
        };
    }
}
