/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build

import net.ltgt.gradle.errorprone.ErrorProneToolChain
import org.gradle.api.tasks.compile.JavaCompile

const val ERROR_PRONE_VERSION = "com.google.errorprone:error_prone_core:2.2.0"

fun JavaCompile.configureWithErrorProne(toolChain: ErrorProneToolChain) {
    this.toolChain = toolChain

    val compilerArgs = this.options.compilerArgs
    compilerArgs += listOf(
            "-XDcompilePolicy=simple", // Workaround for b/36098770
            "-XepExcludedPaths:.*/(build/generated|external)/.*",

            // Enforce the following checks.
            "-Xep:RestrictTo:OFF",
            "-Xep:ParameterNotNullable:ERROR",
            "-Xep:MissingOverride:ERROR",
            "-Xep:JdkObsolete:ERROR",
            "-Xep:EqualsHashCode:ERROR",
            "-Xep:NarrowingCompoundAssignment:ERROR",
            "-Xep:ClassNewInstance:ERROR",
            "-Xep:ClassCanBeStatic:ERROR",
            "-Xep:SynchronizeOnNonFinalField:ERROR",
            "-Xep:OperatorPrecedence:ERROR",
            "-Xep:IntLongMath:ERROR",
            "-Xep:MissingFail:ERROR",
            "-Xep:JavaLangClash:ERROR",
            "-Xep:PrivateConstructorForUtilityClass:ERROR",
            "-Xep:TypeParameterUnusedInFormals:ERROR",
            "-Xep:StringSplitter:ERROR",
            "-Xep:ReferenceEquality:ERROR",

            // Nullaway
            "-XepIgnoreUnknownCheckNames", // https://github.com/uber/NullAway/issues/25
            "-Xep:NullAway:ERROR",
            "-XepOpt:NullAway:AnnotatedPackages=android.arch,android.support,androidx"
    )
}
