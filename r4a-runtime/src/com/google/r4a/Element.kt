package com.google.r4a

import android.view.View

import java.util.ArrayList
import java.util.HashMap

class Element {

    var defaultType: String? = null;
    private var unsubstitutedType: Class<*>? = null
    private var substitutedType: Class<*>? = null
    public var attributes: MutableMap<String, Any?> = HashMap()
    internal var children: ArrayList<Element> = ArrayList<Element>()
    internal var inlineChildren = false // Are children being defined directly in the body of the parent

    public var child: View? = null // Set during reconciliation

    constructor(type: Class<*>) {
        this.unsubstitutedType = type
        val compositeComponent = Component::class.java.isAssignableFrom(unsubstitutedType)
        try {
            if (compositeComponent)
                substitutedType = Class.forName(unsubstitutedType!!.canonicalName + "WrapperView", false, unsubstitutedType!!.classLoader)
            else
                substitutedType = unsubstitutedType
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    constructor(type: String) {
        this.defaultType = type
        if ("include" != defaultType) {
            throw RuntimeException("The only valid/supported default type is 'include'.")
        }
    }

    fun getSubstitutedType(): Class<*>? {
        return substitutedType
    }

    fun getUnsubstitutedType(): Class<*> {
        return unsubstitutedType as Class<*>
    }

    fun putAttribute(key: String, value: Any?) {
        attributes.put(key, value)
    }

    fun <T> getAttribute(key: String, type: Class<T>): T? {
        val value = attributes[key] ?: return null
        if (type.isAssignableFrom(value.javaClass)) return type.cast(attributes[key])
        if (String::class.java == type) return type.cast(value.toString()) as T
        throw ClassCastException("Expecting " + type + " but got " + value.javaClass.canonicalName + " (instance of " + value + ")")
    }

    fun addChild(child: Element) {
        var children: ArrayList<Element>? = attributes["children"] as ArrayList<Element>?
        if (children != null && inlineChildren == false) {
            throw IllegalStateException("Children may be specified as `children` attribute XOR nested in the body, but not both")
        }

        if (children == null) {
            children = ArrayList<Element>()
            attributes.put("children", children)
            inlineChildren = true
        }

        children.add(child)
    }

    override fun toString(): String {
        return if (defaultType != null) "Element(defaultType: $defaultType)" else "Element(" + unsubstitutedType!!.simpleName + attributes + ")"
    }
}
