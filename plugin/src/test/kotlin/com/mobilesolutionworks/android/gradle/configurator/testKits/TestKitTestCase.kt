package com.mobilesolutionworks.android.gradle.configurator.testKits

import com.mobilesolutionworks.android.gradle.configurator.testUtils.CopyResourceFolder
import com.mobilesolutionworks.android.gradle.configurator.util.withPaths
import org.junit.After
import org.junit.Before
import java.io.File
import java.nio.file.Paths
import java.util.*

open class TestKitTestCase(folder: String) {

    val tempDir = CopyResourceFolder(
            Paths.get("build", "tmp", "testKit", javaClass.simpleName).toFile(),
            folder
    )

    @Before
    fun setup() {
        tempDir.create()
        javaClass.classLoader.getResourceAsStream("javaagent-for-testkit.properties")?.let {
            Properties().apply {
                load(it)
            }.let {
                val agentPath = it.getProperty("agentPath")
                val outputDir = it.getProperty("outputDir")

                val execFile = File(outputDir, "${javaClass.name}.exec")
                val agentString = "org.gradle.jvmargs=-javaagent\\:${agentPath}\\=destfile\\=${execFile.absolutePath}"

                File(tempDir.root, "gradle.properties").writeText(agentString)
            }
        }

        tempDir.root.withPaths("build.gradle").apply {
            writeText("""
                plugins {
                    id "com.mobilesolutionworks.gradle.reporting" apply false
                    id "com.mobilesolutionworks.gradle.substitute" apply false
                    id "jacoco"
                }

                repositories {
                    mavenCentral()
                }

                apply plugin: "com.mobilesolutionworks.gradle.reporting"
                subprojects {
                    apply plugin: "jacoco"
                    apply plugin: "com.mobilesolutionworks.gradle.substitute"
                }
            """.trimIndent())
        }

    }

    @After
    fun tearDown() {
        // tempDir.delete()
    }
}
