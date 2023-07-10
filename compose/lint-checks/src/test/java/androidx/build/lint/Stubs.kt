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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles

class Stubs {

    companion object {

        /* ktlint-disable max-line-length */

        /**
         * [TestFile] containing Keep.java from the annotation library.
         */
        val Keep = TestFiles.java(
            """
package androidx.annotation;

public @interface Keep {
}
            """
        )

        val RunWith = TestFiles.kotlin(
            """
package org.junit.runner

annotation class RunWith(val value: KClass<*>)
            """
        )

        val JUnit4Runner = TestFiles.kotlin(
            """
package org.junit.runners

class JUnit4
            """
        )

        val ParameterizedRunner = TestFiles.kotlin(
            """
package org.junit.runners

class Parameterized
            """
        )

        val AndroidJUnit4Runner = TestFiles.kotlin(
            """
package androidx.test.ext.junit.runners

class AndroidJUnit4
            """
        )

        val TestSizeAnnotations = TestFiles.kotlin(
            """
package androidx.test.filters

annotation class SmallTest
annotation class MediumTest
annotation class LargeTest
            """
        )

        val TestAnnotation = TestFiles.kotlin(
            """
package org.junit

annotation class Test
            """
        )

        /**
         * [TestFile] containing OptIn.kt from the Kotlin standard library.
         *
         * This is a workaround for the Kotlin standard library used by the Lint test harness not
         * including the Experimental annotation by default.
         */
        val OptIn = TestFiles.kotlin(
            """
package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind
import kotlin.reflect.KClass

@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@SinceKotlin("1.3")
@RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class RequiresOptIn(
    val message: String = "",
    val level: Level = Level.ERROR
) {
    public enum class Level {
        WARNING,
        ERROR,
    }
}

@Target(
    CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
)
@Retention(SOURCE)
@SinceKotlin("1.3")
@RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class OptIn(
    vararg val markerClass: KClass<out Annotation>
)
            """
        )

        /**
         * [TestFile] containing ChecksSdkIntAtLeast.java from the annotation library.
         */
        val ChecksSdkIntAtLeast = TestFiles.java(
            """
package androidx.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({METHOD, FIELD})
public @interface ChecksSdkIntAtLeast {
    int api() default -1;
    String codename() default "";
    int parameter() default -1;
    int lambda() default -1;
}
            """
        )

        val RequiresApi = TestFiles.java(
            """
package androidx.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
public @interface RequiresApi {
    @IntRange(from = 1)
    int value() default 1;
    @IntRange(from = 1)
    int api() default 1;
}
            """
        )

        val IntRange = TestFiles.java(
            """
package androidx.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE})
public @interface IntRange {
    long from() default Long.MIN_VALUE;
    long to() default Long.MAX_VALUE;
}
            """
        )

        val RestrictTo = TestFiles.java(
            """
package androidx.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({ANNOTATION_TYPE, TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
public @interface RestrictTo {
    Scope[] value();
    enum Scope {
        LIBRARY,
        LIBRARY_GROUP,
        LIBRARY_GROUP_PREFIX,
        @Deprecated
        GROUP_ID,
        TESTS,
        SUBCLASSES,
    }
}
            """
        )

        val JetBrainsAnnotations = TestFiles.kotlin(
            """
package org.jetbrains.annotations

annotation class NotNull
annotation class Nullable
            """
        )

        val IgnoreAnnotation = TestFiles.kotlin(
            """
package org.junit

annotation class Ignore
            """
        )

        val DoNotInline = TestFiles.java(
            """
package androidx.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target({METHOD})
public @interface DoNotInline {
}
            """
        )

        val DeprecatedSinceApi = TestFiles.kotlin(
            """
package androidx.annotation

import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS, CONSTRUCTOR)
public annotation class DeprecatedSinceApi(
    val api: Int,
    val message: String = ""
)
            """
        )

        val JvmDefaultWithCompatibility = TestFiles.kotlin(
            """
package kotlin.jvm

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class JvmDefaultWithCompatibility
            """.trimIndent()
        )
        /* ktlint-enable max-line-length */
    }
}
