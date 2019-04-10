package com.google.r4a.annotations

@Retention(AnnotationRetention.SOURCE)
annotation class ConflictsWith(vararg val properties: String)

