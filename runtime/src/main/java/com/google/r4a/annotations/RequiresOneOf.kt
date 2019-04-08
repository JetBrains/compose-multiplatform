package com.google.r4a.annotations

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class RequiresOneOf(vararg val properties: String)