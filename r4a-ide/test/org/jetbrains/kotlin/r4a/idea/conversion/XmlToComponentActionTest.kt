/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.psi.xml.XmlFile

class XmlToComponentActionTest : BaseXmlToKtxConversionTest() {
    fun testConversion() {
        val xmlFile = myFixture.configureByText(
            "activity_main.xml", /* language=XML */ """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout
                xmlns:android="http://schemas.android.com/apk/res/android"
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
                    android:visibility="visible"
                    android:textAlignment="textStart"/>
            </LinearLayout>""".trimIndent()
        ) as XmlFile

        XmlToComponentAction.convertFiles(listOf(xmlFile))

        myFixture.checkResult(
            "Main.kt", """
            import android.view.View.TEXT_ALIGNMENT_TEXT_START
            import android.view.View.VISIBLE
            import android.view.ViewGroup.LayoutParams.MATCH_PARENT
            import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            import android.widget.LinearLayout
            import android.widget.TextView
            import com.google.r4a.*
            import com.google.r4a.adapters.setLayoutHeight
            import com.google.r4a.adapters.setLayoutWidth
            import com.google.r4a.adapters.setPadding
            import com.google.r4a.adapters.setTextSize
            import com.google.r4a.adapters.sp

            @Composable
            fun Main() {
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
                        visibility=VISIBLE
                        textAlignment=TEXT_ALIGNMENT_TEXT_START />
                </LinearLayout>
            }
            """.trimIndent(), /* stripTrailingSpaces= */ true
        )
    }
}
