package org.jetbrains.compose.web.dsl

import javax.inject.Inject

abstract class WebApplication  @Inject constructor(
    @Suppress("unused")
    val name: String,
) {

}