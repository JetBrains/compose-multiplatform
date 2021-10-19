package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.edt.EdtUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.impl.DocumentCommitProcessor;
import com.intellij.psi.impl.DocumentCommitThread;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class IpwUtil {

    private IpwUtil() {}

    static void appWaitForThreads() {
        try {
            EdtUtil.runInEdtAndWait(() -> {
                var app = ApplicationManager.getApplication();
                if (app != null && !app.isDisposed()) {
                    var index = app.getServiceIfCreated(FileBasedIndex.class);
                    if (index instanceof FileBasedIndexImpl) {
                        ((FileBasedIndexImpl)index).getChangedFilesCollector()
                                                   .waitForVfsEventsExecuted(10, TimeUnit.SECONDS);
                    }

                    var commitThread = ((DocumentCommitThread)app.getServiceIfCreated(DocumentCommitProcessor.class));
                    if (commitThread != null) {
                        commitThread.waitForAllCommits(10, TimeUnit.SECONDS);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void cleanupSwingDataStructures() throws IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Object manager = ReflectionUtil.getDeclaredMethod(Class.forName("javax.swing.KeyboardManager"), "getCurrentManager").invoke(null);
        Map<?, ?> componentKeyStrokeMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "componentKeyStrokeMap");
        componentKeyStrokeMap.clear();
        Map<?, ?> containerMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "containerMap");
        containerMap.clear();
    }

}
