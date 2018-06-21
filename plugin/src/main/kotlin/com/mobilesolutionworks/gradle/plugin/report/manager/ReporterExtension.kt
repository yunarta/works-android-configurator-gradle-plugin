package com.mobilesolutionworks.gradle.plugin.report.manager

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

internal val Project.worksReportManager: ReporterExtension?
    get() {
        return extensions.findByName("worksReportManager") as? ReporterExtension
    }

open class ReporterExtension {

    var defaultTest = "test"

    var defaultCoverage = "jacocoTestReport"

    var checkstyleTasks = mutableListOf<String>()
    var checkstyleFiles: FileCollection? = null

    var pmdTasks = mutableListOf<String>()
    var pmdFiles: FileCollection? = null
}