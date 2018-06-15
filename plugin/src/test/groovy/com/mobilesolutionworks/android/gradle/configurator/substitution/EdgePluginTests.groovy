package com.mobilesolutionworks.android.gradle.configurator.substitution

import com.mobilesolutionworks.android.gradle.configurator.base.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.junit.Before

class EdgePluginTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("project")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def "test print dependencies"() {
        when:
        buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit") with version("4.5")
            }
        }
        """)

        BuildResult result = execute(":app:worksPrintDependencies")

        then:
        def lines = result.output.readLines()
        lines.contains("junit:junit:4.5")
        lines.contains("junit:junit-dep:4.5")
    }
}
