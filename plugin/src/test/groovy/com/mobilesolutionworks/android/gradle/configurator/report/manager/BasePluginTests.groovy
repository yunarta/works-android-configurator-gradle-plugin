package com.mobilesolutionworks.android.gradle.configurator.report.manager

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
        apply plugin: "works-report-manager"
        apply plugin: "jacoco"
        
        subprojects {
            apply plugin: "works-report-manager"
            apply plugin: "works-dependency-substitute"
            
            worksSubstitution {
                substitute spec("junit:junit") with version("4.12")
            }
            
            worksReportManager  {
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
