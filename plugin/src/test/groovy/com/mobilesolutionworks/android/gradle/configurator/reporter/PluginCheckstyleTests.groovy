package com.mobilesolutionworks.android.gradle.configurator.reporter

import com.mobilesolutionworks.android.gradle.configurator.base.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.junit.Before

class PluginCheckstyleTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("gradle-kts")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def "test checkstyle gathering"() {
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

        execute("detektGenerateConfig", "detektCheck", "worksGatherReport")

        then:
        true
    }

    def "test using checkstyle task with output"() {
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
                checkstyleTasks = ["checkAll"]
            }    
        }
        """)

        execute("detektGenerateConfig", "detektCheck", "worksGatherCheckstyle", "test", "worksGatherReport")

        then:
        true
    }

    def "test using checkstyle task with non existing tasks"() {
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
                checkstyleTasks = ["nonExisting"]
            }    
        }
        """)

        execute("worksGatherReport")

        then:
        true
    }
}
