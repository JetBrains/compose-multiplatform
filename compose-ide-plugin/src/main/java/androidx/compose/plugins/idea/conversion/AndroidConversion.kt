/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.plugins.idea.conversion

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.j2k.ast.Expression
import org.jetbrains.kotlin.j2k.ast.LiteralExpression
import org.jetbrains.kotlin.j2k.ast.QualifiedExpression
import org.jetbrains.kotlin.name.FqName
import androidx.compose.plugins.kotlin.ComposeUtils

internal val ANDROID_CONVERSION = conversion {
    DefaultBuiltIns.Instance.booleanType
    anyClass {
        anyAttribute {
            "true" { success<Boolean>(LiteralExpression("true")) }
            "false" { success<Boolean>(LiteralExpression("false")) }
        }
    }
    "android.view.View" {
        "focusable" {
            anyValue {
                val enumValue = when (xmlValue) {
                    "auto" -> "FOCUSABLE_AUTO"
                    "true" -> "FOCUSABLE"
                    "false" -> "NOT_FOCUSABLE"
                    else -> null
                }

                if (enumValue != null) {
                    success<Int>(enumValue.asIdentifier("android.view.View.$enumValue"))
                } else {
                    failure()
                }
            }
        }
        anyOf("layout_width", "layout_height") {
            anyValue {
                val enumValue = when (xmlValue) {
                    "match_parent", "fill_parent" -> "MATCH_PARENT"
                    "wrap_content" -> "WRAP_CONTENT"
                    else -> null
                }

                if (enumValue != null) {
                    success<Int>(
                        enumValue.asIdentifier(
                            FqName("android.view.ViewGroup.LayoutParams.$enumValue")
                        )
                    )
                } else {
                    failure()
                }
            }
        }
        anyAttribute {
            // Resource.
            val resourceTypes = listOf(
                "anim", "animator", "array", "attr", "bool", "color", "dimen", "drawable", "font",
                "id", "integer", "layout", "menu", "mipmap", "string", "style", "styleable"
            ).joinToString("|")
            pattern("^@(([^:]+):)?($resourceTypes)/(.+)$") { matcher ->
                success<Int>(with(matcher) {
                    resourceExpression(
                        group(2),
                        group(3),
                        group(4)
                    )
                })
            }

            // Dimension.
            pattern("^(\\d+(\\.\\d+)?)(px|dip|dp|sp|pt|in|mm)$") { matcher ->
                // Returns expression: <value>.<unit>
                val unit = matcher.group(3)
                success(
                    QualifiedExpression(
                        qualifier = LiteralExpression(matcher.group(1)),
                        identifier = unit.asIdentifier(ComposeUtils.composeFqName("adapters.$unit")),
                        dotPrototype = null
                    ), ComposeUtils.composeFqName("adapters.Dimension")
                )
            }
        }
    }
}

// Returns expression: [<package>.]R.<type>.<name>
private fun resourceExpression(packageName: String?, type: String, name: String): Expression {
    // TODO(jdemeulenaere): We directly return the resource ID
    return if (packageName == null) {
        resourceExpression(type, name)
    } else {
        QualifiedExpression(
            qualifier = packageName.asIdentifier(),
            identifier = resourceExpression(type, name),
            dotPrototype = null
        )
    }
}

// Returns expression: R.<type>.<name>
private fun resourceExpression(type: String, name: String): Expression {
    return QualifiedExpression(
        qualifier = QualifiedExpression(
            qualifier = "R".asIdentifier(),
            identifier = type.asIdentifier(),
            dotPrototype = null
        ),
        identifier = name.asIdentifier(),
        dotPrototype = null
    )
}