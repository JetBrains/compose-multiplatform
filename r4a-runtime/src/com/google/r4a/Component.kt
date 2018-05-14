package com.google.r4a

@Composable
abstract class Component {
    abstract fun compose()
    fun recompose() {
        val cc = CompositionContext.find(this)
        if (cc == null) {
            println("couldnt find composition context to update!")
        } else {
            cc.recomposeFromRoot()
        }
    }
}
