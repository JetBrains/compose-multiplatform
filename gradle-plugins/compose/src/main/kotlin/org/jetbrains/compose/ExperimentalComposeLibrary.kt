package org.jetbrains.compose

// We write explicitly about OptIn, because IDEA doesn't suggest it.
@RequiresOptIn("This library is experimental and can be unstable. Add @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class) annotation.")
@Deprecated("Please specify dependency via version catalog")
annotation class ExperimentalComposeLibrary