package com.mobilesolutionworks.android.gradle.configurator.reporter

import com.mobilesolutionworks.android.gradle.configurator.base.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.junit.Before

class BasePluginTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("project")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def "test default task"() {
        when:
        buildGradle.append("""
        apply plugin: "works-ci-reporter"
        apply plugin: "jacoco"
        
        subprojects {
            apply plugin: "works-ci-reporter"
            apply plugin: "works-dependency-substitute"
            
            worksSubstitution {
                substitute spec("junit:junit") with version("4.12")
            }
            
            worksReporter  {
                defaultCoverage = "developerJacocoTestReport"
                defaultTest = "parallelTest"
            }            
        }
        """)

        execute("worksGatherReport")

        then:
        true
    }
}