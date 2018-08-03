import com.mobilesolutionworks.gradle.publish.PublishedDoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.sure
import java.nio.file.*

plugins {
    kotlin("jvm") version "1.2.50"
    `java-gradle-plugin`
    jacoco
    pmd

    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC7-2"
    id("org.jetbrains.dokka") version "0.9.17"
    id("com.mobilesolutionworks.gradle.jacoco") version "1.1.3"
    id("com.mobilesolutionworks.gradle.publish") version "1.5.3"
    id("com.gradle.plugin-publish") version "0.9.10"
}

group = "com.mobilesolutionworks.gradle"
version = "1.0.9"

worksJacoco {
    hasTestKit = true
}

repositories {
    mavenCentral()
}

val kotlinVersion: String by rootProject.extra
val sourceSets: SourceSetContainer = java.sourceSets

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

jacoco {
    toolVersion = "0.8.1"
    reportsDir = file("$buildDir/reports")
}

detekt {
    version = "1.0.0.RC7-2"

    profile("main", Action {
        input = "src/main/kotlin"
        filters = ".*/resources/.*,.*/build/.*"
        config = file("default-detekt-config.yml")
        output = "$buildDir/reports/detekt"
        outputName = "detekt-report"
        baseline = "reports/baseline.xml"
    })
}

val jacocoRuntime by configurations.creating
val pluginRuntime by configurations.creating

dependencies {
    jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.1")

    testImplementation("junit:junit:4.12")

    testImplementation(gradleTestKit())
    testImplementation("org.mockito:mockito-core:2.19.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(group = "org.codehaus.groovy")
    }

    compileOnly(gradleApi())

    implementation("commons-io:commons-io:2.6")
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))

    pluginRuntime("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.50")
}

gradlePlugin {
    (plugins) {
//        "works-substitute" {
//            id = "com.mobilesolutionworks.gradle.substitute"
//            implementationClass = "com.mobilesolutionworks.android.gradle.configurator.substitution.DependencySubstitutionPlugin"
//        }
        "works-reporting" {
            id = "com.mobilesolutionworks.gradle.reporting"
            implementationClass = "com.mobilesolutionworks.android.gradle.configurator.reporter.ReporterPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/yunarta/https://github.com/yunarta/works-ci-publish-gradle-plugin"
    vcsUrl = "https://github.com/yunarta/https://github.com/yunarta/works-ci-publish-gradle-plugin"
    description = "Plugin for automatically gather project report into root folder for easier continuous integration"
    tags = listOf("jacoco", "works")

    (plugins) {
//        "works-substitute" {
//            id = "com.mobilesolutionworks.gradle.substitute"
//            displayName = "Reusable GroovyDSL publishing to be used in along Jenkins pipeline."
//        }

        "works-reporting" {
            id = "com.mobilesolutionworks.gradle.reporting"
            displayName = "Plugin for automatically gather project report into root folder for easier continuous integration"
        }
    }
}

tasks.withType<PluginUnderTestMetadata> {
    pluginClasspath += configurations["pluginRuntime"].asFileTree
}

task("cleanTest", Delete::class) {
    delete(
            tasks.getByName("test").outputs.files,
            Paths.get("build", "testKit", "jacocoEnv"),
            Paths.get("build", "tmp", "runTest").toFile()
    )
}

tasks.withType<KotlinCompile> {
    group = "compilation"
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JacocoReport> {

    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    // generated classes
    classDirectories = fileTree(mapOf(
            "dir" to "$buildDir/classes/java/main")
    ) + fileTree(mapOf(
            "dir" to "$buildDir/classes/kotlin/main")
    )

    // sources
    sourceDirectories = files(listOf("src/main/kotlin", "src/main/java", "/src/test/groovy"))
    executionData = fileTree(mapOf("dir" to project.rootDir.absolutePath, "include" to "**/build/jacoco/*.exec"))
}

val ignoreFailures: String? by rootProject.extra
val shouldIgnoreFailures = ignoreFailures?.toBoolean() == true

tasks.withType<Test> {
    maxParallelForks = Math.max(1, Runtime.getRuntime().availableProcessors().div(2))
    ignoreFailures = shouldIgnoreFailures

    doFirst {
        logger.quiet("Test with max $maxParallelForks parallel forks")
    }
}