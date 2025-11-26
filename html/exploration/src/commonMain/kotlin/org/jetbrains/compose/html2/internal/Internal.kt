package org.jetbrains.compose.html2.internal

import androidx.compose.runtime.*

internal interface ComposeHtml2Context {

    @Composable
    fun TagElement(
        tag: String,
        attrsScope: () -> Unit,
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
internal inline fun <TScope, T> ComposeDomNode(
    crossinline factory: () -> T,
    elementScope: TScope,
    attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: (@Composable TScope.() -> Unit)
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
    content.invoke(elementScope)
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}