import com.mobilesolutionworks.gradle.publish.worksPublication
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.sure
import java.nio.file.*

plugins {
    `java-library`
    groovy
    jacoco

    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC6-4"
    id("org.jetbrains.dokka") version "0.9.17"
}

group = "com.mobilesolutionworks.android"
version = "1.0.0"

apply {
    plugin("kotlin")
    plugin("works-publish")
}

worksPublication?.apply {
    module = file("module.properties")
}

repositories {
    mavenCentral()
}

val kotlinVersion: String by rootProject.extra
val sourceSets: SourceSetContainer = java.sourceSets

jacoco {
    toolVersion = "0.8.1"
    reportsDir = file("$buildDir/reports")
}

detekt {
    version = "1.0.0.RC6-4"

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

dependencies {
    jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.1")

    testImplementation("junit:junit:4.12") {
        this.reason
    }

    testImplementation(gradleTestKit())
    testImplementation("org.mockito:mockito-core:2.8.9")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(group = "org.codehaus.groovy")
    }

    compileOnly(gradleApi())

    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
}

val createClasspathManifest = task("createClasspathManifest") {
    group = "plugin development"
    description = "Create classpath manifest required to be used in GradleRunner"

    val outputDir = Paths.get(buildDir.toString(), "testKit", "classpath").toFile()
    doFirst {
        buildDir.mkdirs()
    }

    doLast {
        File(outputDir, "plugin-classpath.txt").apply {
            writeText(sourceSets["main"].runtimeClasspath.joinToString(System.lineSeparator()))
        }
        File(outputDir, "plugin-under-test-metadata.properties").apply {
            writeText("implementation-classpath=" +
                    sourceSets["main"].runtimeClasspath.joinToString(":"))
        }
    }

    inputs.files(sourceSets["main"].runtimeClasspath)
    outputs.dir(outputDir)
    dependencies {
        testRuntime(outputs.files)
    }
}

tasks.create("unzipJacoco", Copy::class.java) {
    group = "jacoco"
    description = "Unzip jacocoagent to be used as javaagent in Gradle Runner"

    val outputDir = File(buildDir, "jacocoAgent")

    doFirst {
        outputDir.mkdirs()
    }

    from(zipTree(configurations["jacocoRuntime"].asPath))
    into(outputDir)
}

tasks.create("setupJacocoAgent") {
    group = "jacoco"
    description = "Write gradle.properties file to be used in Gradle Runner"

    dependsOn("unzipJacoco")

    val outputDir = File(buildDir, "jacocoAgent")
    doFirst {
        outputDir.mkdirs()
        File(outputDir, "gradle.properties").writeText("")
    }

    doLast {
        val jacocoPath = File(outputDir, "jacocoagent.jar")

        val gradleProperties = File(outputDir, "gradle.properties")
        if (gradle.taskGraph.hasTask(":${project.name}:jacocoCoverageTest")) {
            val jacocoOutputDir = File(buildDir, "jacoco")
            gradleProperties.writeText("""org.gradle.jvmargs=-javaagent:${jacocoPath}=destfile=$jacocoOutputDir""".trimMargin())

            logger.quiet("""Gradle properties for Tests
                   |${gradleProperties.readText()}
            """.trimMargin())
        }
    }

    outputs.dir(outputDir)
    dependencies {
        testRuntime(outputs.files)
    }
}

tasks.create("jacocoCoverageTest", JacocoReport::class.java) {
    group = "jacoco"
    description = "Generate Jacoco coverage reports for Debug build"

    dependsOn("setupJacocoAgent", "test")
    inputs.file(fileTree(mapOf("dir" to project.rootDir.absolutePath, "include" to "**/build/jacoco/*.exec")))
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

tasks.create("automationTest") {
    group = "automation"
    description = "Execute test with coverage"

    dependsOn("cleanTest", "jacocoCoverageTest")
}

tasks.create("automationCheck") {
    group = "automation"
    description = "Execute check"

    dependsOn("detektCheck")
}

task("cleanTest", Delete::class) {
    delete(
            tasks.getByName("test").outputs.files,
            tasks.getByName("setupJacocoAgent").outputs.files,
            Paths.get("build", "tmp", "runTest").toFile()
    )
}

val ignoreFailures: String? by rootProject.extra
val shouldIgnoreFailures = ignoreFailures?.toBoolean() ?: false

tasks.withType<Test> {
    dependsOn(createClasspathManifest.path)

    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)
    ignoreFailures = shouldIgnoreFailures
    doFirst {
        logger.quiet("Test with max $maxParallelForks parallel forks")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}