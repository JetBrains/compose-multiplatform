package org.jetbrains.compose.web.attributes

import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.css.StyleBuilderImpl
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

class AttrsBuilder<TElement : Element> : EventsListenerBuilder() {
    private val attributesMap = mutableMapOf<String, String>()
    val styleBuilder = StyleBuilderImpl()

    val propertyUpdates = mutableListOf<Pair<(Element, Any) -> Unit, Any>>()
    var refEffect: (DisposableEffectScope.(Element) -> DisposableEffectResult)? = null

    fun style(builder: StyleBuilder.() -> Unit) {
        styleBuilder.apply(builder)
    }

    fun classes(vararg classes: String) = prop(setClassList, classes)

    fun id(value: String) = attr(ID, value)
    fun hidden(value: Boolean) = attr(HIDDEN, value.toString())
    fun title(value: String) = attr(TITLE, value)
    fun dir(value: DirType) = attr(DIR, value.dirStr)
    fun draggable(value: Draggable) = attr(DRAGGABLE, value.str)
    fun contentEditable(value: Boolean) = attr(CONTENT_EDITABLE, value.toString())
    fun lang(value: String) = attr(LANG, value)
    fun tabIndex(value: Int) = attr(TAB_INDEX, value.toString())
    fun spellCheck(value: Boolean) = attr(SPELLCHECK, value.toString())

    fun ref(effect: DisposableEffectScope.(Element) -> DisposableEffectResult) {
        this.refEffect = effect
    }

    fun attr(attr: String, value: String?): AttrsBuilder<TElement> {
        if (value == null) {
            attributesMap.remove(attr)
        } else {
            attributesMap[attr] = value
        }

        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : HTMLElement, V> prop(update: (E, V) -> Unit, value: V) {
        propertyUpdates.add((update to value) as Pair<(Element, Any) -> Unit, Any>)
    }

    fun collect(): Map<String, String> {
        return attributesMap
    }

    companion object {
        const val CLASS = "class"
        const val ID = "id"
        const val HIDDEN = "hidden"
        const val TITLE = "title"
        const val DIR = "dir"
        const val DRAGGABLE = "draggable"
        const val CONTENT_EDITABLE = "contenteditable"
        const val LANG = "lang"
        const val TAB_INDEX = "tabindex"
        const val SPELLCHECK = "spellcheck"
    }
}

val setClassList: (HTMLElement, Array<out String>) -> Unit = { e, classList ->
    e.classList.add(*classList)
}
