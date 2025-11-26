package org.jetbrains.compose.html2.internal

import androidx.compose.runtime.*
import org.jetbrains.compose.html2.AttrsScope

internal interface ComposeHtml2Context {

    @Composable
    fun TagElement(
        tag: String,
        attrsScope: AttrsScope.() -> Unit,
        content: @Composable () -> Unit
    )

    @Composable
    fun TagElement(tag: String)

    @Composable
    fun TextElement(text: String)
}

internal val LocalComposeHtml2Context = staticCompositionLocalOf<ComposeHtml2Context> {
    error("No Html2Context provided")
}

@Composable
@ExplicitGroupsComposable
internal inline fun <T> ComposeDomNode(
    crossinline factory: () -> T,
    attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: (@Composable () -> Unit)
) {
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode {
            factory()
        }
    } else {
        currentComposer.useNode()
    }

    attrsSkippableUpdate.invoke(SkippableUpdater(currentComposer))

    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content.invoke()
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

internal class AttrsScopeBuilder : AttrsScope {
    internal val attrs = mutableMapOf<String, String>()

    override var id: String?
        get() = attrs["id"]
        set(value) {
            if (value != null) {
                attrs["id"] = value
            } else {
                attrs.remove("id")
            }
        }

    override fun attr(name: String, value: String) {
        attrs[name] = value
    }

    override fun attr(name: String) {
        attrs[name] = ""
    }
}

internal val StringBasedComposeHtml2Context = object : ComposeHtml2Context {
    @Composable
    override fun TagElement(
        tag: String,
        attrsScope: AttrsScope.() -> Unit,
        content: @Composable (() -> Unit)
    ) {
        ComposeDomNode(
            factory = { HtmlStringNodeWrapper(element = HtmlElementStringNode(tag)) },
            attrsSkippableUpdate = {
                val attrsBuilder = AttrsScopeBuilder()
                attrsBuilder.attrsScope()

                update {
                    set(attrsBuilder.attrs) { this.element!!.updateAttrs(it) }
                }
            },
            content = content
        )
    }

    @Composable
    override fun TagElement(tag: String) {
        TagElement(tag = tag, attrsScope = {}, content = {})
    }

    @Composable
    override fun TextElement(text: String) {
        ComposeDomNode(
            factory = { HtmlStringNodeWrapper(text = HtmlTextStringNode(text)) },
            attrsSkippableUpdate = { },
            content = { }
        )
    }
}