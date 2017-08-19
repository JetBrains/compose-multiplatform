package com.google.r4a

public class MarkupFragment {

    val elements: List<Element>

    constructor(content : MarkupBuilder.()->Unit) {
        val builder = R4aElementBuilderDSL()
        content.invoke(builder);
        elements = builder.renderResults
    }

    constructor(elements : List<Element>) {
        this.elements = elements
    }

    operator fun unaryPlus(): Unit {
        System.out.println("****** Noise dude: "+elements);
        return;
    }

    fun write(builder: MarkupBuilder) {
        (builder as R4aElementBuilderDSL).appendElements(elements);
    }
}

fun markup_fragment(content : MarkupBuilder.()->Unit) = MarkupFragment(content);
