package org.jetbrains.compose.codeeditor.platform.impl.edt;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Ref;
import com.intellij.util.ThrowableRunnable;

import java.lang.reflect.InvocationTargetException;

public final class EdtUtil {

    private EdtUtil() {}

    public static <T extends Throwable> void runInEdtAndWait(ThrowableRunnable<T> runnable) throws T {
        var app = ApplicationManager.getApplication();
        if (app != null ? app.isDispatchThread() : EdtAdapter.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        Ref<T> exception = new Ref<>();
        Runnable r = () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                exception.set((T)e);
            }
        };

        if (app != null) {
            app.invokeAndWait(r);
        } else {
            try {
                EdtAdapter.invokeAndWait(r);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        if (!exception.isNull()) {
            throw exception.get();
        }
    }

}
