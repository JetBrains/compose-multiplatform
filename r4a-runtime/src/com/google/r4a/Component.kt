package com.google.r4a

@Composable
public abstract class Component {

    protected var markupBuilder : R4aElementBuilderDSL? = null

    companion object {
        private var wrappers = HashSet<Recomposable>()
        @JvmStatic
        public fun addWrapper(wrapper: Recomposable) {wrappers.add(wrapper)}
    }

    public abstract fun compose();
    protected fun markup(content : MarkupBuilder.()->Unit) : Unit {
        markupBuilder = null
        val builder = R4aElementBuilderDSL()
        content.invoke(builder);
        markupBuilder = builder
        // throw UnsupportedOperationException("Please compile using the R4A compiler prior to running")
    }

    final fun recompose() {
        for(wrapper in HashSet(wrappers)) wrapper.recompose()
    }
}
