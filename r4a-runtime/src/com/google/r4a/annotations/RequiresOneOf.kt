package com.google.r4a.annotations

@Retention(AnnotationRetention.SOURCE)
annotation class RequiresOneOf(vararg val properties: String)