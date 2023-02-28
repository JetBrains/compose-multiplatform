package org.jetbrains.compose.web.dom

import org.w3c.dom.Element

internal actual fun removeAttributesExceptStyleAndClass(node: Element) {
    removeNeededAttrs(node)
}

@JsFun(
    """ (node) => {
            const names = node.getAttributeNames()
            for (const n of names) {
                if (n != 'style' && n != 'class') {
                    node.removeAttribute(n);
                }
            }
    }
    """
)
private external fun removeNeededAttrs(node: Element)
