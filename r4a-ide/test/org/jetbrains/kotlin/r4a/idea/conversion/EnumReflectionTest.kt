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

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaOrKotlinMemberDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class EnumReflectionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinLightProjectDescriptor.INSTANCE
    }

    private fun addJavaEnum(): ClassDescriptor {
        return myFixture.addClass(
            """
            package annotations;

            public enum MyAnnotation { FOO, BAR }""".trimIndent()
        ).getJavaOrKotlinMemberDescriptor() as ClassDescriptor
    }

    private fun addKotlinEnum(): ClassDescriptor {
        val ktFile = myFixture.configureByText(
            "MyAnnotation.kt", """
                package annotations

                enum class MyAnnotation { FOO, BAR }""".trimIndent()
        ) as KtFile
        return ktFile.classes[0].getJavaOrKotlinMemberDescriptor() as ClassDescriptor
    }

    private fun addJavaIntEnum() {
        myFixture.addClass(
            """
            package annotations;

            @android.annotation.IntDef(value = {
                MyAnnotation.FOO,
                MyAnnotation.BAR
            })
            public @interface MyAnnotation {
                int FOO = 0;
                int BAR = 1;
            }
        """.trimIndent()
        )
    }

    private val possibleEnumValues = listOf(
        FqName("annotations.MyAnnotation.FOO"),
        FqName("annotations.MyAnnotation.BAR")
    )

    private fun DeclarationDescriptor.assertCorrect() {
        assertEquals(
            possibleEnumValues,
            getPossibleValues(project, this)
        )
    }

    fun testJavaEnum() {
        val descriptor = addJavaEnum()
        assertEquals(
            possibleEnumValues,
            getPossibleValuesFromEnum(descriptor.defaultType)
        )
    }

    fun testKotlinEnum() {
        val descriptor = addKotlinEnum()
        assertEquals(
            possibleEnumValues,
            getPossibleValuesFromEnum(descriptor.defaultType)
        )
    }

    fun testJavaIntSetter() {
        addJavaIntEnum()
        val psiClass = myFixture.addClass(
            """
            public class DummyClass {
                public void setValue(@annotations.MyAnnotation int value) {}
            }
        """.trimIndent()
        )

        psiClass.methods
            .first { it.name == "setValue" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testJavaEnumSetter() {
        addJavaEnum()
        val psiClass = myFixture.addClass(
            """
            public class DummyClass {
                public void setValue(annotations.MyAnnotation value) {}
            }
        """.trimIndent()
        )

        psiClass.methods
            .first { it.name == "setValue" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testJavaIntConstructorParam() {
        addJavaIntEnum()
        val psiClass = myFixture.addClass(
            """
            public class DummyClass {
                public DummyClass(@annotations.MyAnnotation int value) {}
            }
        """.trimIndent()
        )

        (psiClass.constructors.first()
            .getJavaOrKotlinMemberDescriptor()!! as ConstructorDescriptor)
            .valueParameters.first()
            .assertCorrect()
    }

    fun testJavaEnumConstructorParam() {
        addJavaEnum()
        val psiClass = myFixture.addClass(
            """
            public class DummyClass {
                public DummyClass(annotations.MyAnnotation value) {}
            }
        """.trimIndent()
        )

        (psiClass.constructors.first()
            .getJavaOrKotlinMemberDescriptor()!! as ConstructorDescriptor)
            .valueParameters.first()
            .assertCorrect()
    }

    fun testJavaIntField() {
        addJavaIntEnum()
        val psiClass = myFixture.addClass(
            """
            public class DummyClass {
                @annotations.MyAnnotation
                public int value;
            }
        """.trimIndent()
        )

        psiClass.fields
            .first { it.name == "value" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testJavaEnumField() {
        addJavaEnum()
        val psiClass = myFixture.addClass(
            """
            public class DummyClass {
                public annotations.MyAnnotation value;
            }
        """.trimIndent()
        )

        psiClass.fields
            .first { it.name == "value" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testKotlinIntSetter() {
        addJavaIntEnum()
        val ktFile = myFixture.configureByText(
            "DummyClass.kt",
            """
            class DummyClass {
                fun setValue(@annotations.MyAnnotation value: Int) {}
            }
        """.trimIndent()
        ) as KtFile

        ktFile.classes[0]
            .methods
            .first { it.name == "setValue" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testKotlinEnumSetter() {
        addKotlinEnum()
        val ktFile = myFixture.configureByText(
            "DummyClass.kt",
            """
            class DummyClass {
                fun setValue(value: annotations.MyAnnotation) {}
            }
        """.trimIndent()
        ) as KtFile

        ktFile.classes[0]
            .methods
            .first { it.name == "setValue" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testKotlinIntConstructorParam() {
        addJavaIntEnum()
        val ktFile = myFixture.configureByText(
            "DummyClass.kt",
            """
            class DummyClass(@annotations.MyAnnotation val value: Int)
        """.trimIndent()
        ) as KtFile

        (ktFile.classes.first()
            .constructors.first()
            .getJavaOrKotlinMemberDescriptor()!! as ConstructorDescriptor)
            .valueParameters.first()
            .assertCorrect()
    }

    fun testKotlinEnumConstructorParam() {
        addKotlinEnum()
        val ktFile = myFixture.configureByText(
            "DummyClass.kt",
            """
            class DummyClass(val value: annotations.MyAnnotation)
        """.trimIndent()
        ) as KtFile

        (ktFile.classes.first()
            .constructors.first()
            .getJavaOrKotlinMemberDescriptor()!! as ConstructorDescriptor)
            .valueParameters.first()
            .assertCorrect()
    }

    fun testKotlinIntProperty() {
        addJavaIntEnum()
        val ktFile = myFixture.configureByText(
            "DummyClass.kt",
            """
            class DummyClass {
                @annotations.MyAnnotation var value: Int = 0
            }
        """.trimIndent()
        ) as KtFile

        ktFile.classes.first()
            .fields
            .first { it.name == "value" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }

    fun testKotlinEnumProperty() {
        addKotlinEnum()
        val ktFile = myFixture.configureByText(
            "DummyClass.kt",
            """
            class DummyClass {
                var value: annotations.MyAnnotation? = null
            }
        """.trimIndent()
        ) as KtFile

        ktFile.classes.first()
            .fields
            .first { it.name == "value" }
            .getJavaOrKotlinMemberDescriptor()!!
            .assertCorrect()
    }
}