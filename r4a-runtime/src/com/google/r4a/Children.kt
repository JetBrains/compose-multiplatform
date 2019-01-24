package com.google.r4a

/**
 * @Children can be applied to one parameter of a composable function, to indicate that this
 * parameter represents the composable's children.  The @Children parameter is generally used
 * to describe the composable's sub-nodes within the composable's view hierarchy.  The type
 * of the @Children parameter is assumed to be @Composable unless otherwise specified.
 * To learn more, see the "children" section of the r4a-composable documentation available
 * here: https://goto.google.com/r4a-composable
 */
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION
)
annotation class Children(val composable: Boolean = true)
