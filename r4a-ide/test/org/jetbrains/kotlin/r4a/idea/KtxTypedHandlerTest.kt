package org.jetbrains.kotlin.r4a.idea


import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

class KtxTypedHandlerTest : KotlinLightCodeInsightFixtureTestCase() {
    // context added to every test
    private val context = """
        fun Foo() {}
    """.trimIndent()

    fun testOpenDivToSelfClosed() = doCharTypeTest(
        "<Foo <caret>",
        '/',
        "<Foo /<caret>>"
    )

    fun testOpenGtToMatchingClosed() = doCharTypeTest(
        "<Foo<caret>",
        '>',
        "<Foo><caret></Foo>"
    )

    fun testClosingDivToMatchingClosed() = doCharTypeTest(
        "<Foo><Bar /><<caret>",
        '/',
        "<Foo><Bar /></Foo><caret>"
    )

    fun testGtOnBrokenElWithClosingTagDoesntCreateNewOne() = doCharTypeTest(
        """
        <Foo<caret>
            <Bar />
        </Foo>
        """,
        '>',
        """
        <Foo><caret>
            <Bar />
        </Foo>
        """
    )

    fun testGtOnBrokenElWithoutClosingTagCreatesClosingTag() = doCharTypeTest(
        """
        <Foo<caret>
            <Bar />
        """,
        '>',
        """
        <Foo><caret></Foo>
            <Bar />
        """
    )

    fun testOverwriteForDivInSelfClosingTag() = doCharTypeTest(
        "<Foo<caret>/>",
        '/',
        "<Foo/<caret>>"
    )

    fun testOverwriteForGtInSelfClosingTag() = doCharTypeTest(
        "<Foo/<caret>>",
        '>',
        "<Foo/><caret>"
    )

    fun testOverwriteForGtInOpenCloseTag() = doCharTypeTest(
        "<Foo<caret>></Foo>",
        '>',
        "<Foo><caret></Foo>"
    )

    fun testOverwriteForGtInOpenCloseCloseTag() = doCharTypeTest(
        "<Foo></Foo<caret>>",
        '>',
        "<Foo></Foo><caret>"
    )

    fun testNoOverwriteForLt() = doCharTypeTest(
        "<Foo><caret></Foo>",
        '<',
        "<Foo><<caret></Foo>"
    )

    fun doCharTypeTest(before: String, c: Char, after: String) {
        fun String.withFunContext(): String {
            val bodyText = "//---- [test]\n${this.trimIndent()}\n//---- [/test]"
            val contextText = "//----- [context]\n$context\n//---- [/context]"
            val withIndent = bodyText.prependIndent("    ")

            return "$contextText\nfun method() {\n$withIndent\n}"
        }

        myFixture.configureByText(KotlinFileType.INSTANCE, before.withFunContext())
        myFixture.type(c)
        myFixture.checkResult(after.withFunContext())
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST
}
