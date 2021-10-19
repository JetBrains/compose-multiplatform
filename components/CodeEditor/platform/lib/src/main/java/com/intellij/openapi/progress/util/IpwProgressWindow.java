// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Modified by Alex Hosh (n34to0@gmail.com) 2021.
package com.intellij.openapi.progress.util;

import com.intellij.ide.IpwEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.progress.impl.BlockingProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsContexts.ProgressDetails;
import com.intellij.openapi.util.NlsContexts.ProgressText;
import com.intellij.openapi.util.NlsContexts.ProgressTitle;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.EDT;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.AWTEvent;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class IpwProgressWindow extends ProgressIndicatorBase implements BlockingProgressIndicator, Disposable {
    private static final Logger LOG = Logger.getInstance(IpwProgressWindow.class);

    public static final int DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS = 300;

    @Nullable
    protected final Project myProject;
    final boolean myShouldShowCancel;
    @NlsContexts.Button String myCancelText;

    @ProgressTitle
    private String myTitle;

    private boolean myStoppedAlready;
    protected boolean myBackgrounded;
    int myDelayInMillis = DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS;
    private boolean myModalityEntered;

    @FunctionalInterface
    public interface Listener {
        void progressWindowCreated(@NotNull IpwProgressWindow pw);
    }

    @Topic.AppLevel
    public static final Topic<Listener> TOPIC = new Topic<>(Listener.class, Topic.BroadcastDirection.NONE, true);

    public IpwProgressWindow(boolean shouldShowCancel, @Nullable Project project) {
        this(shouldShowCancel, false, project);
    }

    public IpwProgressWindow(boolean shouldShowCancel, boolean shouldShowBackground, @Nullable Project project) {
        this(shouldShowCancel, shouldShowBackground, project, null);
    }

    public IpwProgressWindow(boolean shouldShowCancel, boolean shouldShowBackground, @Nullable Project project,
                             @Nullable @NlsContexts.Button String cancelText) {
        this(shouldShowCancel, shouldShowBackground, project, null, cancelText);
    }

    public IpwProgressWindow(boolean shouldShowCancel,
                             boolean shouldShowBackground,
                             @Nullable Project project,
                             @Nullable JComponent parentComponent,
                             @Nullable @NlsContexts.Button String cancelText) {
        myProject = project;
        myShouldShowCancel = shouldShowCancel;
        myCancelText = cancelText;

        if (myProject != null) {
            Disposer.register(myProject, this);
        }

        setModalityProgress(shouldShowBackground ? null : this);
        addStateDelegate(new MyDelegate());
        ApplicationManager.getApplication().getMessageBus().syncPublisher(TOPIC).progressWindowCreated(this);
    }

    @RequiresEdt
    protected void initializeOnEdtIfNeeded() {
        EDT.assertIsEdt();
    }

    @Override
    public void start() {
        synchronized (getLock()) {
            LOG.assertTrue(!isRunning());
            LOG.assertTrue(!myStoppedAlready);

            super.start();
        }
    }

    public void setDelayInMillis(int delayInMillis) {
        myDelayInMillis = delayInMillis;
    }

    protected void prepareShowDialog() {
    }

    final void enterModality() {
    }

    final void exitModality() {
    }

    @Deprecated
    public void startBlocking(@NotNull Runnable init) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Disposer.register(this, () -> future.complete(null));
        startBlocking(init, future);
    }

    @Override
    public void startBlocking(@NotNull Runnable init, @NotNull CompletableFuture<?> stopCondition) {
        EDT.assertIsEdt();
        synchronized (getLock()) {
            LOG.assertTrue(!isRunning());
            LOG.assertTrue(!myStoppedAlready);
        }

        init.run();

        ApplicationManagerEx.getApplicationEx().runUnlockingIntendedWrite(() -> {
            initializeOnEdtIfNeeded();
            // guarantee AWT event after the future is done will be pumped and loop exited
            stopCondition.thenRun(() -> IpwEventQueue.invokeLater(EmptyRunnable.INSTANCE));
            IpwEventQueue.getInstance().pumpEventsForHierarchy(null, stopCondition, null);
            return null;
        });
    }

    final boolean isCancellationEvent(@NotNull AWTEvent event) {
        return false;
    }

    protected void showDialog() {
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        super.setIndeterminate(indeterminate);
    }

    @Override
    public void stop() {
        synchronized (getLock()) {
            LOG.assertTrue(!myStoppedAlready);

            super.stop();

            UIUtil.invokeLaterIfNeeded(() -> {
                synchronized (getLock()) {
                    myStoppedAlready = true;
                }

                Disposer.dispose(this);
            });

            IpwEventQueue.invokeLater(EmptyRunnable.INSTANCE); // Just to give blocking dispatching a chance to go out.
        }
    }

    public void background() {

    }

    @Override
    public void setText(@ProgressText String text) {
        if (!Objects.equals(text, getText())) {
            super.setText(text);
        }
    }

    @Override
    public void setFraction(double fraction) {
        if (fraction != getFraction()) {
            super.setFraction(fraction);
        }
    }

    @Override
    public void setText2(@ProgressDetails String text) {
        if (!Objects.equals(text, getText2())) {
            super.setText2(text);
        }
    }

    public void setTitle(@ProgressTitle String title) {
        if (!Objects.equals(title, myTitle)) {
            myTitle = title;
        }
    }

    @ProgressTitle
    public String getTitle() {
        return myTitle;
    }

    public void setCancelButtonText(@NlsContexts.Button @NotNull String text) {
        myCancelText = text;
    }

    IdeFocusManager getFocusManager() {
        return IdeFocusManager.getInstance(myProject);
    }

    @Override
    public void dispose() {
        EDT.assertIsEdt();
        if (isRunning()) {
            cancel();
        }
    }

    @Override
    public boolean isPopupWasShown() {
        return false;
    }

    @Override
    public String toString() {
        return getTitle() + " " + System.identityHashCode(this) + ": running=" + isRunning() + "; canceled="
            + isCanceled();
    }

    private final class MyDelegate extends AbstractProgressIndicatorBase implements ProgressIndicatorEx {
        @Override
        public void cancel() {
            super.cancel();
        }

        @Override
        public void checkCanceled() {
            super.checkCanceled();
        }

        @Override
        public void addStateDelegate(@NotNull ProgressIndicatorEx delegate) {
            throw new IncorrectOperationException();
        }

        @Override
        public void finish(@NotNull TaskInfo task) {
        }

        @Override
        public boolean isFinished(@NotNull TaskInfo task) {
            return true;
        }

        @Override
        public boolean wasStarted() {
            return false;
        }

        @Override
        public void processFinish() {
        }
    }
}
