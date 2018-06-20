import com.mobilesolutionworks.gradle.publish.PublishedDoc
import com.mobilesolutionworks.gradle.publish.worksPublication
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.sure
import java.nio.file.*

plugins {
    `java-library`
    groovy
    jacoco
    pmd

    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC7-2"
    id("org.jetbrains.dokka") version "0.9.17"
}

group = "com.mobilesolutionworks.android"
version = "1.0.0"

apply {
    plugin("kotlin")
    plugin("works-publish")
}

worksPublication?.apply {
    javadoc = PublishedDoc.Kotlin
    module = file("module.yaml")
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

val jacocoTestPreparation = tasks.create("jacocoTestPreparation") {
    tasks.withType<Test> {
        shouldRunAfter(this@create)
        extensions.findByType(JacocoTaskExtension::class.java)?.apply {
            isEnabled = false
        }
    }

    tasks.withType<JacocoReportBase> {
        dependsOn("jacocoTestPreparation")
    }

    doFirst {
        tasks.withType<Test> {
            extensions.findByType(JacocoTaskExtension::class.java)?.apply {
                isEnabled = true
            }
        }
    }
}

tasks.create("createClasspathManifest") {
    group = "plugin development"
    description = "Create classpath manifest required to be used in GradleRunner"
    inputs.files(sourceSets["main"].runtimeClasspath)

    val outputDir = Paths.get(buildDir.toString(), "testKit", "classpath").toFile()
    outputs.dir(outputDir)

    tasks.withType<Test> {
        dependsOn(this@create.path)
    }

    doFirst {
        outputDir.mkdirs()
        File(outputDir, "plugin-classpath.txt").apply {
            writeText(sourceSets["main"].runtimeClasspath.joinToString(System.lineSeparator()))
        }
        File(outputDir, "plugin-under-test-metadata.properties").apply {
            writeText("implementation-classpath=" +
                    sourceSets["main"].runtimeClasspath.joinToString(":"))
        }
    }

    dependencies {
        testRuntime(outputs.files)
    }
}

tasks.withType<Test> {
    shouldRunAfter("testKitSetupAgent")
    dependsOn("testKitSetupAgent")
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

tasks.createLater("openJacocoReport", Exec::class.java) {
    group = "jacoco"

    tasks.withType<JacocoReport> {
        val index = reports.html.entryPoint.absolutePath
        setCommandLine("open", "$index")
    }
}

tasks.create("automationTest") {
    group = "automation"
    description = "Execute test with coverage"

    dependsOn("cleanTest", "test", "jacocoTestReport")
}

tasks.create("automationCheck") {
    group = "automation"
    description = "Execute check"

    dependsOn("detektCheck")
}

val extractAgent = tasks.create("testKitExtractAgent", Copy::class.java) {
    group = "jacoco"
    description = "Unzip jacocoagent to be used as javaagent in Gradle Runner"

    shouldRunAfter("jacocoTestPreparation")
    from(zipTree(configurations["jacocoRuntime"].asPath))
    into(Paths.get(buildDir.name, "testKit", "jacocoAgent").toFile())
}

tasks.create("testKitSetupAgent", WriteProperties::class.java) {
    group = "jacoco"
    description = "Write gradle.properties file to be used in Gradle Runner"
    enabled = false
    Paths.get(buildDir.name, "testKit", "jacocoEnv").toFile().also {
        outputFile = File(it, "gradle.properties")
        dependencies {
            testRuntime(files(it))
        }
    }

    jacocoTestPreparation.doFirst {
        this@create.enabled = true
    }

    dependsOn("testKitExtractAgent")
    shouldRunAfter("testKitExtractAgent")

    val jacocoPath = File(extractAgent.destinationDir, "jacocoagent.jar")
    val jacocoReportDir = File(buildDir, "jacoco")
    property("org.gradle.jvmargs", "-javaagent:${jacocoPath}=destfile=${jacocoReportDir}")
    doFirst {
        logger.quiet("""Gradle properties for Tests
                   |org.gradle.jvmargs=-javaagent:${jacocoPath}=destfile=${jacocoReportDir}
            """.trimMargin())
    }
}

tasks.withType<Test> {
    doFirst {
        logger.quiet("Test JVM Arguments")
        allJvmArgs.forEach {
            logger.quiet(" $it")
        }
    }
}

val ignoreFailures: String? by rootProject.extra
val shouldIgnoreFailures = ignoreFailures?.toBoolean() == true

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors() // .div(2)
    ignoreFailures = shouldIgnoreFailures

    doFirst {
        logger.quiet("Test with max $maxParallelForks parallel forks")
    }
}