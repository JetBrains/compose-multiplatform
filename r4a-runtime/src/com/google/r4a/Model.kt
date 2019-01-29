package com.google.r4a

/**
 * @Model can be applied to a class which represents your application's data model, and will cause
 * the data model class to become observable, such that any composable functions which read from the
 * model will be recomposed when the model is changed.  @Model also adds the a MVCC transaction system
 * to ensure data consistency across threads.  To learn more, see https://goto.google.com/r4a-model
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS )
@Retention(AnnotationRetention.BINARY)
annotation class Model
