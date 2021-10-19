package org.jetbrains.compose.codeeditor.platform.impl.edt;

import com.intellij.ide.IpwEventQueue;

import java.lang.reflect.InvocationTargetException;

public class EdtAdapter {

    private EdtAdapter() {}

    @SuppressWarnings("unused")
    public static boolean isEventDispatchThread() {
        return IpwEventQueue.isDispatchThread();
    }

    @SuppressWarnings("unused")
    public static void invokeLater(Runnable runnable) {
        IpwEventQueue.invokeLater(runnable);
    }

    @SuppressWarnings("unused")
    public static void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
        IpwEventQueue.invokeAndWait(runnable);
    }

    @SuppressWarnings("unused")
    public static void dispatchAllInvocationEvents() {
        IpwEventQueue.dispatchAllInvocationEventsInIdeEventQueue();
    }

    @SuppressWarnings("unused")
    public static Thread getEventQueueThread() {
        return IpwEventQueue.getInstance().getDispatchThread();
    }

    @SuppressWarnings("unused")
    public static void invokeLaterIfNeeded(Runnable runnable) {
        if (IpwEventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            IpwEventQueue.invokeLater(runnable);
        }
    }

    @SuppressWarnings("unused")
    public static void invokeAndWaitIfNeeded(Runnable runnable) {
        if (IpwEventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                IpwEventQueue.invokeAndWait(runnable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
