RUN from project root directory:
`./gradlew :compose-compiler-integration:checkComposeCases`

To use specific version:
`./gradlew :compose-compiler-integration:checkComposeCases -PCOMPOSE_CORE_VERSION=1.0.0 -PCOMPOSE_WEB_VERSION=1.0.0

To fun only filtered cases (check for contained in file path):
`./gradlew :compose-compiler-integration:checkComposeCases -PFILTER_CASES=CaseName`
