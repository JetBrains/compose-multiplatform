## Run:

`./gradlew build -Pkotlin.version=2.2.10`
to build and run the tests,

or
`./gradlew allTests -Pkotlin.version=2.2.10`
to only run the tests.

It will build and run the tests for all available targets (depends on a host machine).

## Test cases

#### Directory: `./testcases`

Every test case contains a simple project with 2 or more modules with applied Compose plugin.
The goal of such a project is to check a specific case of Composable usage.    
A test case ensures that the code compiles and behaves as expected when running.   
A test case contains 2 or more modules to check crossmodule compilation (the test dependencies should be placed in a `...-lib` module).

See a test case [template](./testcases/template) for an example.
Splitting the test cases into separate small projects helps in distinguishing the potential compilation bugs.

The behaviour is checked by the tests in a `...-main` module of a test case.   
The tests create a simple text Composition and then compares the text dump with an expected value.       
Example:
```kotlin
@Test
fun testExample2() = runTest {
    val root = composeText {
        TextLeafNode("Leaf")
        TextContainerNode("node") {
            TextLeafNode("child1")
            TextLeafNode("child2")
            TextLeafNode("child3")
        }
    }

    assertEquals("root:{Leaf, node:{child1, child2, child3}}", root.dump())
}
```
The API for the text Composition is implemented in the [common](./common) module. 
More test examples (including recomposition cases) can be found [here](./common/src/commonTest).  

### To add a new test case:
- Use `/testcases/template` as a starting point (copy it and rename). Place a new testcase in `/testcases` folder.
- Add new modules in `settings.gradle.kts`
- Please follow the naming convention for modules. Their names should end with `-lib` or `-main`.   
  We need 2 modules (`...-lib` and `...-main`) to test crossmodule compilation.    
  Place the test dependencies in a `...-lib` module.

