package org.jetbrains.compose.web.attributes

import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.css.StyleBuilderImpl
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

/**
 * [AttrsBuilder] is a class that is used (as a builder context, that is as AttrsBuilder<T>.() -> Unit)
 * in all DOM-element creating API calls. It's used for adding attributes to the element created,
 * adding inline style values (via [style]) and attaching events to the element (since AttrsBuilder
 * is an [EventsListenerBuilder])
 *
 * In that aspect the most important method is [attr]. Setting the most frequently attributes, like [id], [tabIndex]
 * are extracted to a separate methods.
 *
 */
open class AttrsBuilder<TElement : Element> : EventsListenerBuilder() {
    internal val attributesMap = mutableMapOf<String, String>()
    internal val styleBuilder = StyleBuilderImpl()

    internal val propertyUpdates = mutableListOf<Pair<(Element, Any) -> Unit, Any>>()
    internal var refEffect: (DisposableEffectScope.(TElement) -> DisposableEffectResult)? = null

    internal var inputControlledValueSet = false
    internal var inputDefaultValueSet = false
    internal var inputControlledCheckedSet = false
    internal var inputDefaultCheckedSet = false

    /**
     * [style] add inline CSS-style properties to the element via [StyleBuilder] context
     *
     * Example:
     * ```
     * Div({
     *      style { maxWidth(5.px) }
     * })
     * ```
     */
    fun style(builder: StyleBuilder.() -> Unit) {
        styleBuilder.apply(builder)
    }

    /**
     * [classes] adds all values passed as params to the element's classList.
     *  This method acts cumulatively, that is, each call adds values to the classList.
     *  In the ideology of Composable functions and their recomposition one just don't need to remove classes,
     *  since if your classList is, for instance, condition-dependent, you can always just call this method conditionally.
     */
    fun classes(vararg classes: String) = prop(setClassList, classes)

    fun id(value: String) = attr(ID, value)
    fun hidden() = attr(HIDDEN, true.toString())
    fun title(value: String) = attr(TITLE, value)
    fun dir(value: DirType) = attr(DIR, value.dirStr)
    fun draggable(value: Draggable) = attr(DRAGGABLE, value.str)
    fun contentEditable(value: Boolean) = attr(CONTENT_EDITABLE, value.toString())
    fun lang(value: String) = attr(LANG, value)
    fun tabIndex(value: Int) = attr(TAB_INDEX, value.toString())
    fun spellCheck(value: Boolean) = attr(SPELLCHECK, value.toString())

    /**
     * [ref] can be used to retrieve a reference to a html element.
     * The lambda that `ref` takes in is not Composable. It will be called only once when an element added into a composition.
     * Likewise, the lambda passed in `onDispose` will be called only once when an element leaves the composition.
     *
     * Under the hood, `ref` uses [DisposableEffect](https://developer.android.com/jetpack/compose/side-effects#disposableeffect)
     */
    fun ref(effect: DisposableEffectScope.(TElement) -> DisposableEffectResult) {
        this.refEffect = effect
    }

    /**
     * [attr] adds arbitrary attribute to the Element.
     * If it called twice for the same attribute name, attribute value will be resolved to the last call.
     *
     * @param attr - the name of the attribute
     * @param value - the value of the attribute
     *
     * For boolean attributes cast boolean value to String and pass it as value.
     */
    fun attr(attr: String, value: String): AttrsBuilder<TElement> {
        attributesMap[attr] = value
        return this
    }

    /**
     * [prop] allows setting values of element's properties which can't be set by ussing [attr].
     * [update] is a lambda with two parameters: `element` and `value`. `element` is a reference to a native element.
     * Some examples of properties that can set using [prop]: `value`, `checked`, `innerText`.
     *
     * Unlike [ref], lambda passed to [prop] will be invoked every time when AttrsBuilder being called during recomposition.
     * [prop] is not supposed to be used for adding listeners, subscriptions, etc.
     * Also see [ref].
     *
     * Code Example:
     * ```
     * Input(type = InputType.Text, attrs = {
     *      // This is only an example. One doesn't need to set `value` like this, since [Input] has `value(v: String)`
     *      prop({ element: HTMLInputElement, value: String -> element.value = value }, "someTextInputValue")
     * })
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : HTMLElement, V> prop(update: (E, V) -> Unit, value: V) {
        propertyUpdates.add((update to value) as Pair<(Element, Any) -> Unit, Any>)
    }

    internal fun collect(): Map<String, String> {
        return attributesMap
    }

    internal fun copyFrom(attrsBuilder: AttrsBuilder<TElement>) {
        refEffect = attrsBuilder.refEffect
        styleBuilder.copyFrom(attrsBuilder.styleBuilder)

        attributesMap.putAll(attrsBuilder.attributesMap)
        propertyUpdates.addAll(attrsBuilder.propertyUpdates)

        copyListenersFrom(attrsBuilder)
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

private val setClassList: (HTMLElement, Array<out String>) -> Unit = { e, classList ->
    e.classList.add(*classList)
}
