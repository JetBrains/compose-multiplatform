import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure

fun Project.configureMavenPublication(
    groupId: String,
    artifactId: String,
    name: String
) {
    extensions.configure<PublishingExtension> {
        publications {
            all {
                this as MavenPublication

                this.groupId = groupId
                mppArtifactId = artifactId

                pom {
                    this.name.set(name)
                    url.set("https://github.com/JetBrains/compose-jb")
                    licenses {
                        license {
                            this.name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}