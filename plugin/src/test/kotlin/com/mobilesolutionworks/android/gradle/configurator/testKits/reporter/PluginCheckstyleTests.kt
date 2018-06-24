package com.mobilesolutionworks.android.gradle.configurator.testKits.reporter

import com.mobilesolutionworks.android.gradle.configurator.testKits.TestKitTestCase
import com.mobilesolutionworks.android.gradle.configurator.util.withPaths
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

internal class PluginCheckstyleTests : TestKitTestCase("KotlinDSL") {

    @Test
    fun `test checkstyle gathering`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit:junit") with version("4.12")
                    }

                    worksReporting {
                        checkstyleTasks = ["detektCheck"]
                        checkstyleFiles = files("build/reports/detekt")
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments("detektGenerateConfig", "worksGatherReport")
                .build()
                .let {

                }
    }

    @Test
    fun `test using checkstyle task with output`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit:junit") with version("4.12")
                    }

                    worksReporting {
                        checkstyleTasks = ["checkAll"]
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments("detektGenerateConfig", "worksGatherReport")
                .build()
                .let {

                }
    }

    @Test
    fun `test using checkstyle task with non existing tasks`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit:junit") with version("4.12")
                    }

                    worksReporting {
                        checkstyleTasks = ["nonExisting"]
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments("worksGatherReport")
                .build()
                .let {

                }
    }
}
