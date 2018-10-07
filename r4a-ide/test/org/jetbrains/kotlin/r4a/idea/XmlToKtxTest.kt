/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea

import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.r4a.idea.conversion.XmlToComponentAction
import java.io.File

class XmlToKtxTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinJdkAndLibraryProjectDescriptor(
            listOf(
                File("plugins/r4a/r4a-runtime/build/libs/r4a-runtime-1.2-SNAPSHOT.jar"),
                File("custom-dependencies/android-sdk/build/libs/android.jar")
            )
        )
    }

    fun testConversion() {
        val xmlFile = myFixture.configureByText(
            "activity_main.xml", """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                xmlns:tools="http://schemas.android.com/tools"
                android:layout_width="match_parent"
                android:layout_height="fill_parent"
                tools:context=".MainActivity">

                <TextView
                    android:id="@+id/text"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text='Hello\World"$!'
                    android:textSize="32sp"
                    android:textColor="@android:color/black"
                    android:background="@drawable/text_background"
                    android:padding="@dimen/text_padding"
                    android:allCaps="@bool/text_all_caps"
                    android:visibility="visible"/>
            </LinearLayout>""".trimIndent()
        ) as XmlFile

        XmlToComponentAction.convertFiles(listOf(xmlFile))

        myFixture.checkResult(
            "MainComponent.kt", """
            import android.view.View.VISIBLE
            import android.view.ViewGroup.LayoutParams.MATCH_PARENT
            import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            import android.widget.LinearLayout
            import android.widget.TextView
            import com.google.r4a.Component
            import com.google.r4a.adapters.sp

            class MainComponent : Component() {
                override fun compose() {
                    <LinearLayout
                        layoutWidth=MATCH_PARENT
                        layoutHeight=MATCH_PARENT>
                        <TextView
                            layoutWidth=WRAP_CONTENT
                            layoutHeight=WRAP_CONTENT
                            text="Hello\\World\"\$!"
                            textSize=32.sp
                            textColor=android.R.color.black
                            background=R.drawable.text_background
                            padding=R.dimen.text_padding
                            allCaps=R.bool.text_all_caps
                            visibility=VISIBLE />
                    </LinearLayout>
                }
            }
            """.trimIndent(), /* stripTrailingSpaces= */ true
        )
    }
}
