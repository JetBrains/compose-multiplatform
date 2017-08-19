
description = "R4A IDEA Plugin"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":r4a-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep()) { includeJars("openapi", "idea", "util", "extensions") }
    //compile(ideaPluginDeps("maven", plugin = "maven"))
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-api", "gradle", rootProject = rootProject) }
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

