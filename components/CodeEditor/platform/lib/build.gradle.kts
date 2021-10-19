import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

rootProject.apply {
    from(rootProject.file("gradle/projectProperties.gradle.kts"))
}

repositories {
    buildLocalRepo(project)
}

dependencies {
    implementation(project(":api"))

    implementation(intellijDep(), {
        isTransitive = false
        includeJars(
            "platform-api",
            "platform-impl",
            "annotations",
            "bootstrap",
            "util",
            "kotlin-stdlib-jdk8",
            "external-system-rt"
        )
    })

    implementation(intellijPluginDep("java")) {
        isTransitive = false
        includeJars(
            "java-api",
            "java-impl"
        )
    }

    implementation(intellijPluginDep("Kotlin")) {
        isTransitive = false
        includeJars(
            "kotlin-core",
            "kotlin-idea",
            "kotlin-formatter"
        )
    }

    runtimeOnly(intellijDep()) {
        isTransitive = false
        includeJars(
            "intellij-deps-fastutil-8.5.2-6",
            "intellij-xml",
            "intellij-dvcs",
            "resources",
            "resources_en",
            "log4j",
            "trove4j",
            "jdom",
            "jps-model",
            "proxy-vole",
            "caffeine-2.8.8",
            "jna",
            "jna-platform",
            "nanoxml-2.2.3",
            "json",
            "stats",
            "guava",
            "ap-validation-0.0.5",
            "streamex-0.7.3",
            "lz4-java-1.7.1",
            "forms_rt",
            "dom-openapi",
            "dom-impl",
            "asm-7.1",
            "asm-all-9.1",
            "oro-2.0.8",
            "cglib-nodep-3.2.4",
            "netty-codec-http",
            "netty-buffer",
            "protobuf-java-3.13.0",
            "automaton-1.12-1",
            "java-compatibility-1.0.1",
            "jackson-core-2.12.0",
            "jackson-databind",
            "gson-2.8.6",
            "jsp-base-openapi",
            "testFramework"
        )
    }

    runtimeOnly(intellijPluginDep("java")) {
        isTransitive = false
    }
    runtimeOnly(intellijPluginDep("Kotlin")) {
        isTransitive = false
    }
    runtimeOnly(intellijPluginDep("properties")) {
        isTransitive = false
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.compileJava {
    options.compilerArgs.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
}

tasks.processResources {
    from(intellijPlatformDir()) {
        include("build.txt")
        into("home")
    }
}

tasks.javadoc {
    exclude("com/intellij/**")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("ipw.debug", "true")
    jvmArgs(
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.font=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.swing=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED"
    )
}

tasks.register<ShadowJar>("ideaJar") {
    archiveClassifier.set("idea")
    dependencies {
        include {
            it.moduleGroup == "codeeditor.platform.build" && it.moduleName == "ideaIC"
        }
    }
}

tasks.register<ShadowJar>("javaJar") {
    archiveClassifier.set("java")
    dependencies {
        include {
            it.moduleGroup == "codeeditor.platform.build" && it.moduleName == "java"
        }
    }
}

tasks.register<ShadowJar>("propertiesJar") {
    archiveClassifier.set("properties")
    dependencies {
        include {
            it.moduleGroup == "codeeditor.platform.build" && it.moduleName == "properties"
        }
    }
}

tasks.register<ShadowJar>("kotlinJar") {
    archiveClassifier.set("kotlin")
    dependencies {
        include {
            it.moduleGroup == "codeeditor.platform.build" && it.moduleName == "Kotlin"
        }
    }
}

tasks.withType<ShadowJar> {
    manifest {
        from(tasks.jar.get().manifest)
    }

    isZip64 = true
    mergeServiceFiles()
    append("META-INF/io.netty.versions.properties")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("META-INF/jb/$\$size$$")
    exclude("__packageIndex__")
    exclude(".hash")
    exclude("**/package-info.class")
    exclude("**/module-info.class")

    exclude("META-INF/DEPENDENCIES")
    exclude("META-INF/AL2.0")
    exclude("META-INF/LGPL2.1")
    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/NOTICE.txt")

    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-codeeditor-platform-lib",
    name = "CodeEditor Platform library for Compose JB"
)


publishing {
    publications {
        create<MavenPublication>("fatJars") {
            artifactId = "components-codeeditor-platform-lib"
            artifact(tasks["ideaJar"])
            artifact(tasks["javaJar"])
            artifact(tasks["kotlinJar"])
            artifact(tasks["propertiesJar"])
        }
    }
}
