package com.mobilesolutionworks.android.gradle.configurator.reporter

import com.mobilesolutionworks.android.gradle.configurator.util.CollectorLatch
import com.mobilesolutionworks.android.gradle.configurator.util.paths
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.nio.file.Paths


class ReporterPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        if (target.rootProject == target) {
            applyToRootProject(target)
        }
    }

    private fun applyToRootProject(project: Project) {
        with(project) {
            val latch = CollectorLatch(subprojects.size + 1, this) {
                startConfiguration(it)
            }
            afterEvaluate {
                latch.increment()
            }
            subprojects.forEach {
                applySubProject(it)
                it.afterEvaluate {
                    latch.increment()
                }
            }
        }
    }

    private fun startConfiguration(project: Project) {
        with(project) {
            createGatherTestTasks(project)
            createGatherJacocoTasks(project)
            createGatherStaticAnalysisTasks(project)

            tasks.create("worksGatherReport") {
                it.group = "works reporter"

                it.shouldRunAfter("worksRootJacocoReport")
                it.dependsOn("worksGatherCheckstyle", "worksGatherPMD", "worksRootJacocoReport",
                        "worksGatherTestReport", "worksGatherCoverageReport")
            }
        }
    }

    private fun Project.createGatherTestTasks(subProject: Project) {
        tasks.create("worksGatherTestReport", Copy::class.java) { copy: Copy ->
            copy.group = "works reporter"
            copy.into(subProject.buildDir.paths("reports", "junit"))

            subprojects { subProject ->
                subProject.tasks.withType(Test::class.java).map {
                    val name = when (it.name) {
                        subProject.reporter.defaultTest -> "default"
                        else -> it.name
                    }

                    copy.from(it) { spec ->
                        spec.include("*.xml")
                        spec.into(Paths.get("xml", it.project.name, name).toFile())
                    }

                    copy.from(it) { spec ->
                        spec.exclude("*.xml")
                        spec.into(Paths.get("html", it.project.name, name).toFile())
                    }
                }
            }
        }
    }

    private fun Project.createGatherJacocoTasks(subProject: Project) {
        tasks.create("worksGatherCoverageReport", Copy::class.java) { copy ->
            copy.group = "works reporter"
            copy.into(subProject.buildDir.paths("reports", "jacoco"))
            copy.shouldRunAfter("worksRootJacocoReport")

            subprojects { subProject ->
                subProject.tasks.withType(JacocoReport::class.java).forEach {
                    val xmlReport = it.reports.xml
                    val htmlReport = it.reports.html

                    val name = when (it.name) {
                        subProject.reporter.defaultCoverage -> {
                            xmlReport.isEnabled = true
                            htmlReport.isEnabled = true
                            "default"
                        }
                        else -> it.name
                    }

                    if (xmlReport.isEnabled) {
                        copy.from(xmlReport.destination) { spec ->
                            spec.rename(".*\\.xml\$", "coverage.xml")
                            spec.into(Paths.get("xml", it.project.name, name).toFile())
                        }
                    }

                    if (htmlReport.isEnabled) {
                        copy.from(htmlReport.destination) { spec ->
                            spec.into(Paths.get("html", it.project.name, name).toFile())
                        }
                    }
                }
            }
        }

        val mergeCoverage = tasks.create("worksMergeJacocoExec", JacocoMerge::class.java) { jacoco ->
            jacoco.group = "works reporter"
            jacoco.destinationFile = subProject.buildDir.paths("reports", "jacoco", "exec", "root", "jacoco.exec")

            jacoco.onlyIf {
                var execute = false
                subprojects {
                    it.tasks.withType(JacocoReport::class.java).forEach {
                        it.executionData.files.filter {
                            it.exists()
                        }.forEach {
                            execute = true
                            jacoco.executionData(it)
                        }
                    }
                }
                execute
            }
        }

        tasks.create("worksRootJacocoReport", JacocoReport::class.java) { jacoco ->
            jacoco.group = "works reporter"
            jacoco.dependsOn(mergeCoverage.path)

            subprojects {
                val jacocoCoverageTests = it.tasks.withType(JacocoReport::class.java)
                jacoco.classDirectories = files(jacocoCoverageTests.flatMap {
                    it.classDirectories ?: files()
                })

                jacoco.sourceDirectories = files(jacocoCoverageTests.flatMap {
                    it.sourceDirectories ?: files()
                })


                jacoco.executionData(mergeCoverage.destinationFile)
                jacoco.reports {
                    it.xml.isEnabled = true
                    it.xml.destination = subProject.buildDir.paths("reports", "jacoco", "xml", "root", "coverage.xml")
                    it.html.isEnabled = true
                    it.html.destination = subProject.buildDir.paths("reports", "jacoco", "html", "root")
                }
            }
        }
    }

    private fun Project.createGatherStaticAnalysisTasks(subProject: Project) {
        tasks.create("worksGatherCheckstyle", Copy::class.java) { copy ->
            copy.group = "works reporter"
            copy.into(subProject.buildDir.paths("reports", "checkstyle"))

            subprojects {
                val extension = it.reporter
                createCopySpec(copy, it, extension.checkstyleTasks, extension.checkstyleFiles)
            }
        }

        tasks.create("worksGatherPMD", Copy::class.java) { copy ->
            copy.group = "works reporter"
            copy.into(subProject.buildDir.paths("reports", "pmd"))

            subprojects {
                val extension = it.reporter
                createCopySpec(copy, it, extension.pmdTasks, extension.pmdFiles)
            }
        }
    }

    private fun createCopySpec(copy: Copy, project: Project,
                               checkstyleTasks: MutableList<String>, checkstyleFiles: FileCollection?) {
        if (checkstyleTasks.isNotEmpty()) {
            val list = checkstyleTasks.mapNotNull {
                project.tasks.findByName(it)?.let {
                    if (!it.outputs.hasOutput) {
                        project.logger.warn("Task ${it} does not have @TaskOutput")
                    }

                    copy.from(it.outputs) { spec ->
                        spec.into(Paths.get("html", it.project.name).toFile())
                        spec.include("*.html")
                    }

                    copy.from(it.outputs) { spec ->
                        spec.into(Paths.get("xml", it.project.name).toFile())
                        spec.include("*.xml")
                    }
                    it.path
                }
            }
            copy.setShouldRunAfter(list)
        }

        checkstyleFiles?.let {
            copy.from(it) { spec ->
                spec.into(Paths.get("raw", project.name).toFile())
            }
        }
    }

    private fun applySubProject(project: Project) {
        createExtension(project)
    }

    private fun createExtension(project: Project) {
        project.extensions.add("worksReporter", ReporterExtension())
    }

    private val Project.reporter: ReporterExtension
        get() {
            return extensions.getByType(ReporterExtension::class.java)
        }
}