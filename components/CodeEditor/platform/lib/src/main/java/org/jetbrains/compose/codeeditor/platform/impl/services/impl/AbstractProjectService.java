package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.edt.EdtUtil;
import org.jetbrains.compose.codeeditor.platform.impl.GTDData;
import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.EditorService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ModuleService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ProjectService;
import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.EditorTracker;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.GeneratedSourceFileChangeTracker;
import com.intellij.ide.GeneratedSourceFileChangeTrackerImpl;
import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.ide.structureView.StructureViewFactory;
import com.intellij.ide.structureView.impl.StructureViewFactoryImpl;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.projectImport.ProjectOpenedCallback;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiDocumentManagerImpl;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl;
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings;
import com.intellij.serviceContainer.ComponentManagerImpl;
import com.intellij.ui.UiInterceptors;
import com.intellij.util.DocumentUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.io.PathKt;
import com.intellij.util.ui.UIUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker;
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle;
import org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class AbstractProjectService extends IpwService implements ProjectService {

    protected ApplicationService myApplicationService;
    protected FileService myFileService;
    private EditorService myEditorService;
    private Disposable myRootDisposable;
    private ModuleService myModuleService;
    private Project myProject;
    private PsiManagerImpl myPsiManager;
    final String myProjectName;
    private Path myProjectFolder;
    private DataProvider myDataProvider;
    private boolean isDisposed;

    protected AbstractProjectService(ApplicationService applicationService, FileService fileService,
                                     String projectName) {
        myApplicationService = applicationService;
        myFileService = fileService;
        myProjectName = projectName;
    }

    @Override
    protected void doInit() {
        myProjectFolder = getProjectFolder();

        myModuleService = createModuleService();
        myModuleService.init(this);

        myEditorService = new EditorServiceImpl(this, myModuleService);
        myEditorService.init(this);
    }

    protected abstract Path getProjectFolder();

    protected abstract ModuleService createModuleService();

    @Override
    public void dispose() {
        if (isDisposed) return;
        try {
            EdtUtil.runInEdtAndWait(() -> {
                if (myApplicationService.getApplication() == null) return;
                checkProject();
                doCleanUp();
                if (myRootDisposable != null && !Disposer.isDisposed(myRootDisposable)) {
                    Disposer.dispose(myRootDisposable);
                    myRootDisposable = null;
                }
                if (myProject != null) {
                    waitForThreads();
                }
                IpwUtil.appWaitForThreads();
                cleanUpProjectAndApp();
                IpwUtil.cleanupSwingDataStructures();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        isDisposed = true;
    }

    @Override
    public void createProject() {
        if (myProject != null) {
            throw new IllegalStateException("The project has already been created");
        }
        myRootDisposable = Disposer.newDisposable("IpwProjectDisposable");
        EdtUtil.runInEdtAndWait(() -> {
            var app = myApplicationService.getApplication();
            app.invokeAndWait(this::initProject);
            openProject();
            app.invokeAndWait(this::postProjectActions);
            InjectedLanguageManagerImpl.pushInjectors(myProject);
            myDataProvider = new IpwDataProvider(myProject);
            myApplicationService.setDataProvider(myDataProvider);
            myModuleService.synchronizeSourceRoot();
            myPsiManager = ((PsiManagerImpl)PsiManager.getInstance(myProject));
            var daemonCodeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(myProject);
            daemonCodeAnalyzer.prepareForTest();
            ensureIndexesUpToDate();
        });

        var javaSdkVersion = Objects.requireNonNull(
            JavaSdk.getInstance().getVersion(myApplicationService.getSdk())
        );
        LanguageLevelProjectExtension.getInstance(myProject).setLanguageLevel(javaSdkVersion.getMaxLanguageLevel());

        KotlinStyleGuideCodeStyle.Companion.apply(CodeStyle.getSettings(myProject));

        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            ModuleRootManager.getInstance(module).orderEntries()
                             .getAllLibrariesAndSdkClassesRoots();
        }

        //noinspection ResultOfObjectAllocationIgnored
        new UnusedSymbolInspection();

        EditorTracker.getInstance(myProject);
        shakeUpLibrary();
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void addLibraries(List<String> paths) {
        checkProject();
        myModuleService.addLibraries(paths);
    }

    @Override
    public void synchronizeProjectDir() {
        checkProject();
        EdtUtil.runInEdtAndWait(myModuleService::synchronizeSourceRoot);
    }

    @Override
    public List<LookupElement> getCodeCompletion(String path, int caretOffset) {
        return myEditorService.getCodeCompletion(prepareAndGetFile(path), caretOffset);
    }

    @Override
    public GTDData gotoDeclaration(String path, int caretOffset) {
        myEditorService.cancelExecutions();
        Ref<GTDData> ref = Ref.create();
        EdtUtil.runInEdtAndWait(() -> ref.set(myEditorService.gotoDeclaration(prepareAndGetFile(path), caretOffset)));
        return ref.get();
    }

    private VirtualFile prepareAndGetFile(String path) {
        checkProject();
        shakeUpLibrary();
        myEditorService.clearFileAndEditor();
        return myModuleService.refreshFile(path);
    }

    @Override
    public void closeProject() {
        Disposer.dispose(this);
    }

    private void checkProject() {
        if (myProject == null) {
            throw new IllegalStateException("the project is not created");
        }
        myApplicationService.setDataProvider(myDataProvider);
    }

    private void shakeUpLibrary() {
        LibraryModificationTracker.getInstance(myProject).incModificationCount();
    }

    private void initProject() {
        IdeaLogger.ourErrorsOccurred = null;
        myApplicationService.getApplication().runWriteAction(this::cleanPersistedVFSContent);
        Path projectFile = myFileService
            .generateTempPath(myProjectName + ProjectFileType.DOT_DEFAULT_EXTENSION);
        myProject = Objects.requireNonNull(
            ProjectManagerEx.getInstanceEx().newProject(projectFile, createOpenProjectOptions())
        );
        myFileService.synchronizeDirVfs(projectFile);
        try {
            WriteAction.run(this::setUpProject);
        } catch (Throwable e) {
            try {
                closeAndDeleteProject();
            } catch (Throwable suppressed) {
                e.addSuppressed(suppressed);
            }
            throw new RuntimeException(e);
        }
    }

    private void setUpProject() {
        myModuleService.createModule(myProject, myProjectFolder);
    }

    private void openProject() {
        if (!ProjectManagerEx.getInstanceEx().openProject(myProject)) {
            throw new IllegalStateException("Can't open the project");
        }

        if (myApplicationService.getApplication().isDispatchThread()) {
            myApplicationService.dispatchAllInvocationEvents();
        }
    }

    private void postProjectActions() {
        clearUncommittedDocuments();
        InspectionsUtil.configureInspections(myProject, myRootDisposable);
        boolean passed = false;
        try {
            passed = StartupManagerEx.getInstanceEx(myProject).startupActivityPassed();
        } catch (Exception ignored) {}
        if (!passed || !myProject.isInitialized()) {
            throw new RuntimeException("The project was not initialized");
        }
        CodeStyle.setTemporarySettings(myProject, CodeStyle.createTestSettings());
        var manager = FileDocumentManager.getInstance();
        if (manager instanceof FileDocumentManagerImpl) {
            var unsavedDocuments = manager.getUnsavedDocuments();
            manager.saveAllDocuments();
            myApplicationService.getApplication()
                                .runWriteAction(((FileDocumentManagerImpl)manager)::dropAllUnsavedDocuments);
        }
        myApplicationService.dispatchAllInvocationEvents();
        ((FileTypeManagerImpl)FileTypeManager.getInstance()).drainReDetectQueue();
    }

    private void closeAndDeleteProject() {
        Project project = myProject;
        if (project == null) return;
        if (myApplicationService.getApplication().isWriteAccessAllowed()) {
            throw new IllegalStateException("Must not call closeAndDeleteProject from under write action");
        }

        if (!project.isDisposed()) {
            deleteProjectFiles(project);
        }

        myModuleService.closeModule();
        ProjectManagerEx.getInstanceEx().forceCloseProject(project);
        myProject = null;
        myProjectFolder = null;
        myPsiManager = null;
    }

    protected void deleteProjectFiles(Project project) {
        var projectFilePath = project.getProjectFilePath();
        if (projectFilePath != null) {
            Path ioFile = Paths.get(projectFilePath);
            if (Files.exists(ioFile)) {
                PathKt.delete(ioFile);
            }
        }
    }

    private void ensureIndexesUpToDate() {
        var project = myProject;
        if (!DumbService.isDumb(project)) {
            ReadAction.run(() -> {
                for (var extension : FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList()) {
                    FileBasedIndex.getInstance().ensureUpToDate(extension.getName(), project, null);
                }
            });
        }
    }

    private void clearUncommittedDocuments() {
        var documentManager = (PsiDocumentManagerImpl)PsiDocumentManager.getInstance(myProject);
        documentManager.clearUncommittedDocuments();

        var projectManager = ProjectManagerEx.getInstanceEx();
        if (projectManager.isDefaultProjectInitialized()) {
            ((PsiDocumentManagerImpl)PsiDocumentManager.getInstance(projectManager.getDefaultProject()))
                .clearUncommittedDocuments();
        }
    }

    private OpenProjectTask createOpenProjectOptions() {
        boolean forceOpenInNewFrame = true;
        Project projectToClose = null;
        boolean isNewProject = false;
        boolean useDefaultProjectAsTemplate = false;
        Project project = null;
        String projectName = null;
        boolean showWelcomeScreen = false;
        ProjectOpenedCallback callback = null;
        Object frameManager = null;
        int line = -1;
        int column = -1;
        boolean isRefreshVfsNeeded = false;
        boolean runConfigurators = false;
        boolean runConversionBeforeOpen = false;
        String projectWorkspaceId = null;
        boolean isProjectCreatedWithWizard = false;
        boolean preloadServices = true;
        Function1<? super Project, Unit> beforeInit = null;
        Function1<? super Project, Boolean> beforeOpen = null;
        Function1<? super Module, Unit> preparedToOpen = null;

        return new OpenProjectTask(forceOpenInNewFrame, projectToClose, isNewProject,
            useDefaultProjectAsTemplate, project, projectName,
            showWelcomeScreen, callback, frameManager, line,
            column, isRefreshVfsNeeded, runConfigurators, runConversionBeforeOpen,
            projectWorkspaceId, isProjectCreatedWithWizard, preloadServices,
            beforeInit, beforeOpen, preparedToOpen);
    }

    private void cleanPersistedVFSContent() {
        ((PersistentFSImpl)PersistentFS.getInstance()).cleanPersistedContents();
    }

    private void doCleanUp() {
        var project = myProject;
        if (project != null) {
            CodeStyle.dropTemporarySettings(project);
            var autoPopupController = project.getServiceIfCreated(AutoPopupController.class);
            if (autoPopupController != null) {
                autoPopupController.cancelAllRequests();
            }

            LookupManager.hideActiveLookup(project);
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            FileEditorManagerEx.getInstanceEx(project).closeAllFiles();
            EditorHistoryManager.getInstance(project).removeAllFiles();
            ((DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project)).cleanupAfterTest();
            ((ProjectRootManagerImpl)ProjectRootManager.getInstance(project)).clearScopesCachesForModules();
        }
        myEditorService.clearFileAndEditor();
    }

    private void waitForThreads() throws Exception {
        if (myProject instanceof ComponentManagerImpl) {
            ((ComponentManagerImpl)myProject).stopServicePreloading();
        }

        var fileChangeTracker = myProject.getServiceIfCreated(GeneratedSourceFileChangeTracker.class);
        if (fileChangeTracker instanceof GeneratedSourceFileChangeTrackerImpl) {
            ((GeneratedSourceFileChangeTrackerImpl)fileChangeTracker).cancelAllAndWait(10, TimeUnit.SECONDS);
        }
    }

    private void cleanUpProjectAndApp() throws Exception {
        var app = myApplicationService.getApplication();
        var project = myProject;

        var fileTypeManager = app.getServiceIfCreated(FileTypeManager.class);
        if (fileTypeManager instanceof FileTypeManagerImpl) {
            ((FileTypeManagerImpl)fileTypeManager).drainReDetectQueue();
        }

        if (project != null) {
            doPostponedFormatting();

            var startupManager = project.getServiceIfCreated(StartupManager.class);
            if (startupManager instanceof StartupManagerImpl) {
                ((StartupManagerImpl)startupManager).prepareForNextTest();
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                var fileDocumentManager = app.getServiceIfCreated(FileDocumentManager.class);
                if (fileDocumentManager instanceof FileDocumentManagerImpl) {
                    ((FileDocumentManagerImpl)fileDocumentManager).dropAllUnsavedDocuments();
                }
            });

            var editorHistoryManager = project.getServiceIfCreated(EditorHistoryManager.class);
            if (editorHistoryManager != null) {
                editorHistoryManager.removeAllFiles();
            }

            clearUncommittedDocuments();
        }

        var hintManager = app.getServiceIfCreated(HintManager.class);
        if (hintManager instanceof HintManagerImpl) {
            ((HintManagerImpl)hintManager).cleanup();
        }

        if (project != null) {
            ((UndoManagerImpl)UndoManager.getInstance(project)).dropHistoryInTests();

            var templateDataLanguageMappings = project.getServiceIfCreated(TemplateDataLanguageMappings.class);
            if (templateDataLanguageMappings != null) {
                templateDataLanguageMappings.cleanupForNextTest();
            }

            var psiManager = project.getServiceIfCreated(PsiManager.class);
            if (psiManager instanceof PsiManagerImpl) {
                ((PsiManagerImpl)psiManager).cleanupForNextTest();
            }

            var structureViewFactory = project.getServiceIfCreated(StructureViewFactory.class);
            if (structureViewFactory instanceof StructureViewFactoryImpl) {
                ((StructureViewFactoryImpl)structureViewFactory).cleanupForNextTest();
            }

            waitForThreads();
        }

        clearCompletion();
        releaseEditors();

        myApplicationService.setDataProvider(null);
        closeAndDeleteProject();

        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        UiInterceptors.clear();

        if (project != null) InjectedLanguageManagerImpl.checkInjectorsAreDisposed(project);

        var managingFS = app.getServiceIfCreated(ManagingFS.class);
        if (managingFS != null) {
            ((PersistentFS)managingFS).clearIdCache();
        }

        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();

        var projectManager = ProjectManagerEx.getInstanceExIfCreated();
        if (projectManager != null && projectManager.isDefaultProjectInitialized()) {
            var defaultProject = projectManager.getDefaultProject();
            ((PsiManagerImpl)PsiManager.getInstance(defaultProject)).cleanupForNextTest();
        }
    }

    private void doPostponedFormatting() {
        DocumentUtil.writeInRunUndoTransparentAction(() -> {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
            PostprocessReformattingAspect.getInstance(myProject).doPostponedFormatting();
        });
    }

    private void clearCompletion() {
        var completionService = myApplicationService.getApplication().getServiceIfCreated(CompletionService.class);
        if (!(completionService instanceof CompletionServiceImpl)) return;

        var currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
        if (currentCompletion != null && currentCompletion.getProject().equals(myProject)) {
            currentCompletion.closeAndFinish(true);
        }
    }

    private void releaseEditors() {
        UIUtil.dispatchAllInvocationEvents();
        var app = myApplicationService.getApplication();
        EditorFactory editorFactory = app == null ? null : app.getServiceIfCreated(EditorFactory.class);
        if (editorFactory == null) {
            return;
        }

        for (Editor editor : editorFactory.getAllEditors()) {
            if (editor.getProject() == null || editor.getProject().equals(myProject)) {
                editorFactory.releaseEditor(editor);
            }
        }
    }

}
