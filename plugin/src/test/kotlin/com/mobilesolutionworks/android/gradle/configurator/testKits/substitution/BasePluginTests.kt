package com.mobilesolutionworks.android.gradle.configurator.testKits.substitution

import com.mobilesolutionworks.android.gradle.configurator.testKits.TestKitTestCase
import com.mobilesolutionworks.android.gradle.configurator.util.withPaths
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

internal class BasePluginTests : TestKitTestCase("GroovyDSL") {

    @Test
    fun `test substitute by spec project`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit") with version("4.5")
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PdepOutput=deps.txt")
                .build()
                .let {
                    val lines = File(tempDir.root, "deps.txt").readLines()
                    assertTrue(lines.contains("junit:junit:4.5"))
                    assertTrue(lines.contains("junit:junit-dep:4.5"))
                }
    }

    @Test
    fun `test substitute by module project`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit:junit") with version("4.5")
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PdepOutput=deps.txt")
                .build()
                .let {
                    val lines = File(tempDir.root, "deps.txt").readLines()
                    assertTrue(lines.contains("junit:junit:4.5"))
                }
    }

    @Test
    fun `test integration build project`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit") with version("4.5")

                        when {
                            integration {
                                substitute spec("junit") with version("4.11")
                            }
                        }
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PbuildSubstitution=integration", "-PdepOutput=deps.txt")
                .build()
                .let {
                    val lines = File(tempDir.root, "deps.txt").readLines()
                    assertTrue(lines.contains("junit:junit:4.11"))
                    assertTrue(lines.contains("junit:junit-dep:4.11"))
                }
    }

    @Test
    fun `test integration + unstable build project`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit") with version("4.5")

                        when {
                            integration {
                                substitute spec("junit") with version("4.11")
                            }

                            unstable {
                                substitute spec("junit:junit") with version("4.12")
                            }
                        }
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PbuildSubstitution=integration,unstable", "-PdepOutput=deps.txt")
                .build()
                .let {
                    val lines = File(tempDir.root, "deps.txt").readLines()
                    assertTrue(lines.contains("junit:junit:4.12"))
                    assertTrue(lines.contains("junit:junit-dep:4.11"))
                }
    }

    @Test
    fun `test missing build type build project`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit") with version("4.5")

                        when {
                            unstable {
                                substitute spec("junit:junit") with version("4.12")
                            }
                        }
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PbuildSubstitution=integration,unstable,not-even-exists", "-PdepOutput=deps.txt")
                .build()
                .let {
                    val lines = File(tempDir.root, "deps.txt").readLines()
                    assertTrue(lines.contains("junit:junit:4.12"))
                }
    }

    @Test(expected = UnexpectedBuildFailure::class)
    fun `test wrong spec`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit:") with version("4.5")
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PdepOutput=deps.txt")
                .build()
    }

    @Test(expected = UnexpectedBuildFailure::class)
    fun `test incomplete substitution`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                       substitute spec("junit")
                    }
                }
            """.trimIndent())
        }

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempDir.root)
                .withArguments(":app:worksPrintDependencies", "-PdepOutput=deps.txt")
                .build()
    }

}
