package com.mobilesolutionworks.android.gradle.configurator.testKits.reporter

import com.mobilesolutionworks.android.gradle.configurator.testKits.TestKitTestCase
import com.mobilesolutionworks.android.gradle.configurator.util.withPaths
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

internal class BasePluginTests : TestKitTestCase("GroovyDSL") {

    @Test
    fun `test default task`() {
        tempDir.root.withPaths("build.gradle").apply {
            appendText("\n\n")
            appendText("""
                subprojects {
                    worksSubstitution {
                        substitute spec("junit:junit") with version("4.12")
                    }

                    worksReporting  {
                        defaultCoverage = "developerJacocoTestReport"
                        defaultTest = "parallelTest"
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
