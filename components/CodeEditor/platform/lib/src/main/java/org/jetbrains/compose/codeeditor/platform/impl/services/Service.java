package org.jetbrains.compose.codeeditor.platform.impl.services;

import com.intellij.openapi.Disposable;

public interface Service extends Disposable {

    void init(Disposable parentDisposable);

}
