package org.jetbrains.compose.codeeditor.platform.impl;

import org.jetbrains.compose.codeeditor.platform.api.GotoDeclarationData;
import org.jetbrains.compose.codeeditor.platform.api.GotoDeclarationTarget;
import com.intellij.openapi.util.TextRange;

import java.util.Collection;
import java.util.Collections;

public class GTDData implements GotoDeclarationData {
    public static final GTDData NON_NAVIGATABLE = new GTDData(false);

    private final boolean indexNotReady;
    private final TextRange initialElementRange;
    private final Collection<GTDTarget> targets;

    private GTDData(boolean indexNotReady, Collection<GTDTarget> targets, TextRange initialElementRange) {
        this.indexNotReady = indexNotReady;
        this.targets = Collections.unmodifiableCollection(targets);
        this.initialElementRange = initialElementRange;
    }

    private GTDData(boolean indexNotReady, TextRange initialElementRange) {
        this(indexNotReady, Collections.emptyList(), initialElementRange);
    }

    private GTDData(boolean indexNotReady) {
        this(indexNotReady, TextRange.EMPTY_RANGE);
    }

    public GTDData(Collection<GTDTarget> targets, TextRange initialElementRange) {
        this(false, targets, initialElementRange);
    }

    public static GTDData createIndexNotReady(TextRange initialElementRange) {
        return new GTDData(true, initialElementRange);
    }

    public static GTDData createNonNavigatable(TextRange initialElementRange) {
        return new GTDData(false, initialElementRange);
    }

    @Override
    public boolean isIndexNotReady() {
        return indexNotReady;
    }

    @Override
    public boolean canNavigate() {
        return !indexNotReady && !targets.isEmpty();
    }

    @Override
    public boolean isInitialElementOffsetSet() {
        return !initialElementRange.equalsToRange(0, 0);
    }

    @Override
    public int getInitialElementStartOffset() {
        return initialElementRange.getStartOffset();
    }

    @Override
    public int getInitialElementEndOffset() {
        return initialElementRange.getEndOffset();
    }

    @Override
    public Collection<? extends GotoDeclarationTarget> getTargets() {
        return targets;
    }
}
