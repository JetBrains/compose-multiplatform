/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime

/**
 * The [Composable] function is declared to expect an applier with the name [applier]. The [applier]
 * name can be an arbitrary string but is expected to be a fully qualified name of a class that
 * is annotated by [ComposableTargetMarker] containing a descriptive name to be used in diagnostic
 * messages.
 *
 * The [applier] name is used in diagnostic messages but, if it refers to a marked annotation,
 * [ComposableTargetMarker.description] is used instead of the class name.
 *
 * The Compose compiler plugin can, in most cases, infer this or an equivalent
 * [ComposableInferredTarget], for composable functions. For example, if a composable function
 * calls another composable function then both must be of the same group of composable functions
 * (that is, have declared or inferred the same [applier] value). This means that, if the function
 * called is already determined to be in a group, them the function that calls it must also be in
 * the same group. If two functions are called of different groups then the Compose compiler plugin
 * will generate an diagnostic message describing which group was received and which group was
 * expected.
 *
 * The grouping of composable functions corresponds to the instance of [Applier] that is required to
 * be used by the [Composer] to apply changes to the composition. The [Applier] is checked at
 * runtime to ensure the [Applier] that is expected by a composable function is the one supplied at
 * runtime. This annotation, and the corresponding validation performed by the Compose compiler
 * plugin, can detect mismatches at compile time, and issue a diagnostic message when calling a
 * [Composable] function will result in the [Applier] check failing.
 *
 * In most cases this annotation can be inferred. However, this annotation is required for
 * [Composable] functions that call [ComposeNode] directly, for abstract methods, such as
 * interfaces functions (which do not contain a body from which the plugin can infer the
 * annotation), when using a composable lambda in sub-composition, or when a composable lambda is
 * stored in a class field or global variable.
 *
 * Leaving the annotation off in such cases will result in the compiler ignoring the function and it
 * will not emit the diagnostic when the function is called incorrectly.
 *
 * @param applier The applier name used during composable call checking. This is usually
 * inferred by the compiler. This can be an arbitrary string value but is expected to be a fully
 * qualified name of a class that is marked with [ComposableTargetMarker].
 */

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class ComposableTarget(val applier: String)
