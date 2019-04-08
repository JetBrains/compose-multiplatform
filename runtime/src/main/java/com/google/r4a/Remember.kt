package com.google.r4a

// TODO: Add overloads that store the value to check for the remember to be invalidated.
fun <T> remember(block: () -> T) =
    (CompositionContext.current as?
            ComposerCompositionContext)?.composer?.remember(block) ?: block()
