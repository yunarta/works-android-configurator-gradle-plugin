package com.mobilesolutionworks.android.gradle.configurator.reporter

import com.mobilesolutionworks.android.gradle.configurator.base.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.junit.Before

class GradleKtsPluginTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("gradle-kts")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def "test substitute by spec project"() {
        when:
        buildGradle.append("""
        buildscript {
            dependencies {
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.41"
            }
        }
        
        apply plugin: "works-ci-reporter"
        
        subprojects {
            apply plugin: "works-dependency-substitute"
            
            worksSubstitution {
                substitute spec("junit:junit") with version("4.12")
            }
            
            worksReporter  { 
                checkstyleTasks = ["detektCheck"]
                checkstyleFiles = files("build/reports/detekt")
            }            
        }
        """)

        execute("detektGenerateConfig", "detektCheck", "worksGatherCheckstyle", "test", "worksGatherReport")

        then:
        true
    }
}
