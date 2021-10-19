package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.edt.EdtUtil;
import org.jetbrains.compose.codeeditor.platform.impl.edt.EdtAdapter;
import org.jetbrains.compose.codeeditor.platform.impl.services.ApplicationService;
import com.intellij.BundleBase;
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.intellij.concurrency.IdeaForkJoinWorkerThreadFactory;
import com.intellij.diagnostic.StartUpMeasurer;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.execution.process.ProcessIOExecutorService;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.IpwEventQueue;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.HeadlessDataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.ApplicationLoader;
import com.intellij.idea.Main;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingManagerImpl;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.testFramework.TestModeFlags;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexEx;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.EdtInvocationManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ApplicationServiceImpl extends IpwService implements ApplicationService {
    private static final boolean IS_STRESS_TEST = true;
    private static final boolean IS_UNIT_TEST = true;
    private static final String IPW_JDK = "ipw.jdk";

    private Sdk mySdk;

    @Override
    protected void doInit() {
        setEdtInvocationManager();

        Main.setHeadlessInTestMode(true);
        IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool(true);
        // stop the startup measuring
        StartUpMeasurer.disable();
        StartUpMeasurer.stopPluginCostMeasurement();

        if (IS_STRESS_TEST) {
            // it might add a bit of performance
            ApplicationInfoImpl.setInStressTest(true);
        }
        // exclude errors about missing plugins
        PluginManagerCore.isUnitTestMode = true;
        EdtUtil.runInEdtAndWait(this::runAppAndLoadPluginsAndServices);

        // fix the returning of an incomplete result by code completion
        TestModeFlags.set(CompletionAutoPopupHandler.ourTestingAutopopup, true);
        CompletionProgressIndicator.setGroupingTimeSpan(3600 * 1000);
        CodeCompletionHandlerBase.setAutoInsertTimeout(3600 * 1000);

        DaemonCodeAnalyzerSettings.getInstance().setImportHintEnabled(false);
        EdtUtil.runInEdtAndWait(this::registerSdk);
    }

    @Override
    public void dispose() {
        try {
            EdtUtil.runInEdtAndWait(() -> {
                var app = (ApplicationImpl)ApplicationManager.getApplication();
                var encodingManager = app.getServiceIfCreated(EncodingManager.class);
                if (encodingManager instanceof EncodingManagerImpl) {
                    ((EncodingManagerImpl)encodingManager).clearDocumentQueue();
                }
                IpwUtil.cleanupSwingDataStructures();
                UIUtil.dispatchAllInvocationEvents();
                sendClosingMessage(app);
                var fileBasedIndex = app.getServiceIfCreated(FileBasedIndex.class);
                if (fileBasedIndex instanceof FileBasedIndexEx) {
                    ((FileBasedIndexEx)fileBasedIndex).waitUntilIndicesAreInitialized();
                }
                IpwUtil.appWaitForThreads();
                app.invokeAndWait(app::disposeContainer);
                ProcessIOExecutorService.INSTANCE.shutdownNow();
                UIUtil.dispatchAllInvocationEvents();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Application getApplication() {
        return ApplicationManager.getApplication();
    }

    @Override
    public Sdk getSdk() {
        return mySdk;
    }

    private void registerSdk() {
        String jdkPath = System.getProperty(IPW_JDK);
        mySdk = jdkPath != null
            ? ExternalSystemJdkProvider.getInstance().createJdk(null, jdkPath)
            : ExternalSystemJdkUtil.resolveJdkName(null, ExternalSystemJdkUtil.USE_JAVA_HOME);
        getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().addJdk(mySdk, this));
    }

    @Override
    public void setDataProvider(DataProvider provider) {
        getDataManager().setTestDataProvider(provider);
    }

    @Override
    public void dispatchAllInvocationEvents() {
        EdtAdapter.dispatchAllInvocationEvents();
    }

    private void runAppAndLoadPluginsAndServices() {
        var loadedPluginFuture = CompletableFuture.supplyAsync(
            () -> PluginManagerCore.getLoadedPlugins(PathManager.class.getClassLoader()),
            AppExecutorUtil.getAppExecutorService());

        var app = new ApplicationImpl(false, IS_UNIT_TEST, true, true);

        // disable error messages about missing keys
        BundleBase.assertOnMissedKeys(false);

        try {
            List<IdeaPluginDescriptorImpl> plugins = ApplicationLoader.registerRegistryAndInitStore(
                ApplicationLoader.registerAppComponents(loadedPluginFuture, app),
                app
            ).get(40, TimeUnit.SECONDS);

            Executor boundedExecutor = ApplicationLoader.createExecutorToPreloadServices();

            Registry.getInstance().markAsLoaded();
            var preloadedServiceFuture =
                ApplicationLoader.preloadServices(plugins, app, "", false, boundedExecutor);
            app.loadComponents(null);

            preloadedServiceFuture
                .thenCompose(v -> ApplicationLoader.callAppInitialized(app, boundedExecutor))
                .get(40, TimeUnit.SECONDS);

            ((PersistentFSImpl)PersistentFS.getInstance()).cleanPersistedContents();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(Objects.requireNonNullElse(e.getCause(), e));
        } catch (TimeoutException e) {
            throw new RuntimeException("Cannot preload services in 40 seconds: "
                + ThreadDumper.dumpThreadsToString(), e);
        }
    }

    private HeadlessDataManager getDataManager() {
        return ((HeadlessDataManager)DataManager.getInstance());
    }

    private void setEdtInvocationManager() {
        EdtInvocationManager.setEdtInvocationManager(new EdtInvocationManager() {
            @Override
            public boolean isEventDispatchThread() {
                return IpwEventQueue.isDispatchThread();
            }

            @Override
            public void invokeLater(@NotNull Runnable task) {
                IpwEventQueue.invokeLater(task);
            }

            @Override
            public void invokeAndWait(@NotNull Runnable task) throws InvocationTargetException, InterruptedException {
                IpwEventQueue.invokeAndWait(task);
            }
        });
    }

    private void sendClosingMessage(ApplicationImpl app) {
        MessageBus messageBus = app.getMessageBus();
        if (messageBus != null) {
            var appLifecycleListener = messageBus.syncPublisher(AppLifecycleListener.TOPIC);
            if (appLifecycleListener != null) {
                appLifecycleListener.appWillBeClosed(false);
            }
        }
    }
}
