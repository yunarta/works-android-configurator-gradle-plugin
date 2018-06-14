package com.mobilesolutionworks.android.gradle.configurator.substitution

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import java.io.File

class DependencySubstitutionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val container = project.container(SubstituteListContainer::class.java)

        val substitutions = RootSubstituteListContainer(container)
        project.extensions.add("worksSubstitution", substitutions)
//        val substitutions = project.extensions.create("worksSubstitution", RootSubstituteListContainer::class.java)
//        substitutions.conditional = container

        project.afterEvaluate {
            createTasks(it)

            substitutions.requests.forEach {
                println("base = ${it.request.spec}, replacement = ${it.replacement}")
            }
            substitutions.conditional.all {
                println("it.name = ${it.name}")
                it.requests.forEach {
                    println("   sub = ${it.request.spec}, replacement = ${it.replacement}")
                }
            }

            var buildSubstitution: String? = project.properties["buildSubstitution"]?.toString()
            val requestMap = substitutions.evaluate(buildSubstitution)
            requestMap.values.forEach {
                println("resolved = ${it.request.spec}, replacement = ${it.replacement}")
            }

            project.configurations.all {
                it.resolutionStrategy {
                    it.eachDependency { resolve ->
                        val moduleSpec = ModuleSpec(resolve.requested.group, resolve.requested.name, resolve.requested.version)
                        moduleSpec.searchList.mapNotNull {
                            requestMap[it]
                        }.maxBy {
                            it.request.spec.toString()
                        }?.replace(resolve) ?: Unit
                    }
                }
            }
        }
    }

    private fun stepIn(project: Project, root: ResolvedDependency): MutableSet<String> {
        val dependencies = mutableSetOf<String>()
        dependencies.add(ModuleSpec.create(root.module.id).toString())

        root.children.forEach {
            if (project.configurations.findByName(it.configuration)?.isCanBeResolved == true && it in it.parents) {
                dependencies.addAll(stepIn(project, it))
            }
        }

        return dependencies
    }

    private fun createTasks(project: Project) {

        with(project) {
            tasks.create("worksPrintDependencies") {
                it.doLast {

                    val dependencies = mutableSetOf<String>()
                    val resolved = project.configurations.flatMapTo(dependencies) { configuration ->
                        if (configuration.isCanBeResolved) {
                            configuration.resolvedConfiguration.firstLevelModuleDependencies.flatMapTo(dependencies) {
                                stepIn(project, it)
                            }
                        } else {
                            emptySet()
                        }
                    }

                    project.properties["depOutput"]?.let {
                        File(project.rootDir, it.toString())
                                .writeText(resolved.joinToString(System.lineSeparator()))
                    } ?: resolved.forEach {
                        println(it)
                    }
                }
            }
        }
    }
}
