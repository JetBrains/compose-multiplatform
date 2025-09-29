package org.jetbrains.compose.intentions.utils.getRootPsiElement

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetRootPsiElementTest : LightJavaCodeInsightFixtureTestCase() {

    private val getRootElement = GetRootPsiElement()

    @Test
    fun `when a name reference expression is selected , but root is a property , the property should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
            val systemUiController = rememberSystemUiController()
        """.trimIndent()

        val file = ktPsiFactory.createFile(template)

        val property = file.lastChild as KtProperty

        val ktNameReferenceExpression = (property.lastChild as KtCallExpression).firstChild as KtNameReferenceExpression

        TestCase.assertEquals("rememberSystemUiController", ktNameReferenceExpression.text)

        TestCase.assertEquals(property, getRootElement.invoke(ktNameReferenceExpression))
    }

    @Test
    fun `when a name reference expression is selected, with a call expression as root, call expression should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
            fun Box(block:()->Unit) {
                
            }
            
            fun OuterComposable() {
                Box() { 
                 
                }
            }
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val ktNamedFunction = file.lastChild as KtNamedFunction

        val callExpression = ktNamedFunction.lastChild.children.find { it is KtCallExpression }!!
        val ktNameReferenceExpression = callExpression.firstChild as KtNameReferenceExpression

        TestCase.assertEquals("Box", ktNameReferenceExpression.text)

        TestCase.assertEquals(
            callExpression,
            getRootElement.invoke(ktNameReferenceExpression)
        )
    }

    @Test
    fun `when an argument list element is selected, with a call expression as root, call expression should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
            @Composable
            fun Box(block:()->Unit) {
                
            }
            
            fun OuterComposable() {
                // Argument List Element 
                // |
                // v
                Box() {
                 
                }
            }
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val ktNamedFunction = file.lastChild as KtNamedFunction

        val callExpression = ktNamedFunction.lastChild.children.find { it is KtCallExpression }!!

        val argumentListElement = callExpression.firstChild.nextSibling as KtValueArgumentList

        TestCase.assertEquals("()", argumentListElement.text)

        TestCase.assertEquals(
            callExpression,
            getRootElement.invoke(argumentListElement)
        )
    }

    @Test
    fun `when a name reference expression is selected, with a delegated property as root, property should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
           var isComposable by remember {
               true
            }
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val property = file.lastChild as KtProperty

        val referenceExpression = property.lastChild.lastChild.firstChild as KtNameReferenceExpression

        TestCase.assertEquals("remember", referenceExpression.text)

        TestCase.assertEquals(
            property,
            getRootElement.invoke(referenceExpression)
        )
    }

    @Test
    fun `when a name reference expression with dot reference expression is selected, with a delegated property as root, property should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
          val repeatingAnimation = rememberInfiniteTransition()

          val offset by repeatingAnimation.animateFloat(
              0f,
              -20f,
              infiniteRepeatable(
                  repeatMode = RepeatMode.Reverse,
                  animation = tween(
                      durationMillis = 1000,
                      easing = LinearEasing
                  )
              )
          )
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val property = file.lastChild as KtProperty

        val dotQualifiedExpression = property.lastChild.lastChild as KtDotQualifiedExpression

        val referenceExpression = dotQualifiedExpression.lastChild.firstChild as KtNameReferenceExpression

        TestCase.assertEquals("animateFloat", referenceExpression.text)

        TestCase.assertEquals(
            property,
            getRootElement.invoke(referenceExpression)
        )
    }

    @Test
    fun `when a name reference expression with dot reference expression is selected, with a property and dot qualified expression as root, property should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
          val repeatingAnimation = rememberInfiniteTransition()

          val offset = repeatingAnimation.animateFloat(
              0f,
              -20f,
              infiniteRepeatable(
                  repeatMode = RepeatMode.Reverse,
                  animation = tween(
                      durationMillis = 1000,
                      easing = LinearEasing
                  )
              )
          )
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val property = file.lastChild as KtProperty

        val dotQualifiedExpression = property.lastChild as KtDotQualifiedExpression

        val referenceExpression = dotQualifiedExpression.lastChild.firstChild as KtNameReferenceExpression

        TestCase.assertEquals("animateFloat", referenceExpression.text)

        TestCase.assertEquals(
            property,
            getRootElement.invoke(referenceExpression)
        )
    }

    @Test
    fun `when a dot qualified expression is selected, with a delegated property as root, property should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
          val repeatingAnimation = rememberInfiniteTransition()

          val offset by repeatingAnimation.animateFloat(
              0f,
              -20f,
              infiniteRepeatable(
                  repeatMode = RepeatMode.Reverse,
                  animation = tween(
                      durationMillis = 1000,
                      easing = LinearEasing
                  )
              )
          )
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val property = file.lastChild as KtProperty

        val dotQualifiedExpression = property.lastChild.lastChild as KtDotQualifiedExpression

        TestCase.assertEquals(
            """
            repeatingAnimation.animateFloat(
              0f,
              -20f,
              infiniteRepeatable(
                  repeatMode = RepeatMode.Reverse,
                  animation = tween(
                      durationMillis = 1000,
                      easing = LinearEasing
                  )
              )
          )
        """.trimIndent().trim(), dotQualifiedExpression.text
        )

        TestCase.assertEquals(
            property,
            getRootElement.invoke(dotQualifiedExpression)
        )
    }

    @Test
    fun `when a dot qualified expression is selected, with a property as root, property should be returned`() {
        val ktPsiFactory = KtPsiFactory(project)

        @Language("Kotlin")
        val template = """
          val repeatingAnimation = rememberInfiniteTransition()

          val offset = repeatingAnimation.animateFloat(
              0f,
              -20f,
              infiniteRepeatable(
                  repeatMode = RepeatMode.Reverse,
                  animation = tween(
                      durationMillis = 1000,
                      easing = LinearEasing
                  )
              )
          )
        """.trimIndent().trim()

        val file = ktPsiFactory.createFile(template)

        val property = file.lastChild as KtProperty

        val dotQualifiedExpression = property.lastChild as KtDotQualifiedExpression

        TestCase.assertEquals(
            """
            repeatingAnimation.animateFloat(
              0f,
              -20f,
              infiniteRepeatable(
                  repeatMode = RepeatMode.Reverse,
                  animation = tween(
                      durationMillis = 1000,
                      easing = LinearEasing
                  )
              )
          )
        """.trimIndent().trim(), dotQualifiedExpression.text
        )

        TestCase.assertEquals(
            property,
            getRootElement.invoke(dotQualifiedExpression)
        )
    }
}
