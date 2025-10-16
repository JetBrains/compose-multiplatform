package org.jetbrains.compose.resources

/**
 * `@ResourceContentHash` annotation is used to mark resource accessors with the resource content hash.
 * It can be used by a client to determine if the resource content is changed or not.
 */
@Suppress("unused")
@Retention(AnnotationRetention.BINARY)
annotation class ResourceContentHash(val hash: Int)
