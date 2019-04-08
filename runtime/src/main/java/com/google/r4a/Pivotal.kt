package com.google.r4a

/**
 * @Pivotal can be applied to the parameters of a composable to indicate that
 * the parameter contributes to the "identity" of the composable.  Pivotal
 * parameters are used when calculating the composable's `key`, and the
 * composable's key is used to determine if/when private state of the
 * composable (and/or of the composable's children) should be reused.
 * To learn more: https://goto.google.com/r4a-keys
 */
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION
)
annotation class Pivotal
