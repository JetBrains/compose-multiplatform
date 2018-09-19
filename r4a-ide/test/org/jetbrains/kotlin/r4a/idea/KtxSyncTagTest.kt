package org.jetbrains.kotlin.r4a.idea

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase


class KtxSyncTagTest : KotlinLightCodeInsightFixtureTestCase() {

    fun testSelfClosingAtEnd() = doTest("<Foo<caret> />", "ab", "<Fooab />")
    fun testSelfClosingAtBeginning() = doTest("<<caret>Foo />", "ab", "<abFoo />")
    fun testSelfClosingAtMiddle() = doTest("<F<caret>oo />", "ab", "<Faboo />")

    fun testSimpleAtEnd() = doTest("<Foo<caret>></Foo>", "ab", "<Fooab></Fooab>")
    fun testSimpleAtBeginning() = doTest("<<caret>Foo></Foo>", "ab", "<abFoo></abFoo>")
    fun testSimpleAtMiddle() = doTest("<F<caret>oo></Foo>", "ab", "<Faboo></Faboo>")

    fun testSimpleWithChildren() = doTest("<Fo<caret>o><Bar /></Foo>", "ab", "<Foabo><Bar /></Foabo>")

    fun testSimpleWithChildrenOfSameName() = doTest("<Fo<caret>o><Foo /></Foo>", "ab", "<Foabo><Foo /></Foabo>")

    fun testCaretInClosingTag() = doTest("<Foo></F<caret>oo>", "ab", "<Faboo></Faboo>")

    fun testMultiCaret() = doTest(
        """
        <div<caret>></div>
        <div<caret>></div>
        """,
        "v",
        """
        <divv></divv>
        <divv></divv>
        """
    )

    fun testMultiCaretNested() = doTest(
        """
        <div<caret>>
            <div<caret>></div>
        </div>
        """,
        "v",
        """
        <divv>
            <divv></divv>
        </divv>
        """
    )

    fun testSelection() = doTest("<<selection>div</selection>></div>", "b", "<b></b>")

    fun testLastCharDeleted() = doTest("<div<caret>></div>", "\b\b\b", "<></>")

    fun testStartToEndAndEndToStart() {
        doTest("<div<caret>></div>", "v", "<divv></divv>")
        moveCaret(7)
        myFixture.type("\b")
        myFixture.checkResult("<div></div>".withFunContext())
    }

    fun testSpace() = doTest("<div<caret>></div>", " ", "<div ></div>")

    fun testRecommence() = doTest("<divv<caret>></div>", "\bd", "<divd></divd>")

    fun testDotQualified() = doTest("<Foo.bar.bam<caret>></Foo.bar.bam>", "ab", "<Foo.bar.bamab></Foo.bar.bamab>")
    fun testDotQualifiedMiddle() = doTest("<Foo.ba<caret>r.bam></Foo.bar.bam>", "ab", "<Foo.baabr.bam></Foo.baabr.bam>")


    fun moveCaret(relOffset: Int) {
        myFixture.editor.caretModel.allCarets.forEach {
            it.moveToOffset(it.offset + relOffset)
        }
    }

    fun String.withFunContext(): String {
        val bodyText = "//---- [test]\n${this.trimIndent()}\n//---- [/test]"
        val withIndent = bodyText.prependIndent("    ")

        return "fun method() {\n$withIndent\n}"
    }

    fun doTest(before: String, toType: String, after: String) {


        myFixture.configureByText(KotlinFileType.INSTANCE, before.withFunContext())
        myFixture.type(toType)
        myFixture.checkResult(after.withFunContext())
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST
}
