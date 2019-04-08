package com.google.r4a

/**
 * @Composable can be applied to a function or lambda to indicate that the function/lambda can be
 * used as part of an R4A composition to describe a transformation from application data into a
 * widget hierarchy.
 * To learn more, see https://goto.google.com/r4a-composable
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER
)
annotation class Composable
