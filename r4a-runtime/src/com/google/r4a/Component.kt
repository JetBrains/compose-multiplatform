package com.google.r4a

import android.os.Handler
import android.os.Looper

public abstract class Component {

    protected var markupBuilder : R4aElementBuilderDSL? = null

    companion object {
        private var wrappers = HashSet<Rerenderable>()
        @JvmStatic
        public fun addWrapper(wrapper: Rerenderable) {wrappers.add(wrapper)}
    }

    public abstract fun render();
    protected fun markup(content : MarkupBuilder.()->Unit) : Unit {
        markupBuilder = null
        val builder = R4aElementBuilderDSL()
        content.invoke(builder);
        markupBuilder = builder
        // throw UnsupportedOperationException("Please compile using the R4A compiler prior to running")
    }

    final fun rerender() {
        for(wrapper in HashSet(wrappers)) wrapper.rerender()
    }
}
