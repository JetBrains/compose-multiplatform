RUN from project root directory:
`./gradlew :compose-compiler-integration:checkComposeCases`

To use specific version:
`./gradlew :compose-compiler-integration:checkComposeCases -Pcompose.version=1.2.0-beta03

To fun only filtered cases (check for contained in file path):
`./gradlew :compose-compiler-integration:checkComposeCases -PFILTER_CASES=CaseName`
