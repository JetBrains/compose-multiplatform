RUN from project root directory:
`./gradlew :compose-compiler-integration:checkComposeCases`


To use specific version (the default is 0.0.0-SNASPHOT):
`COMPOSE_INTEGRATION_VERSION=0.5.0-build235 ./gradlew :compose-compiler-integration:checkComposeCases`

To fun only filtered cases (check for contained in file path):
`./gradlew :compose-compiler-integration:checkComposeCases -PFILTER_CASES=CaseName`
