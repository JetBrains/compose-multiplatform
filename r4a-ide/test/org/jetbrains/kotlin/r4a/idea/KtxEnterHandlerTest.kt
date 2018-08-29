package org.jetbrains.kotlin.r4a.idea


import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

class KtxEnterHandlerTest : KotlinLightCodeInsightFixtureTestCase() {
    // context added to every test
    private val context = """
        fun Foo() {}
    """.trimIndent()

    fun testSelfClosingEnterInAttributes() = doFunTest(
        """
            <Foo<caret>/>
        """
        ,
        """
            <Foo
                <caret>
            />
        """
    )

    fun testEnterInAttributes() = doFunTest(
        """
            <Foo<caret>></Foo>
        """
        ,
        """
            <Foo
                <caret>
            ></Foo>
        """
    )

    fun testEnterBetweenOpenClose() = doFunTest(
        """
            <Foo><caret></Foo>
        """
        ,
        """
            <Foo>
                <caret>
            </Foo>
        """
    )

    fun testEnterBetweenOpenCloseWithParamList() = doFunTest(
        """
            <Foo> x -><caret></Foo>
        """
        ,
        """
            <Foo> x ->
                <caret>
            </Foo>
        """
    )

    fun doFunTest(before: String, after: String) {
        fun String.withFunContext(): String {
            val bodyText = "//---- [test]\n${this.trimIndent()}\n//---- [/test]"
            val contextText = "//----- [context]\n$context\n//---- [/context]"
            val withIndent = bodyText.prependIndent("    ")

            return "$contextText\nfun method() {\n$withIndent\n}"
        }

        doTest(before.withFunContext(), after.withFunContext())
    }

    fun doTest(before: String, after: String) {
        myFixture.configureByText(KotlinFileType.INSTANCE, before)
        myFixture.type('\n')
        myFixture.checkResult(after)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST
}
