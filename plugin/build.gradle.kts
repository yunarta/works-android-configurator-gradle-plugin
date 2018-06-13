import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.*

plugins {
    `java-library`
    groovy
    jacoco
}

group = "com.mobilesolutionworks.android"
version = "1.0-SNAPSHOT"

apply {
    plugin("kotlin")
}

repositories {
    mavenCentral()
}

val kotlinVersion: String by rootProject.extra
val sourceSets: SourceSetContainer = java.sourceSets

dependencies {
    testImplementation("junit:junit:4.12")

    testImplementation(gradleTestKit())
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(group = "org.codehaus.groovy")
    }

    api(gradleApi())
    implementation("org.codehaus.groovy:groovy-all:2.3.11")
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
}

tasks.create("createClasspathManifest") {
    group = "plugin development"
    description = "Create classpath manifest required to be used in GradleRunner"

    val outputDir = Paths.get(buildDir.absolutePath, "testKit", "classpath").toFile()
    doFirst {
        buildDir.mkdirs()
    }

    doLast {
        File(outputDir, "classpath.txt").apply {
            val classpath = sourceSets["main"].runtimeClasspath.joinToString("\n")
            logger.quiet("""TestKit classpath:
            |${classpath.prependIndent(" - ")}""".trimMargin())

            writeText(classpath)
        }
    }

    inputs.files(sourceSets.getAt("main").runtimeClasspath)
    outputs.dir(outputDir)
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}