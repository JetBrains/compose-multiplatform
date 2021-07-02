package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.SkippableUpdater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.DomApplier
import org.jetbrains.compose.web.DomElementWrapper
import org.jetbrains.compose.web.attributes.AttrsBuilder
import kotlinx.browser.document
import org.w3c.dom.Audio
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLAreaElement
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDataListElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLEmbedElement
import org.w3c.dom.HTMLFieldSetElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHRElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.HTMLLegendElement
import org.w3c.dom.HTMLMapElement
import org.w3c.dom.HTMLMeterElement
import org.w3c.dom.HTMLOListElement
import org.w3c.dom.HTMLObjectElement
import org.w3c.dom.HTMLOptGroupElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLOutputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLParamElement
import org.w3c.dom.HTMLPictureElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLProgressElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTableCaptionElement
import org.w3c.dom.HTMLTableCellElement
import org.w3c.dom.HTMLTableColElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.HTMLTableSectionElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLTrackElement
import org.w3c.dom.HTMLUListElement
import org.w3c.dom.HTMLVideoElement

@OptIn(ComposeCompilerApi::class)
@Composable
@ExplicitGroupsComposable
inline fun <TScope, T, reified E : Applier<*>> ComposeDomNode(
    noinline factory: () -> T,
    elementScope: TScope,
    noinline attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    noinline content: (@Composable TScope.() -> Unit)?
) {
    if (currentComposer.applier !is E) error("Invalid applier")
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }

    SkippableUpdater<T>(currentComposer).apply {
        attrsSkippableUpdate()
    }

    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content?.invoke(elementScope)
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

class DisposableEffectHolder(
    var effect: (DisposableEffectScope.(Element) -> DisposableEffectResult)? = null
)

@Composable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsBuilder<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    val refEffect = remember { DisposableEffectHolder() }

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper, DomApplier>(
        factory = {
            DomElementWrapper(elementBuilder.create() as HTMLElement).also {
                scope.element = it.node.unsafeCast<TElement>()
            }
        },
        attrsSkippableUpdate = {
            val attrsApplied = AttrsBuilder<TElement>().also {
                if (applyAttrs != null) {
                    it.applyAttrs()
                }
            }
            refEffect.effect = attrsApplied.refEffect
            val attrsCollected = attrsApplied.collect()
            val events = attrsApplied.collectListeners()

            update {
                set(attrsCollected, DomElementWrapper::updateAttrs)
                set(events, DomElementWrapper::updateEventListeners)
                set(attrsApplied.propertyUpdates, DomElementWrapper::updateProperties)
                set(attrsApplied.styleBuilder, DomElementWrapper::updateStyleDeclarations)
            }
        },
        elementScope = scope,
        content = content
    )

    DisposableEffect(null) {
        refEffect.effect?.invoke(this, scope.element) ?: onDispose {}
    }
}

@Composable
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: AttrsBuilder<TElement>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) = TagElement(
    elementBuilder = ElementBuilder.createBuilder(tagName),
    applyAttrs = applyAttrs,
    content = content
)
