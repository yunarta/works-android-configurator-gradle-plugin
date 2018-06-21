package com.mobilesolutionworks.android.gradle.configurator.base

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.BeforeClass
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths

abstract class PluginTestSpecification extends Specification {

    private static tempDir = Paths.get("build", "tmp", "runTest").toFile()

    public static TemporaryFolder testDir = new TemporaryFolder(tempDir)

    private static File gradleProperties

    static File buildGradle
    static File settingsGradle

    @BeforeClass
    static def setupFolder() {
        tempDir.mkdirs()
        testDir.create()

        gradleProperties = testDir.newFile("gradle.properties")
        buildGradle = testDir.newFile("build.gradle")
        settingsGradle = testDir.newFile("settings.gradle")
    }

    GradleRunner gradleRunner

    @Before
    def configureRootGradle() {
        def loader = PluginTestSpecification.class.classLoader
        def pluginClasspathResource = loader.getResource "plugin-classpath.txt"
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
        def classpathString = pluginClasspath
                .collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")

        def resource = loader.getResource("gradle.properties")
        if (resource != null) {
            def agent = resource.text.trim() + File.separatorChar + "${getClass().name}.exec"

            gradleProperties.write("""${agent}
            |org.gradle.daemon=true""".stripMargin("|"))
        }

        buildGradle.write("""
        |buildscript {
        |    repositories {
        |        google()
        |        jcenter()
        |        mavenCentral()
        |        maven {
        |            url 'https://dl.bintray.com/mobilesolutionworks/release'
        |        }
        |    }
        |
        |    dependencies {
        |        classpath files($classpathString)
        |        classpath 'com.mobilesolutionworks:works-publish:1.5.1'
        |    }
        |}
        |
        |allprojects {
        |   repositories {
        |       google()
        |       jcenter()
        |       mavenCentral()
        |   }
        |}
        """.stripMargin())

        settingsGradle.write("""
        |buildscript {
        |    dependencies {
        |        classpath files($classpathString)
        |    }
        |}
        |
        |include ':app'
        """.stripMargin())

        gradleRunner = GradleRunner.create()
                .withProjectDir(testDir.root)
                .withPluginClasspath()
                .withGradleVersion("4.8")
                .withPluginClasspath(pluginClasspath)
                .forwardOutput()
    }

    def execute(String... arguments) {
        execute(arguments.toList())
    }

    def execute(List<String> arguments) {
        def execute = new ArrayList(arguments) + "worksPrintDependencies" + "--stacktrace"
        gradleRunner.withArguments(execute).build()
    }

    def verifySuccess(BuildResult result, List<String> arguments) {
        return arguments.collect {
            result.task(it).outcome == TaskOutcome.SUCCESS
        }.inject(true) {
            initial, next -> initial && next
        }
    }
}
