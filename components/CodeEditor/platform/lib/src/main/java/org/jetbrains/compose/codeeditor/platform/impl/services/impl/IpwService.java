package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import org.jetbrains.compose.codeeditor.platform.impl.services.Service;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;

public abstract class IpwService implements Service {

    private volatile boolean isInitialized;
    private final Object lockObject = new Object();

    @Override
    public void init(Disposable parentDisposable) {
        if (isInitialized) return;
        synchronized (lockObject) {
            if (isInitialized) return;
            Disposer.register(parentDisposable, this);
            doInit();
            isInitialized = true;
        }
    }

    protected abstract void doInit();

}
