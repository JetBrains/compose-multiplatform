import TaskUtils.useAndroidJar
import TaskUtils.useAndroidSdk
import javax.xml.ws.Endpoint.publish

description = "R4A Runtime"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(commonDep("com.google.android", "android"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "r4a-runtime.jar")

publish()
