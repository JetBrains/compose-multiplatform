package com.google.r4a

import kotlin.reflect.KClass

abstract class MarkupBuilder {
    abstract fun component(type: Class<*>)
    abstract fun component(type: KClass<*>)
    abstract fun component(type: String)
    abstract fun component(type: Class<*>, body: R4aElementBuilderDSL.()->Unit)
    abstract fun component(type: KClass<*>, body: R4aElementBuilderDSL.()->Unit)
    abstract fun component(type: String, body: R4aElementBuilderDSL.()->Unit)

    operator fun Unit.unaryPlus(): Unit {
        System.out.println("****** Noise dude");
        return;
    }
}
