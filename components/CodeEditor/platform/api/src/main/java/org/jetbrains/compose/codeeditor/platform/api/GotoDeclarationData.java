package org.jetbrains.compose.codeeditor.platform.api;

import java.util.Collection;

public interface GotoDeclarationData {

    /**
     * @return true, if the index is not ready and it is impossible to get "go to declaration" data
     */
    boolean isIndexNotReady();

    /**
     * @return true, if navigation to the target is possible
     */
    boolean canNavigate();

    /**
     * @return true, if the initial element offset was successfully set
     */
    boolean isInitialElementOffsetSet();

    /**
     * @return start offset of the initial element
     */
    int getInitialElementStartOffset();

    /**
     * @return end offset of the initial element
     */
    int getInitialElementEndOffset();

    /**
     * @return collection of items containing coordinates of declarations
     */
    Collection<? extends GotoDeclarationTarget> getTargets();

}
