import org.jetbrains.kotlin.psi.KtFile

class FrameTransformExtensionTests : AbstractCodeGenTest() {

    fun testTestUtilities() = testFile("""
        class Foo {
          val s = "This is a test"
        }

        class Test {
          fun test() {
            Foo().s.expectEqual("This is a test")
          }
        }
    """)


    fun testSimpleComponent() = testFile("""
       import com.google.r4a.Component

       class MyComponent: Component() {
         override fun compose() {}
       }

       class Test {
         fun test() {
           frame { MyComponent() }
         }
       }
    """)


    fun testOneField() = testFile("""
        import com.google.r4a.Component

        class MyComponent: Component() {
          var value: String = "default"

          override fun compose() {}
        }

        class Test {
          fun test() {
            val instance = frame { MyComponent() }
            frame {
              instance.value.expectEqual("default")
              instance.value = "new value"
              instance.value.expectEqual("new value")
            }
            frame {
              instance.value.expectEqual("new value")
            }
          }
        }
    """)

    fun testIsolation() = testFile("""
        import com.google.r4a.Component

        class MyComponent: Component() {
          var value: String = "default"

          override fun compose() {}
        }

        class Test {
          fun test() {
            val instance = frame { MyComponent() }
            val frame1 = suspended {
              instance.value = "new value"
            }
            frame {
              instance.value.expectEqual("default")
            }
            restored(frame1) {
              instance.value.expectEqual("new value")
            }
            frame {
              instance.value.expectEqual("new value")
            }
          }
        }

    """)

    fun testThreeFields() = testFile("""
        import com.google.r4a.Component

        class MyComponent: Component() {
          var strVal = "default"
          var intVal = 1
          var doubleVal = 27.2

          override fun compose() {}
        }

        class Test {
          fun test() {
            val instance = frame { MyComponent() }
            frame {
              instance.strVal.expectEqual("default")
              instance.intVal.expectEqual(1)
              instance.doubleVal.expectEqual(27.2)
            }
            frame {
              instance.strVal = "new value"
            }
            frame {
              instance.strVal.expectEqual("new value")
              instance.intVal.expectEqual(1)
              instance.doubleVal.expectEqual(27.2)
            }
            frame {
              instance.intVal = 2
            }
            frame {
              instance.strVal.expectEqual("new value")
              instance.intVal.expectEqual(2)
              instance.doubleVal.expectEqual(27.2)
            }
          }
        }
    """)

    override fun helperFiles(): List<KtFile> = listOf(sourceFile("Helpers.kt", HELPERS))
}

const val HELPERS = """
    import com.google.r4a.frames.open
    import com.google.r4a.frames.commit
    import com.google.r4a.frames.suspend
    import com.google.r4a.frames.restore
    import com.google.r4a.frames.FrameData

    inline fun <T> frame(crossinline block: ()->T): T {
        open(false, false)
        try {
          return block()
        } finally {
          commit()
        }
    }

    inline fun suspended(crossinline block: ()->Unit): FrameData {
      open(false, false)
      block()
      return suspend()
    }

    inline fun restored(frame: FrameData, crossinline block: ()->Unit) {
      restore(frame)
      block()
      commit()
    }

    inline fun continued(frame: FrameData, crossinline block: ()->Unit): FrameData {
      restore(frame)
      block()
      return suspend()
    }

    fun Any.expectEqual(expected: Any) {
      expect(this, expected)
    }

    fun expect(expected: Any, received: Any) {
      if (expected != received) {
        throw Exception("Expected ${'$'}expected but received ${'$'}received")
      }
    }"""

