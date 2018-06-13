package com.mobilesolutionworks.android.gradle.configurator

import com.mobilesolutionworks.android.gradle.configurator.base.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.junit.Before

class BasePluginTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("android")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)

        settingsGradle.append("""
        |include ':android-lib'
        """.stripMargin())
    }

    def "test java library project"() {
        when:

        BuildResult result = execute("properties")

        then:
        true
//        verifySuccess(result, performTasks)
//        verifyStructure(prefix)
    }

}
