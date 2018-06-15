package com.mobilesolutionworks.android.gradle.configurator.substitution

import org.gradle.api.Plugin
import org.gradle.api.Project

class DependencySubstitutionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val substitutions = createSubstitutionContainer(project)

        project.afterEvaluate {
            val buildSubstitution: String? = project.properties["buildSubstitution"]?.toString()

            val substitutionsMap = substitutions.evaluate(buildSubstitution)
            substitutionsMap.values.forEach {
                println("resolved = ${it.request.spec}, replacement = ${it.replacement}")
            }

            project.configurations.all {
                it.resolutionStrategy {
                    it.eachDependency { resolve ->
                        ModuleSpec(
                                resolve.requested.group,
                                resolve.requested.name,
                                resolve.requested.version
                        ).searchList.mapNotNull {
                            substitutionsMap[it]
                        }.sortedWith(Comparator { first, second ->
                            Integer.compare(second.request.spec.toString().length, first.request.spec.toString().length)
                        }).firstOrNull()?.replace(resolve)
                    }
                }
            }
        }
    }

    private fun createSubstitutionContainer(project: Project): RootSubstituteListContainer {
        return RootSubstituteListContainer(project.container(SubstituteListContainer::class.java)).also {
            project.extensions.add("worksSubstitution", it)
        }
    }
}
