package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.edt.EdtUtil;
import org.jetbrains.compose.codeeditor.platform.impl.GTDData;
import org.jetbrains.compose.codeeditor.platform.impl.services.EditorService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ModuleService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ProjectService;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.IpwLookupImpl;
import com.intellij.facet.FacetManager;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.text.AsyncEditorLoader;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.concurrency.CancellablePromise;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.LockSupport;

public class EditorServiceImpl extends IpwService implements EditorService {

    private ProjectService myProjectService;
    private ModuleService myModuleService;
    private VirtualFile myFile;
    private PsiFile myPsiFile;
    private Editor myEditor;
    private CancellablePromise<GTDData> gtdExecutionProgress;

    public EditorServiceImpl(ProjectService projectService, ModuleService moduleService) {
        myProjectService = projectService;
        myModuleService = moduleService;
    }

    @Override
    protected void doInit() {
    }

    @Override
    public void dispose() {
        cancelExecutions();
        clearFileAndEditor();
    }

    @Override
    public void cancelExecutions() {
        if (gtdExecutionProgress != null) {
            gtdExecutionProgress.cancel();
            gtdExecutionProgress = null;
        }
    }

    @Override
    public void clearFileAndEditor() {
        myFile = null;
        myPsiFile = null;
        myEditor = null;
    }

    @Override
    public List<LookupElement> getCodeCompletion(VirtualFile file, int caretOffset) {
        setCodeInsightSettings();
        configure(file, caretOffset);
        return complete(CompletionType.BASIC, 0);
    }

    @Override
    public GTDData gotoDeclaration(VirtualFile file, int caretOffset) {
        configure(file, caretOffset);
        gtdExecutionProgress = ReadAction
            .nonBlocking(() ->
                GotoDeclarationService
                    .getInstance()
                    .gotoDeclaration(myEditor, myPsiFile, myModuleService.getSourceRoot()))
            .withDocumentsCommitted(myProjectService.getProject())
            .expireWhen(() -> myEditor == null || myPsiFile == null)
            .submit(AppExecutorUtil.getAppExecutorService());
        try {
            var result = gtdExecutionProgress.get();
            return result != null ? result : GTDData.NON_NAVIGATABLE;
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            return GTDData.NON_NAVIGATABLE;
        }
    }

    private void setCodeInsightSettings() {
        var settings = CodeInsightSettings.getInstance();
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = false;
        settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = false;
        settings.AUTO_POPUP_COMPLETION_LOOKUP = false;
    }

    private void configure(VirtualFile file, int caretOffset) {
        CaretLoader loader = CaretLoader.fromFile(file, caretOffset);
        EdtUtil.runInEdtAndWait(() -> configureEditor(file, loader));
    }

    private void configureEditor(VirtualFile file, CaretLoader loader) {
        setFileAndEditor(file, createEditor(file));
        if (myEditor == null) {
            throw new RuntimeException("Editor couldn't be created for: " + file.getPath());
        }

        setCaret(loader.caretInfo);

        Module module = myModuleService.getModule();
        if (module != null) {
            for (var facet : FacetManager.getInstance(module).getAllFacets()) {
                FacetManager.getInstance(module).facetConfigurationChanged(facet);
            }
        }
        PsiDocumentManager.getInstance(myProjectService.getProject()).commitAllDocuments();

        setupEditorForInjectedLanguage();
    }

    private List<LookupElement> complete(CompletionType type, int invocationCount) {
        Ref<List<LookupElement>> ref = Ref.create();
        Project project = myProjectService.getProject();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                var handler = new CodeCompletionHandlerBase(type);
                Editor editor = getCompletionEditor();
                if (editor == null) {
                    throw new RuntimeException("Editor couldn't be created for the complete operation");
                }
                handler.invokeCompletion(project, editor, invocationCount);
                PsiDocumentManager.getInstance(project).commitAllDocuments();
            }, null, null, myEditor.getDocument());
            var lookup = ((IpwLookupImpl)LookupManager.getActiveLookup(myEditor));
            ref.set(lookup == null ? Collections.emptyList() : lookup.getItems());
        });
        return ref.get();
    }

    private Editor getCompletionEditor() {
        return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(myEditor, myPsiFile);
    }

    private void setFileAndEditor(VirtualFile file, Editor editor) {
        myFile = file;
        myEditor = editor;
        myPsiFile = ReadAction.compute(() -> PsiManager.getInstance(myProjectService.getProject()).findFile(myFile));
    }

    private Editor createEditor(VirtualFile file) {
        Project project = myProjectService.getProject();
        var instance = FileEditorManager.getInstance(project);
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        Editor editor = instance.openTextEditor(new OpenFileDescriptor(project, file), false);
        waitForLoading(editor);
        return editor;
    }

    private void waitForLoading(Editor editor) {
        if (EditorUtil.isRealFileEditor(editor)) {
            UIUtil.dispatchAllInvocationEvents();

            while (!AsyncEditorLoader.isEditorLoaded(editor)) {
                LockSupport.parkNanos(100_000_000);
                UIUtil.dispatchAllInvocationEvents();
            }
        }
    }

    private void setCaret(CaretInfo caretInfo) {
        Editor editor = myEditor;
        CaretModel caretModel = editor.getCaretModel();
        CaretState caretState = new CaretState(
            caretInfo.position == null ? null
                : editor.offsetToLogicalPosition(caretInfo.getCaretOffset(editor.getDocument())),
            null, null);
        caretModel.setCaretsAndSelections(List.of(caretState));
    }

    private void setupEditorForInjectedLanguage() {
        Editor editor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(myEditor, myPsiFile);
        if (editor instanceof EditorWindow) {
            setFileAndEditor(((EditorWindow)editor).getInjectedFile().getViewProvider().getVirtualFile(), editor);
        }
    }

    private static final class CaretLoader {
        private final CaretInfo caretInfo;

        private CaretLoader(String fileText, int caretOffset) {
            if (caretOffset > fileText.length()) {
                caretInfo = new CaretInfo(null);
            } else {
                Document document = EditorFactory.getInstance().createDocument(fileText);
                caretInfo = extractCaretByOffset(document, caretOffset);
            }
        }

        private CaretInfo extractCaretByOffset(Document document, int caretOffset) {
            return WriteCommandAction.writeCommandAction(null).compute(() -> {
                RangeMarker caretMarker = document.createRangeMarker(caretOffset, caretOffset);
                return getCaretInfo(document, caretMarker);
            });
        }

        private CaretInfo getCaretInfo(Document document, RangeMarker caretMarker) {
            int line = document.getLineNumber(caretMarker.getStartOffset());
            int column = caretMarker.getStartOffset() - document.getLineStartOffset(line);
            var caretPosition = new LogicalPosition(line, column);
            return new CaretInfo(caretPosition);
        }

        private static CaretLoader fromFile(VirtualFile file, int caretOffset) {
            return fromIoSource(() -> VfsUtilCore.loadText(file), caretOffset);
        }

        private static CaretLoader fromIoSource(ThrowableComputable<String, IOException> source, int caretOffset) {
            try {
                var fileText = StringUtil.convertLineSeparators(source.compute());
                return new CaretLoader(fileText, caretOffset);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class CaretInfo {
        private final LogicalPosition position;

        CaretInfo(LogicalPosition position) {
            this.position = position;
        }

        int getCaretOffset(Document document) {
            return position == null ? -1 : document.getLineStartOffset(position.line) + position.column;
        }
    }
}
