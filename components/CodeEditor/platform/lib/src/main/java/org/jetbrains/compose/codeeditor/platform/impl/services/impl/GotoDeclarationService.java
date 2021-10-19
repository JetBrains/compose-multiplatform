package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.GTDData;
import org.jetbrains.compose.codeeditor.platform.impl.GTDTarget;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.model.psi.impl.TargetsKt;
import com.intellij.navigation.DirectNavigationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtilKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class GotoDeclarationService {

    private static final GotoDeclarationService INSTANCE = new GotoDeclarationService();

    private final List<GTDSupplier> gtdSuppliers;

    private GotoDeclarationService() {
        GTDSupplier inner = new InnerDeclarations();
        gtdSuppliers = List.of(
            new FromProviders(),
            inner,
            new FromHostFile(inner)
        );
    }

    static GotoDeclarationService getInstance() {
        return INSTANCE;
    }

    GTDData gotoDeclaration(Editor editor, PsiFile file, VirtualFile sourceRoot) {
        TextRange initialElementRange = TextRange.EMPTY_RANGE;
        try {
            int offset = editor.getCaretModel().getOffset();
            int adjustedOffset = TargetElementUtil.adjustOffset(file, editor.getDocument(), offset);
            PsiElement initialElement = file.findElementAt(adjustedOffset);
            if (initialElement == null) return GTDData.createNonNavigatable(initialElementRange);
            initialElementRange = initialElement.getTextRange();
            for (GTDSupplier gtdSupplier : gtdSuppliers) {
                var results = gtdSupplier.getResults(editor, file, offset);
                if (results != null) {
                    var targets = mapToTargets(initialElement, results, file, sourceRoot);
                    return targets.isEmpty()
                        ? GTDData.createNonNavigatable(initialElementRange)
                        : new GTDData(targets, initialElementRange);
                }
            }
        } catch (IndexNotReadyException e) {
            return GTDData.createIndexNotReady(initialElementRange);
        }
        return GTDData.createNonNavigatable(initialElementRange);
    }

    private Collection<GTDTarget> mapToTargets(PsiElement initialElement,
                                               Collection<? extends PsiElement> elements,
                                               PsiFile file,
                                               VirtualFile sourceRoot) {
        if (elements.isEmpty()) return Collections.emptyList();
        Collection<GTDTarget> targets = new ArrayList<>(elements.size());
        int offset = initialElement.getTextOffset();
        for (PsiElement element : elements) {
            var navigationElement = element.getNavigationElement();
            var containingFile = navigationElement.getContainingFile();
            if (containingFile == null) continue;
            int textOffset = navigationElement.getTextOffset();
            if (textOffset == offset && containingFile.equals(file)) continue;
            String vFilePath = containingFile.getVirtualFile().getPath();
            String sourceRootPath = sourceRoot.getPath();
            String path;
            if (vFilePath.startsWith(sourceRootPath)) {
                path = vFilePath.substring(sourceRootPath.length() + 1);
            } else {
                path = vFilePath;
            }
            targets.add(new GTDTarget(FileUtil.toSystemDependentName(path), textOffset));
        }
        return targets;
    }

    private interface GTDSupplier {
        Collection<? extends PsiElement> getResults(Editor editor, PsiFile file, int offset);
    }

    private static class FromHostFile implements GTDSupplier {
        private final GTDSupplier wrappable;

        FromHostFile(GTDSupplier wrappable) {
            this.wrappable = wrappable;
        }

        @Override
        public Collection<? extends PsiElement> getResults(Editor editor, PsiFile file, int offset) {
            var manager = InjectedLanguageManager.getInstance(file.getProject());
            var topLevelFile = manager.getTopLevelFile(file);
            if (topLevelFile == null) return null;
            return wrappable.getResults(editor, topLevelFile, manager.injectedToHost(file, offset));
        }
    }

    private static class FromProviders implements GTDSupplier {
        @Override
        public Collection<? extends PsiElement> getResults(Editor editor, PsiFile file, int offset) {
            if (file == null) return null;
            int adjustedOffset = TargetElementUtil.adjustOffset(file, editor.getDocument(), offset);
            PsiElement leafElement = file.findElementAt(adjustedOffset);
            if (leafElement == null) return null;
            for (var handler : GotoDeclarationHandler.EP_NAME.getExtensionList()) {
                PsiElement[] targets = handler.getGotoDeclarationTargets(leafElement, offset, editor);
                if (targets == null || targets.length == 0) continue;
                return List.of(targets);
            }
            return null;
        }
    }

    private static class InnerDeclarations implements GTDSupplier {
        private final GTDSupplier fromDirectNavigation = new FromDirectNavigation();
        private final GTDSupplier fromTargetData = new FromTargetData();

        @Override
        public Collection<? extends PsiElement> getResults(Editor editor, PsiFile file, int offset) {
            var result = fromDirectNavigation.getResults(editor, file, offset);
            return result != null ? result : fromTargetData.getResults(editor, file, offset);
        }
    }

    private static class FromDirectNavigation implements GTDSupplier {
        @Override
        public Collection<? extends PsiElement> getResults(Editor editor, PsiFile file, int offset) {
            if (DirectNavigationProvider.EP_NAME.getExtensionList().isEmpty()) return null;
            var iter = PsiTreeUtilKt.elementsAroundOffsetUp(file, offset);
            while (iter.hasNext()) {
                PsiElement element = iter.next().getFirst();
                for (var provider : DirectNavigationProvider.EP_NAME.getExtensionList()) {
                    PsiElement navigationElement = provider.getNavigationElement(element);
                    if (navigationElement != null) {
                        return List.of(navigationElement);
                    }
                }
            }
            return null;
        }
    }

    private static class FromTargetData implements GTDSupplier {
        @Override
        public Collection<? extends PsiElement> getResults(Editor editor, PsiFile file, int offset) {
            Collection<Symbol> targetSymbols = TargetsKt.targetSymbols(file, offset);
            if (targetSymbols.isEmpty()) return null;
            return targetSymbols
                .stream()
                .map(PsiSymbolService.getInstance()::extractElementFromSymbol)
                .collect(Collectors.toList());
        }
    }

}
