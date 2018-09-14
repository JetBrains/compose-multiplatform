package org.jetbrains.kotlin.r4a.idea


import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

class KtxAttributeAutoParensHandlerTest : KotlinLightCodeInsightFixtureTestCase() {

    // context added to every test
    private val context = """
        fun Foo() {}
    """.trimIndent()

    fun test1() = doCharTypeTest(
        "<Foo a=b!<caret> />",
        '=',
        "<Foo a=(b!=<caret>) />"
    )
    fun test2() = doCharTypeTest(
        "<Foo a=b=<caret> />",
        '=',
        "<Foo a=(b==<caret>) />"
    )
    fun test3() = doCharTypeTest(
        "<Foo a=b =<caret> />",
        '=',
        "<Foo a=(b ==<caret>) />"
    )
    fun test4() = doCharTypeTest(
        "<Foo a=b !<caret> />",
        '=',
        "<Foo a=(b !=<caret>) />"
    )
    fun test5() = doCharTypeTest(
        "<Foo a = b !<caret> />",
        '=',
        "<Foo a = (b !=<caret>) />"
    )

    fun test6() = doCharTypeTest(
        "<Foo a=b<caret> />",
        '+',
        "<Foo a=(b+<caret>) />"
    )
    fun test7() = doCharTypeTest(
        "<Foo a=b <caret> />",
        '+',
        "<Foo a=(b +<caret>) />"
    )
    fun test8() = doCharTypeTest(
        "<Foo a=(b <caret>) />",
        '+',
        "<Foo a=(b +<caret>) />"
    )

    // if an unmatched paren is present, it doesn't auto-close it
    fun test9() = doCharTypeTest(
        "<Foo a=(b <caret> />",
        '+',
        "<Foo a=(b +<caret> />"
    )

    // properly handles parens in value expression
    fun test10() = doCharTypeTest(
        "<Foo a=b.foo() <caret> />",
        '+',
        "<Foo a=(b.foo() +<caret>) />"
    )

    fun test11() = doCharTypeTest(
        "<Foo a=b<caret>></Foo>",
        '-',
        "<Foo a=(b-<caret>)></Foo>"
    )

    fun test12() = doCharTypeTest(
        "<Foo a=b <caret>></Foo>",
        '-',
        "<Foo a=(b -<caret>)></Foo>"
    )

    fun test13() = doCharTypeTest(
        "<Foo a=b<caret>></Foo>",
        '>',
        "<Foo a=b><caret></Foo>"
    )

    // typing '>' will overwrite tag brackets if those are the next characters
    fun test14() = doCharTypeTest(
        "<Foo a=b <caret>></Foo>",
        '>',
        "<Foo a=b ><caret></Foo>"
    )

    fun test15() = doCharTypeTest(
        "<Foo a=b <caret>/>",
        '>',
        "<Foo a=(b ><caret>)/>"
    )

    fun test16() = doCharTypeTest(
        "<Foo a=b <caret> />",
        '>',
        "<Foo a=(b ><caret>) />"
    )

    fun test17() = doCharTypeTest(
        "<Foo a=b <caret> />",
        '/',
        "<Foo a=(b /<caret>) />"
    )

    // typing '/' will overwrite tag brackets if those are the next characters
    fun test18() = doCharTypeTest(
        "<Foo a=b <caret>/>",
        '/',
        "<Foo a=b /<caret>>"
    )

    fun test19() = listOf('+', '-', '%', '|', '&', '*', '<').forEach { c ->
        doCharTypeTest(
            "<Foo a=b <caret> />",
            c,
            "<Foo a=(b $c<caret>) />"
        )
    }

    fun test20() = doCharTypeTest(
        "<Foo a=b ?<caret> />",
        ':',
        "<Foo a=(b ?:<caret>) />"
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
