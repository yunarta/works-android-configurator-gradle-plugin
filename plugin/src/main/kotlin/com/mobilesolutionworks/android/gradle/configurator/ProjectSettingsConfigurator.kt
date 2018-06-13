package com.mobilesolutionworks.android.gradle.configurator

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class ProjectSettingsConfigurator : Plugin<Settings> {

    override fun apply(settings: Settings) {
        settings.apply {

        }
        settings.pluginManagement {
            val repositories = it.repositories
            repositories.add(repositories.gradlePluginPortal())
            repositories.add(repositories.google())

            it.resolutionStrategy {
                it.eachPlugin {
                    when (it.requested.id.id) {
                        "com.android.application", "com.android.library" -> {
                            it.useModule("com.android.tools.build:gradle:3.1.3")
                        }
                    }
                }
            }
        }
    }
}
