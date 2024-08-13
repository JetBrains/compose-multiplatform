This simple project does 2 things (for jvm, k/js, k/native):

- ensure that Compose + kotlinx.serialization compilation works
- ensure the Composable function taking a parameter of Serializable type runs


To run:

`./gradlew build -Pkotlinx.serializationCore=1.4.1 -Pkotlin.version=1.8.0 -Pcompose.version=1.3.0-rc02 -Pcompose.kotlinCompilerPluginVersion=1.4.0-alpha04`