package com.mobilesolutionworks.android.gradle.configurator

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidProjectConfigurator: Plugin<Project> {

    override fun apply(project: Project) {
        with(project.rootProject) {
            // dependencies.components.
        }
    }
}
