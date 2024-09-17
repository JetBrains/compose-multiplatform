package org.jetbrains.compose.resources.vector

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.decodeToImageVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlVectorParserTest {


    private val arrowVector1 = """
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="32dp"
            android:autoMirrored="true"
            android:height="32dp"
            android:viewportWidth="32"
            android:viewportHeight="32">
            <path
                android:pathData="M2.667,17.333H26.115L21.724,21.724C21.203,22.245 21.203,23.089 21.724,23.609C21.985,23.87 22.325,24 22.667,24C23.008,24 23.349,23.87 23.609,23.609L30.276,16.943C30.526,16.693 30.667,16.354 30.667,16C30.667,15.646 30.526,15.307 30.276,15.057L23.609,8.391C23.089,7.87 22.245,7.87 21.724,8.391C21.203,8.911 21.203,9.755 21.724,10.276L26.115,14.667H2.667C1.931,14.667 1.333,15.264 1.333,16C1.333,16.736 1.931,17.333 2.667,17.333Z"
                android:fillColor="#ffffff" />
        </vector>
    """.trimIndent().encodeToByteArray()

    private val arrowVector2 = """
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="40dp"
            android:height="30dp"
            android:viewportWidth="36"
            android:viewportHeight="24">
            <path
                android:pathData="M2.667,17.333H26.115L21.724,21.724C21.203,22.245 21.203,23.089 21.724,23.609C21.985,23.87 22.325,24 22.667,24C23.008,24 23.349,23.87 23.609,23.609L30.276,16.943C30.526,16.693 30.667,16.354 30.667,16C30.667,15.646 30.526,15.307 30.276,15.057L23.609,8.391C23.089,7.87 22.245,7.87 21.724,8.391C21.203,8.911 21.203,9.755 21.724,10.276L26.115,14.667H2.667C1.931,14.667 1.333,15.264 1.333,16C1.333,16.736 1.931,17.333 2.667,17.333Z"
                android:fillColor="#ffffff" />
        </vector>
    """.trimIndent().encodeToByteArray()

    @Test
    fun canDecodeToImageVector() {
        val iv = arrowVector1.decodeToImageVector(Density(1f))
        assertEquals(iv.viewportWidth, 32f)
        assertEquals(iv.viewportHeight, 32f)
        assertEquals(iv.defaultWidth, 32.dp)
        assertEquals(iv.defaultHeight, 32.dp)
        assertTrue(iv.autoMirror)

        val iv2 = arrowVector2.decodeToImageVector(Density(1f))
        assertEquals(iv2.viewportWidth, 36f)
        assertEquals(iv2.viewportHeight, 24f)
        assertEquals(iv2.defaultWidth, 40.dp)
        assertEquals(iv2.defaultHeight, 30.dp)
        assertFalse(iv2.autoMirror)
    }
}