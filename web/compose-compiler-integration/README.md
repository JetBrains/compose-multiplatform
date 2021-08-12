RUN from project root directory:
`./gradlew :compose-compiler-integration:checkComposeCases`


To use specific version (the default is 0.0.0-SNASPHOT):
`./gradlew :compose-compiler-integration:checkComposeCases -PCOMPOSE_CORE_VERSION=0.5.0-build243`

To fun only filtered cases (check for contained in file path):
`./gradlew :compose-compiler-integration:checkComposeCases -PFILTER_CASES=CaseName`
