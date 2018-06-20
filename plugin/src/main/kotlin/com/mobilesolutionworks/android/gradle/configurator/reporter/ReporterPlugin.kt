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

    private val reporterExtensions = mutableMapOf<String, ReporterExtension>()

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
            tasks.create("worksGatherTestReport", Copy::class.java) { copy: Copy ->
                copy.group = "works reporter"
                copy.into(project.buildDir.paths("reports", "junit"))

                subprojects { subProject ->
                    subProject.tasks.withType(Test::class.java).map {
                        val name = when (it.name) {
                            reporterExtensions[subProject.name]?.defaultTest -> {
                                println("replaced")
                                "default"
                            }
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

            tasks.create("worksGatherCoverageReport", Copy::class.java) { copy ->
                copy.group = "works reporter"
                copy.into(project.buildDir.paths("reports", "jacoco"))
                copy.shouldRunAfter("worksRootJacocoReport")

                subprojects {subProject ->
                    subProject.tasks.withType(JacocoReport::class.java).forEach {
                        val xmlReport = it.reports.xml
                        val htmlReport = it.reports.html

                        val name = when (it.name) {
                            reporterExtensions[subProject.name]?.defaultCoverage -> {
                                xmlReport.isEnabled = true
                                htmlReport.isEnabled = true
                                "default"
                            }
                            "worksRootJacocoReport" -> return@forEach
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
                subprojects {
                    it.tasks.withType(JacocoReport::class.java).forEach {
                        jacoco.executionData(it.executionData)
                    }
                }
                jacoco.destinationFile = project.buildDir.paths("reports", "jacoco", "exec", "root", "jacoco.exec")
                jacoco.onlyIf {
                    jacoco.executionData?.map {
                        it.exists()
                    }?.reduce { a, b -> a && b } ?: false
                }
            }

            tasks.create("worksRootJacocoReport", JacocoReport::class.java) { jacoco ->
                jacoco.group = "works reporter"
                jacoco.dependsOn(mergeCoverage.path)

                subprojects {
                    val jacocoCoverageTests = it.tasks.withType(JacocoReport::class.java)
                    jacoco.classDirectories = files(*(jacocoCoverageTests.flatMap {
                        it.classDirectories ?: files()
                    }.toTypedArray()))

                    jacoco.sourceDirectories = files(*(jacocoCoverageTests.flatMap {
                        it.sourceDirectories ?: files()
                    }.toTypedArray()))


                    jacoco.executionData(mergeCoverage.destinationFile)
                    jacoco.reports {
                        it.xml.isEnabled = true
                        it.xml.destination = project.buildDir.paths("reports", "jacoco", "xml", "root", "coverage.xml")
                        it.html.isEnabled = true
                        it.html.destination = project.buildDir.paths("reports", "jacoco", "html", "root")
                    }
                }
            }

            tasks.create("worksGatherCheckstyle", Copy::class.java) { copy ->
                copy.into(project.buildDir.paths("reports", "checkstyle"))

                subprojects { subProject ->
                    reporterExtensions[subProject.name]?.let { extension ->
                        createCopySpec(copy, subProject, extension.checkstyleTasks, extension.checkstyleFiles)
                    } ?: copy.onlyIf { false }
                }
            }

            tasks.create("worksGatherPMD", Copy::class.java) { copy ->
                copy.into(project.buildDir.paths("reports", "pmd"))

                subprojects { subProject ->
                    reporterExtensions[subProject.name]?.let { extension ->
                        createCopySpec(copy, subProject, extension.pmdTasks, extension.pmdFiles)
                    } ?: copy.onlyIf { false }
                }
            }

            tasks.create("worksGatherReport") {
                it.shouldRunAfter("worksRootJacocoReport")
                it.dependsOn("worksRootJacocoReport", "worksGatherTestReport", "worksGatherCoverageReport")
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
                        spec.into(Paths.get(it.project.name).toFile())
                    }
                    it.path
                }
            }.toTypedArray()

            copy.dependsOn(*list)
            copy.shouldRunAfter(*list)
        }

        checkstyleFiles?.let {
            copy.from(it) { spec ->
                spec.into(Paths.get(project.name).toFile())
                spec.include("*.xml")
            }
            copy.dependsOn(it)
        }
    }

    private fun applySubProject(project: Project) {
        createExtension(project)
    }

    private fun createExtension(project: Project) {
        ReporterExtension().also {
            reporterExtensions[project.name] = it
            project.extensions.add("worksReporter", it)
        }
    }
}