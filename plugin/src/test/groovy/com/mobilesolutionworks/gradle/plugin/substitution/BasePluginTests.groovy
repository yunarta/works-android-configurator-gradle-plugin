package com.mobilesolutionworks.gradle.plugin.substitution

import com.mobilesolutionworks.gradle.plugin.PluginTestSpecification
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.junit.Before

class BasePluginTests extends PluginTestSpecification {

    @Before
    static def configure() {
        def loader = PluginTestSpecification.class.classLoader

        def resource = loader.getResource("project")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def "test substitute by spec project"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit") with version("4.5")
            }
        }
        """)

        execute(":app:worksPrintDependencies", "-PdepOutput=deps.txt")

        then:
        def lines = new File(testDir.root, "deps.txt").readLines()
        lines.contains("junit:junit:4.5")
        lines.contains("junit:junit-dep:4.5")
    }

    def "test substitute by module project"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit:junit") with version("4.5")
            }
        }
        """)

        execute(":app:worksPrintDependencies", "-PdepOutput=deps.txt")

        then:
        def lines = new File(testDir.root, "deps.txt").readLines()
        lines.contains("junit:junit:4.5")
    }

    def "test integration build project"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit") with version("4.5")
                
                when {
                    integration {
                        substitute spec("junit") with version("4.11")
                    }
                }
            }
        }
        """)

        execute(":app:worksPrintDependencies",
                "-PbuildSubstitution=integration", "-PdepOutput=deps.txt")

        then:
        def lines = new File(testDir.root, "deps.txt").readLines()
        lines.contains("junit:junit:4.11")
        lines.contains("junit:junit-dep:4.11")
    }

    def "test integration + unstable build project"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
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
        """)

        execute(":app:worksPrintDependencies",
                "-PbuildSubstitution=integration,unstable", "-PdepOutput=deps.txt")

        then:
        def lines = new File(testDir.root, "deps.txt").readLines()
        lines.contains("junit:junit:4.12")
        lines.contains("junit:junit-dep:4.11")
    }

    def "test missing build type build project"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit") with version("4.5")
                
                when {
                    unstable {
                        substitute spec("junit:junit") with version("4.12")
                    }
                }
            }
        }
        """)

        BuildResult result = execute(":app:worksPrintDependencies",
                "-PbuildSubstitution=integration,unstable,not-even-exists", "-PdepOutput=deps.txt")

        then:
        def lines = new File(testDir.root, "deps.txt").readLines()
        lines.contains("junit:junit:4.12")
    }

    def "trim wrong spec"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit:") with version("4.5")
            }
        }
        """)

        Exception exception = null
        try {
            execute(":app:worksPrintDependencies", "-PdepOutput=deps.txt")
        } catch (e) {
            exception = e
        }

        then:
        exception != null
    }

    def "trim incomplete substitution"() {
        when:
        PluginTestSpecification.buildGradle.append("""
        subprojects {
            apply plugin: "works-dependency-substitute"
            worksSubstitution {
                substitute spec("junit")
            }
        }
        """)

        Exception exception = null
        try {
            execute(":app:worksPrintDependencies", "-PdepOutput=deps.txt")
        } catch (e) {
            exception = e
        }

        then:
        exception != null
    }
}
