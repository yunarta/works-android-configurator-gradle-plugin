package com.mobilesolutionworks.android.gradle.configurator.substitution

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
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.50"
            }
        }
        
        """)

        execute(":app:worksPrintDependencies", "-PdepOutput=deps.txt")

        then:
        def lines = new File(testDir.root, "deps.txt").readLines()
        lines.contains("junit:junit:4.5")
        lines.contains("junit:junit-dep:4.5")
    }
}
