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

package androidx.compose.lint

import kotlinx.metadata.ClassName

/**
 * Contains common names used for lint checks.
 */
object Names {
    object Animation {
        val PackageName = Package("androidx.compose.animation")
    }
    object AnimationCore {
        val PackageName = Package("androidx.compose.animation.core")
    }
    object Runtime {
        val PackageName = Package("androidx.compose.runtime")

        val Composable = Name(PackageName, "Composable")
        val CompositionLocal = Name(PackageName, "CompositionLocal")
        val MutableState = Name(PackageName, "MutableState")
        val MutableStateOf = Name(PackageName, "mutableStateOf")
        val MutableStateListOf = Name(PackageName, "mutableStateListOf")
        val MutableStateMapOf = Name(PackageName, "mutableStateMapOf")
        val ProduceState = Name(PackageName, "produceState")
        val Remember = Name(PackageName, "remember")
    }
    object Ui {
        val PackageName = Package("androidx.compose.ui")
        val Composed = Name(PackageName, "composed")
        val Modifier = Name(PackageName, "Modifier")
        object Layout {
            val PackageName = Package("androidx.compose.ui.layout")
            val ParentDataModifier = Name(PackageName, "ParentDataModifier")
        }
    }
    object UiGraphics {
        val PackageName = Package("androidx.compose.ui.graphics")
        val Color = Name(PackageName, "Color")
    }
}

/**
 * Represents a qualified package
 *
 * @property segments the segments representing the package
 */
class PackageName internal constructor(internal val segments: List<String>) {
    /**
     * The Java-style package name for this [Name], separated with `.`
     */
    val javaPackageName: String
        get() = segments.joinToString(".")
}

/**
 * Represents the qualified name for an element
 *
 * @property pkg the package for this element
 * @property nameSegments the segments representing the element - there can be multiple in the
 * case of nested classes.
 */
class Name internal constructor(
    private val pkg: PackageName,
    private val nameSegments: List<String>
) {
    /**
     * The short name for this [Name]
     */
    val shortName: String
        get() = nameSegments.last()

    /**
     * The Java-style fully qualified name for this [Name], separated with `.`
     */
    val javaFqn: String
        get() = pkg.segments.joinToString(".", postfix = ".") +
            nameSegments.joinToString(".")

    /**
     * The [ClassName] for use with kotlinx.metadata. Note that in kotlinx.metadata the actual
     * type might be different from the underlying JVM type, for example:
     * kotlin/Int -> java/lang/Integer
     */
    val kmClassName: ClassName
        get() = pkg.segments.joinToString("/", postfix = "/") +
            nameSegments.joinToString(".")
}

/**
 * @return a [PackageName] with a Java-style (separated with `.`) [packageName].
 */
fun Package(packageName: String): PackageName =
    PackageName(packageName.split("."))

/**
 * @return a [PackageName] with a Java-style (separated with `.`) [packageName].
 */
fun Package(packageName: PackageName, shortName: String): PackageName =
    PackageName(packageName.segments + shortName.split("."))

/**
 * @return a [Name] with the provided [pkg] and Java-style (separated with `.`) [shortName].
 */
fun Name(pkg: PackageName, shortName: String): Name =
    Name(pkg, shortName.split("."))
