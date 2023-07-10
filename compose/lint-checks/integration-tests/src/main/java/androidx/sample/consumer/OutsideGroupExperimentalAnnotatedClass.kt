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

@file:Suppress("unused")

package androidx.sample.consumer

import sample.annotation.provider.ExperimentalSampleAnnotationJava
import sample.annotation.provider.RequiresAndroidXOptInSampleAnnotationJava
import sample.annotation.provider.RequiresAndroidXOptInSampleAnnotationJavaDuplicate
import sample.annotation.provider.RequiresOptInSampleAnnotationJava
import sample.annotation.provider.RequiresOptInSampleAnnotationJavaDuplicate

class OutsideGroupExperimentalAnnotatedClass {

    // b/201564937 (comments 3, 5-7) - temporarily commenting out due to import issue
//    @ExperimentalSampleAnnotation
//    fun invalidAnnotatedFunction() {
//        // Nothing to see here.
//    }

    @ExperimentalSampleAnnotationJava
    fun invalidExperimentalAnnotatedMethod() {
        // Nothing to see here.
    }

    @RequiresOptInSampleAnnotationJava
    fun invalidRequiresOptInAnnotatedMethod() {
        // Nothing to see here.
    }

    @kotlin.OptIn(RequiresOptInSampleAnnotationJava::class)
    fun invalidMethodWithSingleOptIn() {
        // Nothing to see here.
    }

    @kotlin.OptIn(
        RequiresOptInSampleAnnotationJava::class,
        RequiresOptInSampleAnnotationJavaDuplicate::class
    )
    fun invalidMethodWithMultipleOptInsWithLineBreaks() {
        // Nothing to see here.
    }

    /* ktlint-disable max-line-length */
    @kotlin.OptIn(RequiresOptInSampleAnnotationJava::class, RequiresOptInSampleAnnotationJavaDuplicate::class)
    fun invalidMethodWithMultipleOptInsWithoutLineBreaks() {
        // Nothing to see here.
    }
    /* ktlint-enable max-line-length */

    @androidx.annotation.OptIn(RequiresAndroidXOptInSampleAnnotationJava::class)
    fun invalidMethodWithSingleAndroidXOptIn() {
        // Nothing to see here.
    }

    @androidx.annotation.OptIn(
        RequiresAndroidXOptInSampleAnnotationJava::class,
        RequiresAndroidXOptInSampleAnnotationJavaDuplicate::class
    )
    fun invalidMethodWithMultipleAndroidXOptInsWithLineBreaks() {
        // Nothing to see here.
    }

    /* ktlint-disable max-line-length */
    @androidx.annotation.OptIn(RequiresAndroidXOptInSampleAnnotationJava::class, RequiresAndroidXOptInSampleAnnotationJavaDuplicate::class)
    fun invalidMethodWithMultipleAndroidXOptInsWithoutLineBreaks() {
        // Nothing to see here.
    }
    /* ktlint-enable max-line-length */
}
