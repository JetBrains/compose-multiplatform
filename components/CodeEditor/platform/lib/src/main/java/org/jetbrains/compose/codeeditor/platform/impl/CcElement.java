package org.jetbrains.compose.codeeditor.platform.impl;

import org.jetbrains.compose.codeeditor.platform.api.CodeCompletionElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;

public class CcElement implements CodeCompletionElement {
    private final String name;
    private final String type;
    private final String tail;
    private String cacheString;

    CcElement(LookupElement element) {
        var presentation = LookupElementPresentation.renderElement(element);
        name = element.getLookupString();
        type = presentation.getTypeText();
        tail = presentation.getTailText();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTail() {
        return tail;
    }

    @Override
    public String toString() {
         if (cacheString == null) {
             var sb = new StringBuilder();
             if (type != null) sb.append(type).append(' ');
             if (name != null) sb.append(name);
             if (tail != null) sb.append(tail);
             cacheString = sb.toString();
         }
         return cacheString;
    }

}
