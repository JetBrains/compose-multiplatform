// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Modified by Alex Hosh (n34to0@gmail.com) 2021.
package com.intellij.ide;

import com.intellij.codeWithMe.ClientId;
import com.intellij.diagnostic.LoadingState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EDT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.intellij.openapi.application.impl.InvocationUtil.FLUSH_NOW_CLASS;
import static com.intellij.openapi.application.impl.InvocationUtil.REPAINT_PROCESSING_CLASS;
import static com.intellij.openapi.application.impl.InvocationUtil.extractRunnable;

public final class IpwEventQueue {
    private static final Logger LOG = Logger.getInstance(IpwEventQueue.class);

    private final BlockingQueue<AWTEvent> queue;
    private final Thread edtThread;

    private static final Set<Class<? extends Runnable>> ourRunnablesWoWrite = Set.of(REPAINT_PROCESSING_CLASS);
    private static final Set<Class<? extends Runnable>> ourRunnablesWithWrite = Set.of(FLUSH_NOW_CLASS);
    private static final boolean ourDefaultEventWithWrite = true;

    private static TransactionGuardImpl ourTransactionGuard;
    private static ProgressManager ourProgressManager;

    private int myEventCount;
    @NotNull
    private AWTEvent myCurrentEvent = new InvocationEvent(this, EmptyRunnable.getInstance());
    @Nullable
    private AWTEvent myCurrentSequencedEvent;
    private volatile long myLastActiveTime = System.nanoTime();
    private final List<EventDispatcher> myPostProcessors = ContainerUtil.createLockFreeCopyOnWriteList();

    private static final class IpwEventQueueHolder {
        private static final IpwEventQueue INSTANCE = new IpwEventQueue();
    }

    public static IpwEventQueue getInstance() {
        return IpwEventQueueHolder.INSTANCE;
    }

    public static boolean isDispatchThread() {
        return Thread.currentThread() == getInstance().edtThread;
    }

    public static void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
        if (isDispatchThread()) {
            throw new Error("Cannot call invokeAndWait from the event dispatcher thread");
        }

        Object lock = new Object();
        InvocationEvent event = new InvocationEvent(IpwEventQueue.class, runnable, lock, true);

        synchronized (lock) {
            getInstance().postEvent(event);
            while (!event.isDispatched()) {
                lock.wait();
            }
        }

        var eventThrowable = event.getThrowable();
        if (eventThrowable != null) {
            throw new InvocationTargetException(eventThrowable);
        }
    }

    public static void dispatchAllInvocationEventsInIdeEventQueue() {
        IpwEventQueue eventQueue = getInstance();
        while (true) {
            var event = eventQueue.peekEvent();
            if (event == null) break;
            try {
                event = eventQueue.getNextEvent();
                if (event instanceof InvocationEvent) {
                    eventQueue.dispatchEvent(event);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void invokeLater(Runnable runnable) {
        getInstance().postEvent(new InvocationEvent(IpwEventQueue.class, runnable));
    }

    private IpwEventQueue() {
        queue = new LinkedBlockingQueue<>();
        edtThread = new Thread("IpwEdt-Thread") {
            @Override
            public void run() {
                EDT.updateEdt();
                while (!isInterrupted()) {
                    try {
                        var event = queue.take();
                        dispatchEvent(event);
                    } catch (InterruptedException ignored) {}
                }
            }
        };
        edtThread.setDaemon(true);
        edtThread.start();
    }

    public Thread getDispatchThread() {
        return edtThread;
    }

    private static boolean ourAppIsLoaded;

    private static boolean appIsLoaded() {
        if (ourAppIsLoaded) {
            return true;
        }

        if (LoadingState.COMPONENTS_LOADED.isOccurred()) {
            ourAppIsLoaded = true;
            return true;
        }
        return ourAppIsLoaded;
    }

    // used for GuiTests to stop IdeEventQueue when application is disposed already
    @SuppressWarnings("unused")
    public static void applicationClose() {
        ourAppIsLoaded = false;
    }

    @SuppressWarnings("unused") // todo: @Override
    public void postEvent(@NotNull AWTEvent event) {
        doPostEvent(event);
    }

    // todo: @Override
    public void dispatchEvent(@NotNull AWTEvent e) {
        fixNestedSequenceEvent(e);
        // Add code below if you need

        // Update EDT if it changes (might happen after Application disposal)
        EDT.updateEdt();

        if (!appIsLoaded()) {
            try {
                __dispatchEvent(e);
            } catch (Throwable t) {
                processException(t);
            }
            return;
        }

        if (isInputEvent(e) || isFocusEvent(e)) {
            throw new RuntimeException("Invalid event: " + e);
        }

        AWTEvent oldEvent = myCurrentEvent;
        myCurrentEvent = e;

        AWTEvent finalE1 = e;
        Runnable runnable = extractRunnable(e);
        Class<? extends Runnable> runnableClass = runnable != null ? runnable.getClass() : Runnable.class;
        Runnable processEventRunnable = () -> {
            try (AccessToken ignored = startActivity(finalE1)) {
                ProgressManager progressManager = obtainProgressManager();
                if (progressManager != null) {
                    progressManager.computePrioritized(() -> {
                        _dispatchEvent(myCurrentEvent);
                        return null;
                    });
                } else {
                    _dispatchEvent(myCurrentEvent);
                }
            } catch (Throwable t) {
                processException(t);
            } finally {
                myCurrentEvent = oldEvent;

                if (myCurrentSequencedEvent == finalE1) {
                    myCurrentSequencedEvent = null;
                }

                for (EventDispatcher each : myPostProcessors) {
                    each.dispatch(finalE1);
                }

            }
        };

        if (runnableClass != Runnable.class) {
            if (ourRunnablesWoWrite.contains(runnableClass)) {
                processEventRunnable.run();
                return;
            }
            if (ourRunnablesWithWrite.contains(runnableClass)) {
                ApplicationManagerEx.getApplicationEx().runIntendedWriteActionOnCurrentThread(processEventRunnable);
                return;
            }
        }

        if (ourDefaultEventWithWrite) {
            ApplicationManagerEx.getApplicationEx().runIntendedWriteActionOnCurrentThread(processEventRunnable);
        } else {
            processEventRunnable.run();
        }
    }

    @SuppressWarnings("unused")
    public @NotNull
    AWTEvent getNextEvent() throws InterruptedException {
        AWTEvent event = appIsLoaded() ?
            ApplicationManagerEx.getApplicationEx().runUnlockingIntendedWrite(() -> _getNextEvent()) :
            _getNextEvent();
        return event;
    }

    private AWTEvent _getNextEvent() throws InterruptedException {
        return queue.take();
    }

    @SuppressWarnings("unused") // todo:
    public void flushQueue() {
        while (true) {
            AWTEvent event = peekEvent();
            if (event == null) return;
            try {
                dispatchEvent(getNextEvent());
            } catch (Exception e) {
                LOG.error(e); //?
            }
        }
    }

    // todo: @Override
    public AWTEvent peekEvent() {
        AWTEvent event = _peekEvent();
        if (event != null) {
            return event;
        }
        if (isTestMode() && LaterInvocator.ensureFlushRequested()) {
            return _peekEvent();
        }
        return null;
    }

    private AWTEvent _peekEvent() {
        return queue.peek();
    }

    // return true if posted, false if consumed immediately
    boolean doPostEvent(@NotNull AWTEvent event) {
        if (event instanceof InvocationEvent && !ClientId.isCurrentlyUnderLocalId() && ClientId.Companion
            .getPropagateAcrossThreads()) {
            // only do wrapping trickery with non-local events to preserve correct behaviour - local events will get dispatched under local ID anyways
            ClientId clientId = ClientId.getCurrent();
            _postEvent(new InvocationEvent(event.getSource(), () -> ClientId.withClientId(clientId, () -> {
                dispatchEvent(event);
            })));
            return true;
        }

        if (event instanceof KeyEvent) {
            throw new RuntimeException("KeyEvent: " + event);
        }

        if (isFocusEvent(event)) {
            throw new RuntimeException("FocusEvent: " + event);
        }

        _postEvent(event);

        return true;
    }

    private void _postEvent(AWTEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface EventDispatcher {
        boolean dispatch(@NotNull AWTEvent e);

    }

    @SuppressWarnings("unused")
    public void addPostprocessor(@NotNull EventDispatcher dispatcher, @Nullable Disposable parent) {
        LOG.warn("addPostprocessor");
        _addProcessor(dispatcher, parent, myPostProcessors);
    }

    @SuppressWarnings("unused")
    public void removePostprocessor(@NotNull EventDispatcher dispatcher) {
        LOG.warn("removePostprocessor");
        myPostProcessors.remove(dispatcher);
    }

    private static void _addProcessor(@NotNull EventDispatcher dispatcher,
                                      Disposable parent,
                                      @NotNull Collection<? super EventDispatcher> set) {
        set.add(dispatcher);
        if (parent != null) {
            Disposer.register(parent, () -> set.remove(dispatcher));
        }
    }

    @SuppressWarnings("unused")
    public int getEventCount() {
        LOG.warn("getEventCount");
        return myEventCount;
    }

    @SuppressWarnings("unused")
    public void setEventCount(int evCount) {
        LOG.warn("setEventCount");
        myEventCount = evCount;
    }

    @SuppressWarnings("unused")
    public long getIdleTime() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - myLastActiveTime);
    }

    // Fixes IDEA-218430: nested sequence events cause deadlock
    private void fixNestedSequenceEvent(@NotNull AWTEvent e) {
        if (e.getClass() == SequencedEventNestedFieldHolder.SEQUENCED_EVENT_CLASS) {
            if (myCurrentSequencedEvent != null) {
                AWTEvent sequenceEventToDispose = myCurrentSequencedEvent;
                myCurrentSequencedEvent = null; // Set to null BEFORE dispose b/c `dispose` can dispatch events internally
                SequencedEventNestedFieldHolder.invokeDispose(sequenceEventToDispose);
            }
            myCurrentSequencedEvent = e;
        }
    }

    @Nullable
    private static ProgressManager obtainProgressManager() {
        /*ProgressManager manager = ourProgressManager;
        if (manager == null) {
            Application app = ApplicationManager.getApplication();
            if (app != null && !app.isDisposed()) {
                ourProgressManager = manager = ApplicationManager.getApplication().getService(ProgressManager.class);
            }
        }
        return manager;*/
        return null;
    }

    private static boolean isInputEvent(@NotNull AWTEvent e) {
        return e instanceof InputEvent || e instanceof InputMethodEvent || e instanceof WindowEvent
            || e instanceof ActionEvent;
    }

    static @Nullable AccessToken startActivity(@NotNull AWTEvent e) {
        if (ourTransactionGuard == null && appIsLoaded()) {
            Application app = ApplicationManager.getApplication();
            if (app != null && !app.isDisposed()) {
                ourTransactionGuard = (TransactionGuardImpl)TransactionGuard.getInstance();
            }
        }
        return ourTransactionGuard == null
            ? null
            : ourTransactionGuard.startActivity(isInputEvent(e) || e instanceof ItemEvent || e instanceof FocusEvent);
    }

    private void processException(@NotNull Throwable t) {
        ExceptionUtil.rethrow(t);
    }

    private void _dispatchEvent(@NotNull AWTEvent e) {
        myEventCount++;
        defaultDispatchEvent(e);
    }

    private void defaultDispatchEvent(@NotNull AWTEvent e) {
        try {
            __dispatchEvent(e);
        } catch (Throwable t) {
            processException(t);
        }
    }

    private void __dispatchEvent(AWTEvent e) {
        ((InvocationEvent)e).dispatch();
    }

    @SuppressWarnings("unused")
    public void pumpEventsForHierarchy(Component modalComponent, @NotNull Future<?> exitCondition,
                                       Predicate<? super AWTEvent> isCancelEvent) {
        while (!exitCondition.isDone()) {
            try {
                AWTEvent event = getNextEvent();
                dispatchEvent(event);
            } catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    private static final class SequencedEventNestedFieldHolder {
        private static final Field NESTED_FIELD;
        private static final Method DISPOSE_METHOD;

        private static final Class<?> SEQUENCED_EVENT_CLASS;

        private static void invokeDispose(AWTEvent event) {
            try {
                DISPOSE_METHOD.invoke(event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        static {
            try {
                SEQUENCED_EVENT_CLASS = Class.forName("java.awt.SequencedEvent");
                NESTED_FIELD = ReflectionUtil.getDeclaredField(SEQUENCED_EVENT_CLASS, "nested");
                DISPOSE_METHOD = ReflectionUtil.getDeclaredMethod(SEQUENCED_EVENT_CLASS, "dispose");
                if (NESTED_FIELD == null) throw new RuntimeException();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static boolean isFocusEvent(@NotNull AWTEvent e) {
        return
            e.getID() == FocusEvent.FOCUS_GAINED ||
                e.getID() == FocusEvent.FOCUS_LOST ||
                e.getID() == WindowEvent.WINDOW_ACTIVATED ||
                e.getID() == WindowEvent.WINDOW_DEACTIVATED ||
                e.getID() == WindowEvent.WINDOW_LOST_FOCUS ||
                e.getID() == WindowEvent.WINDOW_GAINED_FOCUS;
    }

    private Boolean myTestMode;

    private boolean isTestMode() {
        Boolean testMode = myTestMode;
        if (testMode != null) return testMode;

        Application application = ApplicationManager.getApplication();
        if (application == null) return false;

        testMode = application.isUnitTestMode();
        myTestMode = testMode;
        return testMode;
    }

    public enum BlockMode {
        COMPLETE, ACTIONS
    }

    @FunctionalInterface
    public interface PostEventHook extends EventListener {
        boolean consumePostedEvent(@NotNull AWTEvent event);

    }

    @SuppressWarnings("unused")
    public void addPostEventListener(@NotNull PostEventHook listener, @NotNull Disposable parentDisposable) {
        throw new RuntimeException("addPostEventListener");
    }

    @SuppressWarnings("unused")
    @NotNull
    public String runnablesWaitingForFocusChangeState() {
        throw new RuntimeException("runnablesWaitingForFocusChangeState");
    }

    @SuppressWarnings("unused")
    public void executeWhenAllFocusEventsLeftTheQueue(@NotNull Runnable runnable) {
        throw new RuntimeException("executeWhenAllFocusEventsLeftTheQueue");
    }

    @SuppressWarnings("unused")
    public void setWindowManager(@NotNull WindowManagerEx windowManager) {
        throw new RuntimeException("setWindowManager");
    }

    @SuppressWarnings("unused")
    public void addIdleListener(@NotNull final Runnable runnable, final int timeoutMillis) {
        throw new RuntimeException("addIdleListener");
    }

    @SuppressWarnings("unused")
    public void removeIdleListener(@NotNull final Runnable runnable) {
        throw new RuntimeException("removeIdleListener");
    }

    @SuppressWarnings("unused")
    public void addActivityListener(@NotNull Runnable runnable, @NotNull Disposable parentDisposable) {
        throw new RuntimeException("addActivityListener");
    }

    @SuppressWarnings("unused")
    public void addDispatcher(@NotNull EventDispatcher dispatcher, Disposable parent) {
        throw new RuntimeException("addDispatcher");
    }

    @SuppressWarnings("unused")
    public void removeDispatcher(@NotNull EventDispatcher dispatcher) {
        throw new RuntimeException("removeDispatcher");
    }

    @SuppressWarnings("unused")
    public boolean containsDispatcher(@NotNull EventDispatcher dispatcher) {
        throw new RuntimeException("containsDispatcher");
    }

    @SuppressWarnings("unused")
    public @NotNull
    AWTEvent getTrueCurrentEvent() {
        throw new RuntimeException("getTrueCurrentEvent");
    }

    @SuppressWarnings("unused")
    public void onActionInvoked(@NotNull KeyEvent e) {
        throw new RuntimeException("onActionInvoked");
    }

    @SuppressWarnings("unused")
    public IdePopupManager getPopupManager() {
        throw new RuntimeException("getPopupManager");
    }

    @SuppressWarnings("unused")
    public IdeKeyEventDispatcher getKeyEventDispatcher() {
        throw new RuntimeException("IdeKeyEventDispatcher");
    }

    @SuppressWarnings("unused")
    public void blockNextEvents(@NotNull MouseEvent e) {
        throw new RuntimeException("blockNextEvents");
    }

    @SuppressWarnings("unused")
    public void blockNextEvents(@NotNull MouseEvent e, @NotNull BlockMode blockMode) {
        throw new RuntimeException("blockNextEvents");
    }

    @SuppressWarnings("unused")
    public void maybeReady() {
        throw new RuntimeException("maybeReady");
    }

    @SuppressWarnings("unused")
    public void doWhenReady(@NotNull Runnable runnable) {
        throw new RuntimeException("doWhenReady");
    }

    @SuppressWarnings("unused")
    public boolean isPopupActive() {
        throw new RuntimeException("isPopupActive");
    }

    @SuppressWarnings("unused")
    public boolean isInputMethodEnabled() {
        throw new RuntimeException("isInputMethodEnabled");
    }

    @SuppressWarnings("unused")
    public void disableInputMethods(@NotNull Disposable parentDisposable) {
        throw new RuntimeException("ERROR");
    }
}
