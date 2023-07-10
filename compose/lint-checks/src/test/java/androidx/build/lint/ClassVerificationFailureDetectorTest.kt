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

import androidx.build.lint.Stubs.Companion.DoNotInline
import androidx.build.lint.Stubs.Companion.RequiresApi
import androidx.build.lint.Stubs.Companion.IntRange
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClassVerificationFailureDetectorTest : AbstractLintDetectorTest(
    useDetector = ClassVerificationFailureDetector(),
    useIssues = listOf(ClassVerificationFailureDetector.ISSUE),
    stubs = arrayOf(
        // AndroidManifest with minSdkVersion=14
        manifest().minSdk(14),
    ),
) {

    @Test
    fun `Detection of unsafe references in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.ClassVerificationFailureFromJava"),
            RequiresApi,
            IntRange
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ClassVerificationFailureFromJava.java:37: Error: This call references a method added in API level 21; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            view.setBackgroundTintList(tint);
                 ~~~~~~~~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:46: Error: This call references a method added in API level 17; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return View.generateViewId();
                        ~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:56: Error: This call references a method added in API level 23; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return view.getAccessibilityClassName();
                    ~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.widget.ListViewCompat"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/widget/ListViewCompat.java:39: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y);
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompat.java:69: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return listView.canScrollList(direction);
                            ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 39: Extract to static inner class:
@@ -39 +39
-             listView.scrollListBy(y);
+             Api19Impl.scrollListBy((android.widget.AbsListView) listView, y);
@@ -91 +91
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static void scrollListBy(android.widget.AbsListView absListView, int y) {
+         absListView.scrollListBy(y);
+     }
+
@@ -92 +103
+ }
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 69: Extract to static inner class:
@@ -69 +69
-             return listView.canScrollList(direction);
+             return Api19Impl.canScrollList((android.widget.AbsListView) listView, direction);
@@ -91 +91
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean canScrollList(android.widget.AbsListView absListView, int direction) {
+         return absListView.canScrollList(direction);
+     }
+
@@ -92 +103
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.sample.core.widget.ListViewCompatKotlin"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:33: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y)
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:58: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.canScrollList(direction)
                     ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Java source`() {
        val input = arrayOf(
            javaSample("androidx.RequiresApiJava"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Kotlin source`() {
        val input = arrayOf(
            ktSample("androidx.RequiresApiKotlin"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/RequiresApiKotlinOuter19Passes.kt:67: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinNoAnnotationFails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:77: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinOuter16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:87: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinInner16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:98: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinInner16Outer16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Auto-fix unsafe void-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeVoidMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeVoidMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -37 +37
+ @androidx.annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
+
@@ -38 +49
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe constructor reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeConstructorReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeConstructorReferenceJava.java line 35: Extract to static inner class:
@@ -35 +35
-             AccessibilityNodeInfo node = new AccessibilityNodeInfo(new View(context), 1);
+             AccessibilityNodeInfo node = Api30Impl.createAccessibilityNodeInfo(new View(context), 1);
@@ -38 +38
+ @androidx.annotation.RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static AccessibilityNodeInfo createAccessibilityNodeInfo(View root, int virtualDescendantId) {
+         return new AccessibilityNodeInfo(root, virtualDescendantId);
+     }
+
@@ -39 +50
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe static method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeStaticMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeStaticMethodReferenceJava.java line 33: Extract to static inner class:
@@ -33 +33
-             return View.generateViewId();
+             return Api17Impl.generateViewId();
@@ -37 +37
+ @androidx.annotation.RequiresApi(17)
+ static class Api17Impl {
+     private Api17Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static int generateViewId() {
+         return View.generateViewId();
+     }
+
@@ -38 +49
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe generic-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeGenericMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeGenericMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             return context.getSystemService(serviceClass);
+             return Api23Impl.getSystemService(context, serviceClass);
@@ -38 +38
+ @androidx.annotation.RequiresApi(23)
+ static class Api23Impl {
+     private Api23Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static <T> T getSystemService(Context context, java.lang.Class<T> serviceClass) {
+         return context.getSystemService(serviceClass);
+     }
+
@@ -39 +50
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source with existing inner class`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeReferenceWithExistingClassJava"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingClassJava.java line 36: Extract to static inner class:
@@ -36 +36
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -46 +46
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
+
@@ -47 +58
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source when the fix code already exists`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeReferenceWithExistingFix"),
            RequiresApi,
            DoNotInline
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeReferenceWithExistingFix.java:37: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeReferenceWithExistingFix is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        view.setBackgroundTintList(new ColorStateList(null, null));
             ~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeReferenceWithExistingFix.java:45: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeReferenceWithExistingFix is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        drawable.getOutline(null);
                 ~~~~~~~~~~
2 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFix.java line 37: Extract to static inner class:
@@ -37 +37
-         view.setBackgroundTintList(new ColorStateList(null, null));
+         Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFix.java line 45: Extract to static inner class:
@@ -45 +45
-         drawable.getOutline(null);
+         Api21Impl.getOutline(drawable, null);
@@ -58 +58
-     }
+     @DoNotInline
+ static void getOutline(Drawable drawable, android.graphics.Outline outline) {
+     drawable.getOutline(outline);
@@ -60 +62
+ }
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix for qualified expressions (issue 205026874)`() {
        val input = arrayOf(
            javaSample("androidx.sample.appcompat.widget.ActionBarBackgroundDrawable"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:71: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mSplitBackground.getOutline(outline);
                                            ~~~~~~~~~~
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:76: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mBackground.getOutline(outline);
                                       ~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 71: Extract to static inner class:
@@ -71 +71
-                 mContainer.mSplitBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mSplitBackground, outline);
@@ -90 +90
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
+
@@ -91 +102
+ }
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 76: Extract to static inner class:
@@ -76 +76
-                 mContainer.mBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mBackground, outline);
@@ -90 +90
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
+
@@ -91 +102
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix includes fully qualified class name (issue 205035683, 236721202)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeMethodWithQualifiedClass"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeMethodWithQualifiedClass.java:40: Error: This call references a method added in API level 19; however, the containing class androidx.AutofixUnsafeMethodWithQualifiedClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return builder.setMediaSize(mediaSize);
                       ~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/androidx/AutofixUnsafeMethodWithQualifiedClass.java line 40: Extract to static inner class:
@@ -40 +40
+         return Api19Impl.setMediaSize(builder, mediaSize);
+     }
+ @RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static PrintAttributes.Builder setMediaSize(PrintAttributes.Builder builder, PrintAttributes.MediaSize mediaSize) {
@@ -42 +52
+
@@ -43 +54
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Auto-fix for unsafe method call on this`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallToThis")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallToThis.java:39: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            getClipToPadding();
            ~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallToThis.java:48: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            this.getClipToPadding();
                 ~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallToThis.java:57: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            super.getClipToPadding();
                  ~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallToThis.java line 39: Extract to static inner class:
@@ -39 +39
-             getClipToPadding();
+             Api21Impl.getClipToPadding(this);
@@ -60 +60
+ @androidx.annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
+
@@ -61 +72
+ }
Fix for src/androidx/AutofixUnsafeCallToThis.java line 48: Extract to static inner class:
@@ -48 +48
-             this.getClipToPadding();
+             Api21Impl.getClipToPadding((ViewGroup) this);
@@ -60 +60
+ @androidx.annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
+
@@ -61 +72
+ }
Fix for src/androidx/AutofixUnsafeCallToThis.java line 57: Extract to static inner class:
@@ -57 +57
-             super.getClipToPadding();
+             Api21Impl.getClipToPadding(super);
@@ -60 +60
+ @androidx.annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
+
@@ -61 +72
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for unsafe method call on cast object (issue 206111383)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallOnCast")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallOnCast.java:32: Error: This call references a method added in API level 28; however, the containing class androidx.AutofixUnsafeCallOnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            ((DisplayCutout) secretDisplayCutout).getSafeInsetTop();
                                                  ~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallOnCast.java line 32: Extract to static inner class:
@@ -32 +32
-             ((DisplayCutout) secretDisplayCutout).getSafeInsetTop();
+             Api28Impl.getSafeInsetTop((DisplayCutout) secretDisplayCutout);
@@ -35 +35
+ @androidx.annotation.RequiresApi(28)
+ static class Api28Impl {
+     private Api28Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static int getSafeInsetTop(DisplayCutout displayCutout) {
+         return displayCutout.getSafeInsetTop();
+     }
+
@@ -36 +47
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new return type (issue 214389795)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallWithImplicitReturnCast"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:36: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new AdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:44: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new AdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:52: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return Icon.createWithAdaptiveBitmap(null);
                    ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:60: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return Icon.createWithAdaptiveBitmap(null);
                    ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:68: Error: This call references a method added in API level 24; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        useStyle(new Notification.DecoratedCustomViewStyle());
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 36: Extract to static inner class:
@@ -36 +36
-         return new AdaptiveIconDrawable(null, null);
+         return Api26Impl.createAdaptiveIconDrawableReturnsDrawable(null, null);
@@ -77 +77
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Drawable createAdaptiveIconDrawableReturnsDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
+         return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
+     }
+
@@ -78 +89
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 44: Extract to static inner class:
@@ -44 +44
-         return new AdaptiveIconDrawable(null, null);
+         return Api26Impl.createAdaptiveIconDrawable(null, null);
@@ -77 +77
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static AdaptiveIconDrawable createAdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
+         return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
+     }
+
@@ -78 +89
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 52: Extract to static inner class:
@@ -52 +52
-         return Icon.createWithAdaptiveBitmap(null);
+         return Api26Impl.createWithAdaptiveBitmapReturnsObject(null);
@@ -77 +77
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static java.lang.Object createWithAdaptiveBitmapReturnsObject(android.graphics.Bitmap bits) {
+         return Icon.createWithAdaptiveBitmap(bits);
+     }
+
@@ -78 +89
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 60: Extract to static inner class:
@@ -60 +60
-         return Icon.createWithAdaptiveBitmap(null);
+         return Api26Impl.createWithAdaptiveBitmap(null);
@@ -77 +77
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Icon createWithAdaptiveBitmap(android.graphics.Bitmap bits) {
+         return Icon.createWithAdaptiveBitmap(bits);
+     }
+
@@ -78 +89
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 68: Extract to static inner class:
@@ -68 +68
-         useStyle(new Notification.DecoratedCustomViewStyle());
+         useStyle(Api24Impl.createDecoratedCustomViewStyleReturnsStyle());
@@ -77 +77
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Notification.Style createDecoratedCustomViewStyleReturnsStyle() {
+         return new Notification.DecoratedCustomViewStyle();
+     }
+
@@ -78 +89
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for constructor needs qualified class name (issue 244714253)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeConstructorQualifiedClass"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeConstructorQualifiedClass.java:32: Error: This call references a method added in API level 24; however, the containing class androidx.AutofixUnsafeConstructorQualifiedClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new Notification.DecoratedCustomViewStyle();
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeConstructorQualifiedClass.java line 32: Extract to static inner class:
@@ -32 +32
+         return Api24Impl.createDecoratedCustomViewStyle();
+     }
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Notification.DecoratedCustomViewStyle createDecoratedCustomViewStyle() {
@@ -34 +44
+
@@ -35 +46
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new parameter type (issue 266845827)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallWithImplicitParamCast"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallWithImplicitParamCast.java:34: Error: This call references a method added in API level 16; however, the containing class androidx.AutofixUnsafeCallWithImplicitParamCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        style.setBuilder(builder);
              ~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitParamCast.java:43: Error: This call references a method added in API level 20; however, the containing class androidx.AutofixUnsafeCallWithImplicitParamCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        builder.extend(extender);
                ~~~~~~
2 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallWithImplicitParamCast.java line 34: Extract to static inner class:
@@ -34 +34
-         style.setBuilder(builder);
+         Api16Impl.setBuilder((Notification.Style) style, builder);
@@ -45 +45
+ @RequiresApi(16)
+ static class Api16Impl {
+     private Api16Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setBuilder(Notification.Style style, Notification.Builder builder) {
+         style.setBuilder(builder);
+     }
+
@@ -46 +57
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitParamCast.java line 43: Extract to static inner class:
@@ -43 +43
-         builder.extend(extender);
+         Api20Impl.extend(builder, (Notification.Extender) extender);
@@ -45 +45
+ @RequiresApi(20)
+ static class Api20Impl {
+     private Api20Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Notification.Builder extend(Notification.Builder builder, Notification.Extender extender) {
+         return builder.extend(extender);
+     }
+
@@ -46 +57
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for method with varargs that are implicitly cast (issue 266845827)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java:35: Error: This call references a method added in API level 27; however, the containing class androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        adapter.setAutofillOptions();
                ~~~~~~~~~~~~~~~~~~
src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java:43: Error: This call references a method added in API level 27; however, the containing class androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        adapter.setAutofillOptions(vararg);
                ~~~~~~~~~~~~~~~~~~
src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java:52: Error: This call references a method added in API level 27; however, the containing class androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        adapter.setAutofillOptions(vararg1, vararg2, vararg3);
                ~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java line 35: Extract to static inner class:
@@ -35 +35
-         adapter.setAutofillOptions();
+         Api27Impl.setAutofillOptions(adapter);
@@ -54 +54
+ @RequiresApi(27)
+ static class Api27Impl {
+     private Api27Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setAutofillOptions(BaseAdapter baseAdapter, java.lang.CharSequence... options) {
+         baseAdapter.setAutofillOptions(options);
+     }
+
@@ -55 +66
+ }
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java line 43: Extract to static inner class:
@@ -43 +43
-         adapter.setAutofillOptions(vararg);
+         Api27Impl.setAutofillOptions(adapter, (java.lang.CharSequence) vararg);
@@ -54 +54
+ @RequiresApi(27)
+ static class Api27Impl {
+     private Api27Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setAutofillOptions(BaseAdapter baseAdapter, java.lang.CharSequence... options) {
+         baseAdapter.setAutofillOptions(options);
+     }
+
@@ -55 +66
+ }
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java line 52: Extract to static inner class:
@@ -52 +52
-         adapter.setAutofillOptions(vararg1, vararg2, vararg3);
+         Api27Impl.setAutofillOptions(adapter, (java.lang.CharSequence) vararg1, (java.lang.CharSequence) vararg2, (java.lang.CharSequence) vararg3);
@@ -54 +54
+ @RequiresApi(27)
+ static class Api27Impl {
+     private Api27Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setAutofillOptions(BaseAdapter baseAdapter, java.lang.CharSequence... options) {
+         baseAdapter.setAutofillOptions(options);
+     }
+
@@ -55 +66
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }
}
