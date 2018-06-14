import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.sure
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

val jacocoRuntime by configurations.creating

dependencies {
    jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.1")

    testImplementation("junit:junit:4.12")
    testImplementation(gradleTestKit())
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

    val outputDir = file("$buildDir/jacocoAgent")

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

    val outputDir = file("$buildDir/jacocoAgent")
    doFirst {
        outputDir.mkdirs()
        file("$outputDir/gradle.properties").writeText("")
    }

    doLast {
        val jacocoPath = File(outputDir, "jacocoagent.jar").absolutePath

        val gradleProperties = file("$outputDir/gradle.properties")
        if (gradle.taskGraph.hasTask(":${project.name}:createJacocoTestReport")) {
            val jacocoOutputDir = File(buildDir, "jacoco").absolutePath
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

tasks.create("createJacocoTestReport", JacocoReport::class.java) {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for Debug build"

    dependsOn("setupJacocoAgent", "test")
    inputs.file(fileTree(mapOf("dir" to project.rootDir.absolutePath, "include" to "**/build/jacoco/*.exec")))
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    // what to exclude from coverage report
    // UI, "noise", generated classes, platform classes, etc.
    val excludes = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/*\$ViewInjector*.*",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/*Fragment.*",
            "**/*Activity.*"
    )
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

tasks.create("testWithCoverage") {
    group = "automation"
    description = "Execute test with coverage"

    dependsOn("createJacocoTestReport")
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

    ignoreFailures = shouldIgnoreFailures
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}