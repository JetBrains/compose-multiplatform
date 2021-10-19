package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.edt.EdtUtil;
import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import org.jetbrains.compose.codeeditor.platform.impl.services.FileService;
import org.jetbrains.compose.codeeditor.platform.impl.services.ModuleService;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.EmptyModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexImpl;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.IndexableFileSet;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

abstract class AbstractModuleService extends IpwService implements ModuleService {

    private ApplicationService myApplicationService;
    protected FileService myFileService;
    protected Module myModule;
    protected String myModuleName;
    private String myModuleLibName;
    protected Path myProjectFolder;
    protected VirtualFile mySourceRoot;
    private boolean isDisposed;

    protected AbstractModuleService(ApplicationService applicationService, FileService fileService) {
        myApplicationService = applicationService;
        myFileService = fileService;
    }

    @Override
    protected void doInit() {}

    @Override
    public void dispose() {
        if (isDisposed) return;
        EdtUtil.runInEdtAndWait(() -> {
            deleteModuleFiles();
            myModule = null;
            mySourceRoot = null;
        });
        isDisposed = true;
    }

    @Override
    public void createModule(Project project, Path projectFolder) {
        myProjectFolder = projectFolder;
        myModuleName = project.getName() + "_mdl";
        myModuleLibName = myModuleName + "_lib";
        Path moduleFile = getModuleFilePath();
        try {
            Files.deleteIfExists(moduleFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        myModule = WriteAction.compute(() ->
            ModuleManager.getInstance(project).newModule(moduleFile, EmptyModuleType.EMPTY_MODULE));

        registerSourceRoot(project);
        createContentEntry(project);
    }

    protected abstract Path getModuleFilePath();

    @Override
    public void closeModule() {
        Disposer.dispose(this);
    }

    protected abstract void deleteModuleFiles();

    @Override
    public Module getModule() {
        return myModule;
    }

    @Override
    public void addLibraries(List<String> paths) {
        List<VirtualFile> jars = getLibJars(paths);
        WriteAction.runAndWait(() -> {
            var model = ModuleRootManager.getInstance(myModule).getModifiableModel();
            var libraryTable = model.getModuleLibraryTable();
            try {
                var library = libraryTable.getLibraryByName(myModuleLibName);
                if (library == null) {
                    library = libraryTable.createLibrary(myModuleLibName);
                }
                var libraryModel = library.getModifiableModel();
                jars.forEach(jar -> libraryModel.addRoot(jar, OrderRootType.CLASSES));
                libraryModel.commit();
                model.commit();
            } finally {
                if (!model.isDisposed()) model.dispose();
            }
            invalidateKeyHashToVirtualFileMappingCacheOfClassShortNamesIndex(FileBasedIndex.getFileId(jars.get(0)));
        });
    }

    private void invalidateKeyHashToVirtualFileMappingCacheOfClassShortNamesIndex(int fileId) {
        ((StubIndexImpl)StubIndex.getInstance()).updateIndex(
            JavaStubIndexKeys.CLASS_SHORT_NAMES,
            fileId,
            Collections.singleton("$$$$_$$$$"),
            Collections.emptySet()
        );
    }

    private List<VirtualFile> getLibJars(List<String> paths) {
        List<VirtualFile> jars = new ArrayList<>();
        paths.forEach(
            path -> extractLibJarsFromPath(jars, LocalFileSystem.getInstance().refreshAndFindFileByPath(path))
        );
        return jars;
    }

    private void extractLibJarsFromPath(List<VirtualFile> jars, VirtualFile vFile) {
        if (vFile != null) {
            if (vFile.isDirectory()) {
                Arrays.stream(vFile.getChildren()).forEach(child -> extractLibJarsFromPath(jars, child));
            } else {
                var jar = JarFileSystem.getInstance().refreshAndFindFileByPath(vFile.getPath() + "!/");
                if (jar != null) {
                    vFile = jar;
                }
                jars.add(vFile);
            }
        }
    }

    @Override
    public VirtualFile getSourceRoot() {
        return mySourceRoot;
    }

    @Override
    public void synchronizeSourceRoot() {
        myFileService.synchronizeDirVfs(mySourceRoot);
    }

    protected VirtualFile findFileInSource(String path) {
        return myFileService.getFile(mySourceRoot, path);
    }

    private void registerSourceRoot(Project project) {
        var srcRoot = defineSourceRoot();
        IndexableFileSet indexableFileSet = file -> file.getFileSystem() == srcRoot.getFileSystem() && project.isOpen();
        var fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
        fileBasedIndex.registerIndexableSet(indexableFileSet, project);
        Disposer.register(project, () -> fileBasedIndex.removeIndexableSet(indexableFileSet));
        mySourceRoot = srcRoot;
    }

    protected abstract VirtualFile defineSourceRoot();

    private void createContentEntry(Project project) {
        ModuleRootModificationUtil.updateModel(myModule, model -> {
            Sdk sdk = Objects.requireNonNull(myApplicationService.getSdk(),
                "JDK not found. Set JAVA_HOME or ipw.jdk property");
            model.setSdk(sdk);

            ContentEntry contentEntry = model.addContentEntry(mySourceRoot);
            contentEntry.addSourceFolder(mySourceRoot, JavaSourceRootType.SOURCE);
            configureModule(model);
        });
    }

    private void configureModule(ModifiableRootModel model) {
        var extension = model.getModuleExtension(LanguageLevelModuleExtension.class);
        if (extension != null) {
            var sdk = Objects.requireNonNull(model.getSdk());
            var version = JavaSdk.getInstance().getVersion(sdk);
            if (version != null) {
                extension.setLanguageLevel(version.getMaxLanguageLevel());
            }
        }
    }

}
