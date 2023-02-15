## Run:

`./gradlew build -Pcompose.kotlinCompilerPluginVersion=1.4.2-rc01 -Pkotlin.version=1.8.10`

It will build and run the tests for all available targets (depends on a host machine).
See [TargetsConfiguration](./buildSrc/src/main/kotlin/TargetsConfiguration.kt) to update the targets.


## Test cases

#### Directory: `./testcases`

Every test case contains a simple project with 2 or more modules with applied Compose plugin.
The goal of such a project is to check a specific case of Composable usage.    
A test case ensures that the code compiles and behaves as expected when running.   
A test case contains 2 or more modules to check crossmodule compilation (the test dependencies should be placed in a `...-lib` module).

See a test case [template](./testcases/template) for an example.
Splitting the test cases into separate small projects helps in distinguishing the potential compilation bugs.


### To add a new test case:
- Use `/testcases/template` as a starting point (copy it and rename). Place a new testcase in `/testcases` folder.
- Add new modules in `settings.gradle.kts`
- Please follow the naming convention for modules. Their names should end with `-lib` or `-main`.   
  We need 2 modules (`...-lib` and `...-main`) to test crossmodule compilation.    
  Place the test dependencies in a `...-lib` module.

