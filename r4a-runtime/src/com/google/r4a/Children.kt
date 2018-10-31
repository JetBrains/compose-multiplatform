package com.google.r4a

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION
)
annotation class Children(val composable: Boolean = true)
