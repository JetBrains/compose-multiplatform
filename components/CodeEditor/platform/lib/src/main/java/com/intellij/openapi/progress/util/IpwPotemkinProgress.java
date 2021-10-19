// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Modified by Alex Hosh (n34to0@gmail.com) 2021.
package com.intellij.openapi.progress.util;

import com.intellij.ide.IpwEventQueue;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.Semaphore;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.InvocationEvent;

public final class IpwPotemkinProgress extends IpwProgressWindow implements PingProgress {
    private final Application myApp = ApplicationManager.getApplication();

    public IpwPotemkinProgress(@NotNull @NlsContexts.ProgressTitle String title, @Nullable Project project, @Nullable JComponent parentComponent,
                            @Nullable @Nls(capitalization = Nls.Capitalization.Title) String cancelText) {
        super(cancelText != null,false, project, parentComponent, cancelText);
        setTitle(title);
        myApp.assertIsDispatchThread();
    }

    @Override
    public void interact() {
    }

    public void runInSwingThread(@NotNull Runnable action) {
        myApp.assertIsDispatchThread();
        try {
            ProgressManager.getInstance().runProcess(action, this);
        }
        catch (ProcessCanceledException ignore) {
        }
    }

    public void runInBackground(@NotNull Runnable action) {
        myApp.assertIsDispatchThread();
        enterModality();

        try {
            ensureBackgroundThreadStarted(action);

            while (isRunning()) {
            }
        }
        finally {
            exitModality();
        }
    }

    private void ensureBackgroundThreadStarted(@NotNull Runnable action) {
        Semaphore started = new Semaphore();
        started.down();
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            ProgressManager.getInstance().runProcess(() -> {
                started.up();
                action.run();
            }, this);
        });

        started.waitFor();
    }

    public static void invokeLaterNotBlocking(Object source, Runnable runnable) {
        IpwEventQueue.getInstance().postEvent(new InvocationEvent(source, runnable));
    }

}
