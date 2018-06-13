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

dependencies {
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


task("cleanTest", Delete::class) {
    delete(
            tasks.getByName("test").outputs.files,
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