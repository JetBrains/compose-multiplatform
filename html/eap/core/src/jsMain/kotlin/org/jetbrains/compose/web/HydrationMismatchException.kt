package org.jetbrains.compose.web

/** Describes why an existing server-rendered DOM tree could not be hydrated. */
class HydrationMismatchException(message: String) : IllegalStateException(message)
