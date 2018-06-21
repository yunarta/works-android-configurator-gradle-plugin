package com.mobilesolutionworks.gradle.plugin.report.manager

import com.mobilesolutionworks.gradle.plugin.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.junit.Before

class PluginPmdTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("project")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def "test substitute by spec project"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        apply plugin: "works-report-manager"
        apply plugin: "jacoco"
        
        subprojects {
            apply plugin: "works-report-manager"
            apply plugin: "works-dependency-substitute"
            
            worksSubstitution {
                substitute spec("junit:junit") with version("4.12")
            }
            
            worksReportManager  {
                pmdTasks = ["pmdMain"]
            }            
        }
        """)

        execute("pmdMain", "test", "jacocoTestReport", "developerJacocoTestReport", "worksGatherReport")

        then:
        true
    }

    def "test substitute by spec project2"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        apply plugin: "works-report-manager"
        apply plugin: "jacoco"
        
        subprojects {
            apply plugin: "works-report-manager"
            apply plugin: "works-dependency-substitute"
            
            worksSubstitution {
                substitute spec("junit:junit") with version("4.12")
            }
            
            worksReportManager  {
                pmdFiles = files("build/reports/pmd")
            }            
        }
        """)

        execute("pmdMain", "test", "jacocoTestReport", "developerJacocoTestReport", "worksGatherReport")

        then:
        true
    }
}
