/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin

import org.intellij.lang.annotations.Language
import org.junit.Test

class DecoyTests : ComposeIrTransformTest() {

    @Test
    fun testWhileWithKey() = verifyDecoys(
        source = """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """.trimIndent(),
        """
            @Decoy(targetName = "Example%composable", signature = "", "Example%composable", "4127392143323184578", "0")
            fun Example(x: Int) {
              return illegalDecoyCallException("Example")
            }
            @NonRestartableComposable
            @Composable
            @DecoyImplementation(name = "Example%composable", id = 1540031808018075376L)
            fun Example%composable(x: Int, %composer: Composer?, %changed: Int) {
              var %composer = %composer
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example%composable):Test.kt")
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                sourceInformation(%composer, "<A()>")
                A%composable(%composer, 0)
                %composer.endMovableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """.trimIndent()
    )

    @Test
    fun testDecoysForTopLevelComposable() =
        verifyDecoys(
            """
                @NonRestartableComposable
                @Composable fun myFunction() { }
            """.trimIndent(),
            """
                @Decoy(targetName = "myFunction%composable", signature = "", "myFunction%composable", "6708958259269563382", "0")
                fun myFunction() {
                  return illegalDecoyCallException("myFunction")
                }
                @NonRestartableComposable
                @Composable
                @DecoyImplementation(name = "myFunction%composable", id = 6724225025824466888L)
                fun myFunction%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(myFunction%composable):Test.kt")
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysForMethodComposable() =
        verifyDecoys(
            """
                @Stable
                class SomeClass {
                  @NonRestartableComposable
                  @Composable fun myFunction() { }
                }
            """.trimIndent(),
            """
                @Stable
                class SomeClass {
                  @Decoy(targetName = "myFunction%composable", signature = "", "SomeClass.myFunction%composable", "6708958259269563382", "0")
                  fun myFunction() {
                    return illegalDecoyCallException("myFunction")
                  }
                  @NonRestartableComposable
                  @Composable
                  @DecoyImplementation(name = "myFunction%composable", id = 6724225025824466888L)
                  fun myFunction%composable(%composer: Composer?, %changed: Int) {
                    var %composer = %composer
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(myFunction%composable):Test.kt")
                    %composer.endReplaceableGroup()
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysForPropertyGetterComposable() =
        verifyDecoys(
            """
                val someProperty: String
                    @Composable get() = ""
            """.trimIndent(),
            """
                val someProperty: String
                  @Decoy(targetName = "%get-someProperty%%composable", signature = "", "%get-someProperty%%composable", "-1758808480827937492", "0")
                  get() {
                    return illegalDecoyCallException("<get-someProperty>")
                  }
                @Composable
                @DecoyImplementation(name = "%get-someProperty%%composable", id = -4577413779453222054L)
                fun %get-someProperty%%composable(%composer: Composer?, %changed: Int): String {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(%get-someProperty%%composable):Test.kt")
                  val tmp0 = ""
                  %composer.endReplaceableGroup()
                  return tmp0
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysFunctionCallSubstitution() =
        verifyDecoys(
            """
                @NonRestartableComposable
                @Composable fun myFunction() {
                    A()
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "myFunction%composable", signature = "", "myFunction%composable", "6708958259269563382", "0")
                fun myFunction() {
                  return illegalDecoyCallException("myFunction")
                }
                @NonRestartableComposable
                @Composable
                @DecoyImplementation(name = "myFunction%composable", id = 6724225025824466888L)
                fun myFunction%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(myFunction%composable)<A()>:Test.kt")
                  A%composable(%composer, 0)
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysPropertyGetterSubstitution() =
        verifyDecoys(
            """
                val someProperty: String @Composable get() = ""

                @NonRestartableComposable
                @Composable fun myFunction() {
                    println(someProperty)
                }
            """.trimIndent(),
            """
                val someProperty: String
                  @Decoy(targetName = "%get-someProperty%%composable", signature = "", "%get-someProperty%%composable", "-1758808480827937492", "0")
                  get() {
                    return illegalDecoyCallException("<get-someProperty>")
                  }
                @Decoy(targetName = "myFunction%composable", signature = "", "myFunction%composable", "6708958259269563382", "0")
                fun myFunction() {
                  return illegalDecoyCallException("myFunction")
                }
                @Composable
                @DecoyImplementation(name = "%get-someProperty%%composable", id = -4577413779453222054L)
                fun %get-someProperty%%composable(%composer: Composer?, %changed: Int): String {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(%get-someProperty%%composable):Test.kt")
                  val tmp0 = ""
                  %composer.endReplaceableGroup()
                  return tmp0
                }
                @NonRestartableComposable
                @Composable
                @DecoyImplementation(name = "myFunction%composable", id = 6724225025824466888L)
                fun myFunction%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(myFunction%composable)<somePr...>:Test.kt")
                  println(%get-someProperty%%composable(%composer, 0))
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysTypeParameter() =
        verifyDecoys(
            """
                @Composable fun <T> myFunction(param: Boolean, value: T): T? {
                    if (param) {
                        return value
                    } else {
                        return null
                    }
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "myFunction%composable", signature = "", "myFunction%composable", "6160000889274225079", "0")
                fun <T> myFunction(param: Boolean, value: T): T? {
                  return illegalDecoyCallException("myFunction")
                }
                @Composable
                @DecoyImplementation(name = "myFunction%composable", id = 30396251832882702L)
                fun <T> myFunction%composable(param: Boolean, value: T, %composer: Composer?, %changed: Int): T? {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(myFunction%composable):Test.kt")
                  if (param) {
                    val tmp0_return = value
                    %composer.endReplaceableGroup()
                    return tmp0_return
                  } else {
                    val tmp1_return = null
                    %composer.endReplaceableGroup()
                    return tmp1_return
                  }
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysEnclosingClassTypeParameter() =
        verifyDecoys(
            """
                @Stable
                class Parent<T> {
                    @Composable fun myFunction(param: Boolean, value: T): T? {
                        if (param) {
                            return value
                        } else {
                            return null
                        }
                    }
                }
            """.trimIndent(),
            """
                @Stable
                class Parent<T>  {
                  @Decoy(targetName = "myFunction%composable", signature = "", "Parent.myFunction%composable", "8115089805078599247", "0")
                  fun myFunction(param: Boolean, value: T): T? {
                    return illegalDecoyCallException("myFunction")
                  }
                  @Composable
                  @DecoyImplementation(name = "myFunction%composable", id = -7842536290152739736L)
                  fun myFunction%composable(param: Boolean, value: T, %composer: Composer?, %changed: Int): T? {
                    var %composer = %composer
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(myFunction%composable):Test.kt")
                    if (param) {
                      val tmp0_return = value
                      %composer.endReplaceableGroup()
                      return tmp0_return
                    } else {
                      val tmp1_return = null
                      %composer.endReplaceableGroup()
                      return tmp1_return
                    }
                    %composer.endReplaceableGroup()
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysPropertyTypeParameter() =
        verifyDecoys(
            """
                @Stable
                class Parent<T> {
                    val myProperty: T? @Composable get() = null
                }
            """.trimIndent(),
            """
                @Stable
                class Parent<T>  {
                  val myProperty: T?
                    @Decoy(targetName = "%get-myProperty%%composable", signature = "", "Parent.%get-myProperty%%composable", "-3421074922056462185", "0")
                    get() {
                      return illegalDecoyCallException("<get-myProperty>")
                    }
                  @Composable
                  @DecoyImplementation(name = "%get-myProperty%%composable", id = 2813712154409589600L)
                  fun %get-myProperty%%composable(%composer: Composer?, %changed: Int): T? {
                    var %composer = %composer
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(%get-myProperty%%composable):Test.kt")
                    val tmp0 = null
                    %composer.endReplaceableGroup()
                    return tmp0
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysInterface() =
        verifyDecoys(
            """
                interface IntroFace<T> {
                    @Composable
                    fun a(): T
                }
            """.trimIndent(),
            """
                interface IntroFace<T>  {
                  @Decoy(targetName = "a%composable", signature = "", "IntroFace.a%composable", "-1346198135366331071", "0")
                  abstract fun a(): T
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                  abstract fun a%composable(%composer: Composer?, %changed: Int): T
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysTypeParameterInterface() =
        verifyDecoys(
            """
                interface IntroFace {
                    @Composable
                    fun <T> a(param: T): T
                }
            """.trimIndent(),
            """
                interface IntroFace {
                  @Decoy(targetName = "a%composable", signature = "", "IntroFace.a%composable", "2986573616514828521", "0")
                  abstract fun <T> a(param: T): T
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = 788770056597229566L)
                  abstract fun <T> a%composable(param: T, %composer: Composer?, %changed: Int): T
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysInterfaceOverride() =
        verifyDecoys(
            """
                interface IntroFace {
                    @Composable
                    fun a(): Int
                }

                @Stable
                class Test : IntroFace {
                    @Composable
                    override fun a(): Int = 0
                }
            """.trimIndent(),
            """
                interface IntroFace {
                  @Decoy(targetName = "a%composable", signature = "", "IntroFace.a%composable", "-1346198135366331071", "0")
                  abstract fun a(): Int
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                  abstract fun a%composable(%composer: Composer?, %changed: Int): Int
                }
                @Stable
                class Test : IntroFace {
                  @Decoy(targetName = "a%composable", signature = "", "Test.a%composable", "-1346198135366331071", "0")
                  override fun a(): Int {
                    return illegalDecoyCallException("a")
                  }
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                  override fun a%composable(%composer: Composer?, %changed: Int): Int {
                    var %composer = %composer
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(a%composable):Test.kt")
                    val tmp0 = 0
                    %composer.endReplaceableGroup()
                    return tmp0
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysInterfaceOverrideCall() =
        verifyDecoys(
            """
                interface IntroFace {
                    @Composable
                    fun a(): Int
                }
                @Stable
                class Test : IntroFace {
                    @Composable
                    override fun a(): Int = 0

                    @NonRestartableComposable
                    @Composable
                    fun b() {
                        a()
                    }
                }
            """.trimIndent(),
            """
                interface IntroFace {
                  @Decoy(targetName = "a%composable", signature = "", "IntroFace.a%composable", "-1346198135366331071", "0")
                  abstract fun a(): Int
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                  abstract fun a%composable(%composer: Composer?, %changed: Int): Int
                }
                @Stable
                class Test : IntroFace {
                  @Decoy(targetName = "a%composable", signature = "", "Test.a%composable", "-1346198135366331071", "0")
                  override fun a(): Int {
                    return illegalDecoyCallException("a")
                  }
                  @Decoy(targetName = "b%composable", signature = "", "Test.b%composable", "-5933976465021585245", "0")
                  fun b() {
                    return illegalDecoyCallException("b")
                  }
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                  override fun a%composable(%composer: Composer?, %changed: Int): Int {
                    var %composer = %composer
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(a%composable):Test.kt")
                    val tmp0 = 0
                    %composer.endReplaceableGroup()
                    return tmp0
                  }
                  @NonRestartableComposable
                  @Composable
                  @DecoyImplementation(name = "b%composable", id = 4789657038926421504L)
                  fun b%composable(%composer: Composer?, %changed: Int) {
                    var %composer = %composer
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(b%composable)<a()>:Test.kt")
                    a%composable(%composer, 0b1110 and %changed)
                    %composer.endReplaceableGroup()
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoysInterfaceCall() =
        verifyDecoys(
            """
                interface IntroFace {
                    @Composable
                    fun <T> a(): T?
                }

                @NonRestartableComposable
                @Composable
                fun a(value: IntroFace) {
                    value.a<Int>()
                }
            """.trimIndent(),
            """
                interface IntroFace {
                  @Decoy(targetName = "a%composable", signature = "", "IntroFace.a%composable", "5317272182441714074", "0")
                  abstract fun <T> a(): T?
                  @Composable
                  @DecoyImplementation(name = "a%composable", id = -4852060153575466125L)
                  abstract fun <T> a%composable(%composer: Composer?, %changed: Int): T?
                }
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "-7370519321062369770", "0")
                fun a(value: IntroFace) {
                  return illegalDecoyCallException("a")
                }
                @NonRestartableComposable
                @Composable
                @DecoyImplementation(name = "a%composable", id = 7403804501910746229L)
                fun a%composable(value: IntroFace, %composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(a%composable)<a<Int>...>:Test.kt")
                  value.a%composable(%composer, 0b1110 and %changed)
                  %composer.endReplaceableGroup()
                }
            """.trimIndent(),
        )

    @Test
    fun testDecoyLinking() =
        verifyDecoys(
            """
                import androidx.compose.runtime.currentComposer
                import androidx.compose.runtime.remember
                import androidx.compose.runtime.ExperimentalComposeApi

                @OptIn(ExperimentalComposeApi::class)
                @Composable fun a() {
                    remember(currentComposer.composition) { }
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "-1346198135366331071", "0")
                fun a() {
                  return illegalDecoyCallException("a")
                }
                @OptIn(markerClass = ExperimentalComposeApi::class)
                @Composable
                @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                fun a%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(a%composable)<rememb...>:Test.kt")
                  if (%changed !== 0 || !%composer.skipping) {
                    remember%composable(%composer.composition, {
                    }, %composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    a%composable(%composer, %changed or 0b0001)
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoySameName() =
        verifyDecoys(
            """
                @Composable
                @NonRestartableComposable
                fun a() { }

                @Composable
                @NonRestartableComposable
                fun a(param: Int) { }

                @Composable
                @NonRestartableComposable
                fun test() {
                    a()
                    a(param = 0)
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "-1346198135366331071", "0")
                fun a() {
                  return illegalDecoyCallException("a")
                }
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "7521638925490819195", "0")
                fun a(param: Int) {
                  return illegalDecoyCallException("a")
                }
                @Decoy(targetName = "test%composable", signature = "", "test%composable", "7492538349555837981", "0")
                fun test() {
                  return illegalDecoyCallException("test")
                }
                @Composable
                @NonRestartableComposable
                @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                fun a%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(a%composable):Test.kt")
                  %composer.endReplaceableGroup()
                }
                @Composable
                @NonRestartableComposable
                @DecoyImplementation(name = "a%composable", id = 6611953703272633265L)
                fun a%composable(param: Int, %composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(a%composable):Test.kt")
                  %composer.endReplaceableGroup()
                }
                @Composable
                @NonRestartableComposable
                @DecoyImplementation(name = "test%composable", id = 6620506149988718649L)
                fun test%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(test%composable)<a()>,<a(para...>:Test.kt")
                  a%composable(%composer, 0)
                  a%composable(0, %composer, 0b0110)
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )

    @Test
    fun testDecoyWithGenericComposableParams() =
        verifyDecoys(
            """
                @NonRestartableComposable
                @Composable
                fun a(f: @Composable () -> Unit) {
                  return f()
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "-1960268328678034754", "0")
                fun a(f: @[Composable] Function0<Unit>) {
                  return illegalDecoyCallException("a")
                }
                @NonRestartableComposable
                @Composable
                @DecoyImplementation(name = "a%composable", id = -3955326755550631944L)
                fun a%composable(f: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(a%composable)<f()>:Test.kt")
                  f(%composer, 0b1110 and %changed)
                  %composer.endReplaceableGroup()
                }
            """.trimIndent(),
        )

    @Test
    fun testDecoyWithReturnComposable() =
        verifyDecoys(
            """
                @Composable
                fun a(): @Composable () -> Unit {
                  return { }
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "-1346198135366331071", "0")
                fun a(): @[Composable] Function0<Unit> {
                  return illegalDecoyCallException("a")
                }
                internal object ComposableSingletons%TestKt {
                  val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                    var %composer = %composer
                    sourceInformation(%composer, "C:Test.kt")
                    if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                      Unit
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                  ::invoke
                }
                @Composable
                @DecoyImplementation(name = "a%composable", id = -4432112437378250461L)
                fun a%composable(%composer: Composer?, %changed: Int): Function2<Composer, Int, Unit> {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(a%composable):Test.kt")
                  val tmp0 = ComposableSingletons%TestKt.lambda-1
                  %composer.endReplaceableGroup()
                  return tmp0
                }
            """.trimIndent()
        )

    @Test
    fun testDecoyReceiverComposable() =
        verifyDecoys(
            """
                @NonRestartableComposable
                @Composable
                fun (@Composable () -> Unit).a() { }

                @Composable
                fun b() {
                    val something: (@Composable () -> Unit) = {
                    }
                    something.a()
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "-5141356682581289801", "0")
                fun @[Composable] Function0<Unit>.a() {
                  return illegalDecoyCallException("a")
                }
                @Decoy(targetName = "b%composable", signature = "", "b%composable", "-5933976465021585245", "0")
                fun b() {
                  return illegalDecoyCallException("b")
                }
                internal object ComposableSingletons%TestKt {
                  val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                    var %composer = %composer
                    sourceInformation(%composer, "C:Test.kt")
                    if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                      Unit
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                  ::invoke
                }
                @NonRestartableComposable
                @Composable
                @DecoyImplementation(name = "a%composable", id = -3637875817732186957L)
                fun Function2<Composer, Int, Unit>.a%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(a%composable):Test.kt")
                  %composer.endReplaceableGroup()
                }
                @Composable
                @DecoyImplementation(name = "b%composable", id = 4789657038926421504L)
                fun b%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(b%composable)<a()>:Test.kt")
                  if (%changed !== 0 || !%composer.skipping) {
                    val something = ComposableSingletons%TestKt.lambda-1
                    something.a%composable(%composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    b%composable(%composer, %changed or 0b0001)
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoyComposableInProperty() =
        verifyDecoys(
            """
                private var EmptyComposable: @Composable () -> Unit = {
                }

                fun emptyContent() = EmptyComposable
            """.trimIndent(),
            """
                var EmptyComposable: Function2<Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
                fun emptyContent(): Function2<Composer, Int, Unit> {
                  return EmptyComposable
                }
                internal object ComposableSingletons%TestKt {
                  val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                    var %composer = %composer
                    sourceInformation(%composer, "C:Test.kt")
                    if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                      Unit
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                  ::invoke
                }
                @DecoyImplementation(name = "%set-EmptyComposable%%composable", id = 2L)
                private fun %set-EmptyComposable%%composable(set-?%: Function2<Composer, Int, Unit>) {
                  .EmptyComposable = set-?%
                }
            """.trimIndent()
        )

    @Test
    fun testDecoyVarargParam() =
        verifyDecoys(
            """
                @Composable
                fun onCommit(vararg inputs: Any?) {
                    onCommit(inputs)
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "onCommit%composable", signature = "", "onCommit%composable", "5705951862413367936", "0")
                fun onCommit(inputs: Array<out Any?>) {
                  return illegalDecoyCallException("onCommit")
                }
                @Composable
                @DecoyImplementation(name = "onCommit%composable", id = 3418784457683034909L)
                fun onCommit%composable(inputs: Array<out Any?>, %composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(onCommit%composable)<onComm...>:Test.kt")
                  val %dirty = %changed
                  %composer.startReplaceableGroup(inputs.size)
                  val tmp0_iterator = inputs.iterator()
                  while (tmp0_iterator.hasNext()) {
                    val value = tmp0_iterator.next()
                    %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0
                  }
                  %composer.endReplaceableGroup()
                  if (%dirty and 0b1110 === 0) {
                    %dirty = %dirty or 0b0010
                  }
                  if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                    onCommit%composable(inputs, %composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    onCommit%composable(*inputs, %composer, %changed or 0b0001)
                  }
                }
            """.trimIndent()
        )

    @Test
    fun testDecoyLambdaMemoization() =
        verifyDecoys(
            """
                @Composable
                inline fun <T> emit(f: @Composable () -> T) {
                    f()
                }

                @Composable
                fun Something() {
                    emit {
                        Something()
                    }
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "emit%composable", signature = "", "emit%composable", "4159217327243672831", "0")
                fun <T> emit(f: @[Composable] Function0<T>) {
                  return illegalDecoyCallException("emit")
                }
                @Decoy(targetName = "Something%composable", signature = "", "Something%composable", "-5284146267129462034", "0")
                fun Something() {
                  return illegalDecoyCallException("Something")
                }
                @Composable
                @DecoyImplementation(name = "emit%composable", id = -789693119272983178L)
                fun <T> emit%composable(f: Function2<Composer, Int, T>, %composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(emit%composable)<f()>:Test.kt")
                  f(%composer, 0b1110 and %changed)
                  %composer.endReplaceableGroup()
                }
                @Composable
                @DecoyImplementation(name = "Something%composable", id = 6101469802196086059L)
                fun Something%composable(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Something%composable)<emit>:Test.kt")
                  if (%changed !== 0 || !%composer.skipping) {
                    emit%composable({ %composer: Composer?, %changed: Int ->
                      var %composer = %composer
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "C<Someth...>:Test.kt")
                      if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                        Something%composable(%composer, 0)
                      } else {
                        %composer.skipToGroupEnd()
                      }
                      %composer.endReplaceableGroup()
                    }, %composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Something%composable(%composer, %changed or 0b0001)
                  }
                }
            """.trimIndent(),
        )

    @Test
    fun testDecoyConstructor() =
        verifyDecoys(
            """
                class Test(param: @Composable () -> Unit) {
                    constructor(int: Int, param: @Composable () -> Unit) : this(param)
                }

                fun test() {
                    Test(0) {}
                }
            """.trimIndent(),
            """
                @StabilityInferred(parameters = 0)
                class Test(param: @[Composable] Function0<Unit>) {
                  @Decoy(targetName = "%init%%composable", signature = "", "Test.<init>", "-5419686343446778487", "0")
                  constructor(int: Int, param: @[Composable] Function0<Unit>){
                    return illegalDecoyCallException("<init>")
                  }
                  val %stable: Int = 0
                  @DecoyImplementation(name = "%init%%composable", id = -955554854620973372L)
                  constructor(param: Function2<Composer, Int, Unit>){
                    ctor<Any>()
                    init<Test>()
                  }
                  @DecoyImplementation(name = "%init%%composable", id = 6138638643038331420L)
                  constructor(int: Int, param: Function2<Composer, Int, Unit>){
                    ctor<Test>(param)
                  }
                }
                fun test() {
                  Test(0, ComposableSingletons%TestKt.lambda-1)
                }
                internal object ComposableSingletons%TestKt {
                  val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                    var %composer = %composer
                    sourceInformation(%composer, "C:Test.kt")
                    if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                      Unit
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                  ::invoke
                }
            """.trimIndent(),
            dumpTree = false
        )

    @Test
    fun testDecoyNestedComposable() =
        verifyDecoys(
            """
                fun a(list: List<@Composable () -> Unit>) {
                    a(list)
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "4403675071889283743", "0")
                fun a(list: List<@[Composable] Function0<Unit>>) {
                  return illegalDecoyCallException("a")
                }
                @DecoyImplementation(name = "a%composable", id = 8022514244444091105L)
                fun a%composable(list: List<Function2<Composer, Int, Unit>>) {
                  a%composable(list)
                }
            """.trimIndent()
        )

    @Test
    fun testDecoyInlineFunctionReference() =
        verifyDecoys(
            """
                fun a(f: @Composable () -> Unit) {
                    f.also {
                        println("${'$'}it")
                    }
                }
            """.trimIndent(),
            """
                @Decoy(targetName = "a%composable", signature = "", "a%composable", "1543565005338797865", "0")
                fun a(f: @[Composable] Function0<Unit>) {
                  return illegalDecoyCallException("a")
                }
                @DecoyImplementation(name = "a%composable", id = -3955326755550631944L)
                fun a%composable(f: Function2<Composer, Int, Unit>) {
                  f.also { it: Function2<Composer, Int, Unit> ->
                    println("%it")
                  }
                }
            """.trimIndent(),
        )

    @Test
    fun testDecoyComposableWithDefaultParameter() = verifyDecoys(
        """
            @Composable
            fun Something(i: Int = 5) {}

            @Composable
            fun callSomething() {
                Something()
                Something(100)
            }
        """.trimIndent(),
        """
            @Decoy(targetName = "Something%composable", signature = "", "Something%composable", "-9095560056427050117", "0")
            fun Something(i: Int) {
              return illegalDecoyCallException("Something")
            }
            @Decoy(targetName = "callSomething%composable", signature = "", "callSomething%composable", "1623959014857491899", "0")
            fun callSomething() {
              return illegalDecoyCallException("callSomething")
            }
            @Composable
            @DecoyImplementation(name = "Something%composable", id = -2550755975037578980L)
            fun Something%composable(i: Int, %composer: Composer?, %changed: Int, %default: Int) {
              var i = i
              var %composer = %composer
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Something%composable):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  i = 5
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Something%composable(i, %composer, %changed or 0b0001, %default)
              }
            }
            @Composable
            @DecoyImplementation(name = "callSomething%composable", id = -9102957534442699635L)
            fun callSomething%composable(%composer: Composer?, %changed: Int) {
              var %composer = %composer
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(callSomething%composable)<Someth...>,<Someth...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                Something%composable(0, %composer, 0, 0b0001)
                Something%composable(100, %composer, 0b0110, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                callSomething%composable(%composer, %changed or 0b0001)
              }
            }
        """.trimIndent()
    )

    @Test
    fun testDecoyComposableWithDefaultLambda() = verifyDecoys(
        """
            @Composable
            fun Something(f: @Composable (Int) -> Unit = {}) {
                f(0)
            }
        """.trimIndent(),
        """
            @Decoy(targetName = "Something%composable", signature = "", "Something%composable", "-6157974296146815163", "0")
            fun Something(f: @[Composable] Function1<Int, Unit>) {
              return illegalDecoyCallException("Something")
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function3<Int, Composer, Int, Unit> = composableLambdaInstance(<>, false) { it: Int, %composer: Composer?, %changed: Int ->
                var %composer = %composer
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b01010001 xor 0b00010000 !== 0 || !%composer.skipping) {
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              ::invoke
            }
            @Composable
            @DecoyImplementation(name = "Something%composable", id = -8233454269585783755L)
            fun Something%composable(f: Function3<Int, Composer, Int, Unit>?, %composer: Composer?, %changed: Int, %default: Int) {
              var f = f
              var %composer = %composer
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Something%composable)<f(0)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(f)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  %composer.startDefaults()
                  if (%default and 0b0001 !== 0) {
                    f = ComposableSingletons%TestKt.lambda-1
                    %dirty = %dirty and 0b1110.inv()
                  }
                  %composer.endDefaults()
                } else {
                  %composer.skipCurrentGroup()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                f(0, %composer, 0b0110 or 0b01110000 and %dirty shl 0b0011)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Something%composable(f, %composer, %changed or 0b0001, %default)
              }
            }
        """.trimIndent()
    )

    @Test
    fun testDecoyAnonymousObjectInterfaceImpl() = verifyDecoys(
        """
            interface Something {
                @Composable fun test()
            }
            val field = object : Something {
                @Composable
                override fun test() {}
            }
        """.trimIndent(),
        """
            interface Something {
              @Decoy(targetName = "test%composable", signature = "", "Something.test%composable", "7492538349555837981", "0")
              abstract fun test()
              @Composable
              @DecoyImplementation(name = "test%composable", id = 6620506149988718649L)
              abstract fun test%composable(%composer: Composer?, %changed: Int)
            }
            val field: Something = object : Something {
              @Decoy(targetName = "test%composable", signature = )
              override fun test() {
                return illegalDecoyCallException("test")
              }
              @Composable
              @DecoyImplementation(name = "test%composable", id = 6620506149988718649L)
              override fun test%composable(%composer: Composer?, %changed: Int) {
                var %composer = %composer
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C(test%composable):Test.kt")
                %composer.endReplaceableGroup()
              }
            }
        """.trimIndent()
    )

    @Test
    fun testDecoyAnonymousObject() = verifyDecoys(
        """
            val field = object {
                @Composable
                fun test() {}
            }
        """.trimIndent(),
        """
            val field: Any = object {
              @Composable
              fun test(%composer: Composer?, %changed: Int) {
                var %composer = %composer
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C(test):Test.kt")
                %composer.endReplaceableGroup()
              }
            }
        """.trimIndent()
    )

    @Test
    fun testDecoyLocalFunction() = verifyDecoys(
        """
            @Composable
            fun test() {
                @Composable
                fun inner() { }
                inner()
            }
        """.trimIndent(),
        """
            @Decoy(targetName = "test%composable", signature = "", "test%composable", "7492538349555837981", "0")
            fun test() {
              return illegalDecoyCallException("test")
            }
            @Composable
            @DecoyImplementation(name = "test%composable", id = 6620506149988718649L)
            fun test%composable(%composer: Composer?, %changed: Int) {
              var %composer = %composer
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test%composable)<inner(...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                @Composable
                fun inner(%composer: Composer?, %changed: Int) {
                  var %composer = %composer
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(inner):Test.kt")
                  %composer.endReplaceableGroup()
                }
                inner(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test%composable(%composer, %changed or 0b0001)
              }
            }
        """.trimIndent()
    )

    @Test
    fun testSimplerBox() = verifyDecoys(
        source = """
            @Composable
            fun SimpleBox(modifier: Modifier) {
               used(modifier)
            }
        """.trimIndent(),
        """
            @Decoy(targetName = "SimpleBox%composable", signature = "", "SimpleBox%composable", "6064675549046567690", "0")
            fun SimpleBox(modifier: Modifier) {
              return illegalDecoyCallException("SimpleBox")
            }
            @Composable
            @DecoyImplementation(name = "SimpleBox%composable", id = 9130371992496279898L)
            fun SimpleBox%composable(modifier: Modifier, %composer: Composer?, %changed: Int) {
              var %composer = %composer
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SimpleBox%composable):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                used(modifier)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                SimpleBox%composable(modifier, %composer, %changed or 0b0001)
              }
            }
        """.trimIndent()
    )

    @Test
    fun testCallingAWrapperComposableJS(): Unit = verifyDecoys(
        """
            @Composable
            fun Test() {
              W {
                A()
              }
            }
        """,
        """
            @Decoy(targetName = "Test%composable", signature = "", "Test%composable", "8019329703648006073", "0")
            fun Test() {
              return illegalDecoyCallException("Test")
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                var %composer = %composer
                sourceInformation(%composer, "C<A()>:Test.kt")
                if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                  A%composable(%composer, 0)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              ::invoke
            }
            @Composable
            @DecoyImplementation(name = "Test%composable", id = 117512589682537508L)
            fun Test%composable(%composer: Composer?, %changed: Int) {
              var %composer = %composer
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test%composable)<W>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                W%composable(ComposableSingletons%TestKt.lambda-1, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test%composable(%composer, %changed or 0b0001)
              }
            }
        """
    )

    override val decoysEnabled: Boolean get() = true

    private fun verifyDecoys(
        @Language("kotlin") source: String,
        expected: String,
        dumpTree: Boolean = false
    ) =
        verifyComposeIrTransform(
            """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.key

            $source
            """.trimIndent(),
            expected,
            """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.Stable

             @Stable
             interface Modifier {
                companion object : Modifier { }
             }
            
             @Composable fun A() {}
             fun used(x: Any?) {}
             @Composable fun W(content: @Composable () -> Unit) { content() }
            """.trimIndent(),
            dumpTree = dumpTree,
            compilation = JsCompilation(verifySignatures = true),
        )
}
