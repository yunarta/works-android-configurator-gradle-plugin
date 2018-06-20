package com.mobilesolutionworks.android.gradle.configurator.reporter

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

val Project.worksReporter: ReporterExtension?
    get() {
        return extensions.findByName("worksReporter") as? ReporterExtension
    }

open class ReporterExtension {

    var defaultTest = "test"

    var defaultCoverage = "jacocoTestReport"

    var checkstyleTasks = mutableListOf<String>()
    var checkstyleFiles: FileCollection? = null

    var pmdTasks = mutableListOf<String>()
    var pmdFiles: FileCollection? = null
}